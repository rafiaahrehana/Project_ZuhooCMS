package com.businessos.modules.ai.service.impl;

import com.businessos.modules.ai.audit.AiAuditService;
import com.businessos.modules.ai.client.AiToolCallOrText;
import com.businessos.modules.ai.client.AiToolExchange;
import com.businessos.modules.ai.dto.request.AiAgentTurnRequest;
import com.businessos.modules.ai.dto.request.AiGenerateRequest;
import com.businessos.modules.ai.dto.request.AiPromptTemplateRequest;
import com.businessos.modules.ai.dto.request.AiProviderConfigRequest;
import com.businessos.modules.ai.dto.request.AiThreadCreateRequest;
import com.businessos.modules.ai.dto.response.AiGenerateResponse;
import com.businessos.modules.ai.dto.response.AiPromptTemplateResponse;
import com.businessos.modules.ai.dto.response.AiProviderConfigResponse;
import com.businessos.modules.ai.dto.response.AiThreadResponse;
import com.businessos.modules.ai.dto.response.AiUsageSummaryResponse;
import com.businessos.modules.ai.entity.AiConversation;
import com.businessos.modules.ai.entity.AiConversationThread;
import com.businessos.modules.ai.entity.AiPromptTemplate;
import com.businessos.modules.ai.entity.AiProviderConfig;
import com.businessos.modules.ai.entity.AiToolCallLog;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.exception.AiProviderException;
import com.businessos.modules.ai.exception.AiQuotaExceededException;
import com.businessos.modules.ai.mapper.AiMapper;
import com.businessos.modules.ai.provider.AiProviderAdapter;
import com.businessos.modules.ai.repository.AiConversationRepository;
import com.businessos.modules.ai.repository.AiConversationThreadRepository;
import com.businessos.modules.ai.repository.AiPromptTemplateRepository;
import com.businessos.modules.ai.repository.AiProviderConfigRepository;
import com.businessos.modules.ai.repository.AiToolCallLogRepository;
import com.businessos.modules.ai.repository.AiUsageLogRepository;
import com.businessos.modules.ai.resolver.AiProviderResolver;
import com.businessos.modules.ai.config.AiProperties;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolRegistry;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.ai.util.AiKeyDecryptor;
import com.businessos.modules.ai.util.AiTextSanitizer;
import com.businessos.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiProviderResolver         resolver;
    private final AiAuditService             auditService;
    private final AiUsageLogRepository       usageLogRepository;
    private final AiProviderConfigRepository configRepository;
    private final AiPromptTemplateRepository templateRepository;
    private final AiConversationRepository   conversationRepository; // FIX: was missing
    private final AiConversationThreadRepository threadRepository;
    private final AiToolCallLogRepository    toolCallLogRepository;
    private final AiToolRegistry             toolRegistry;
    private final AiKeyDecryptor             keyDecryptor;
    private final AiTextSanitizer            textSanitizer;
    private final AiProperties               aiProperties;
    private final SecurityUtil               securityUtil;
    private final AuthorizationService       authorizationService;

    /*
     * Deliberately NOT @Transactional: generateWithRetry makes up to 3 blocking
     * provider calls (10s timeout each, plus 1.2s of backoff), so wrapping this
     * in a transaction pinned a pooled DB connection for up to ~31s whenever a
     * provider was slow or down - enough to exhaust the pool under load. Nothing
     * here needs one: the reads are independent, and AiAuditService.record runs
     * REQUIRES_NEW in its own transaction either way.
     */
    @Override
    public AiGenerateResponse generate(AiGenerateRequest request) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        User user      = securityUtil.getCurrentUser();
        Long companyId = securityUtil.getCurrentCompanyId();
        Company company = companyRef(companyId);

        enforceRateLimits(companyId, user.getId());

        AiConversationThread thread = resolveOwnedThread(request.getThreadId(), companyId, user.getId());

        String rawPrompt = resolvePrompt(request.getFeature(), request.getPrompt(), companyId);
        String prompt = textSanitizer.sanitize(
            thread != null ? withThreadHistory(thread, rawPrompt) : rawPrompt);
        AiProviderAdapter adapter = resolver.resolve(companyId);

        long start    = System.currentTimeMillis();
        String result = generateWithRetry(adapter, prompt);
        long elapsed  = System.currentTimeMillis() - start;

        // Audited with the *caller's* prompt, not the history-augmented one -
        // the transcript is reconstructible from the thread's own prior rows,
        // so duplicating it into every row's requestPayload would just bloat
        // storage and make each row's audit text misleading about what the
        // employee actually typed.
        String uuid = auditService.record(
            request.getFeature(), adapter.getProviderType(), adapter.getModel(),
            request.getPrompt(), result, elapsed, user, company, thread);

        if (thread != null) {
            stampThreadActivity(thread, request.getPrompt());
        }

        AiGenerateResponse response = new AiGenerateResponse();
        response.setConversationUuid(uuid);
        response.setFeature(request.getFeature());
        response.setProvider(adapter.getProviderType());
        response.setModel(adapter.getModel());
        response.setResult(result);
        response.setExecutionTimeMs(elapsed);
        response.setThreadId(thread != null ? thread.getId() : null);
        return response;
    }

    @Override
    public String generateFromPrompt(AiFeature feature, String prompt) {
        AiGenerateRequest request = new AiGenerateRequest();
        request.setFeature(feature);
        request.setPrompt(prompt);
        return generate(request).getResult();
    }

    /** Not @Transactional, for the same reason as {@link #generate}. */
    @Override
    public String generateRaw(AiFeature feature, String prompt) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        User user       = securityUtil.getCurrentUser();
        Long companyId  = securityUtil.getCurrentCompanyId();
        Company company = companyRef(companyId);

        enforceRateLimits(companyId, user.getId());

        // Callers pass a prompt assembled by a *PromptBuilder, whose inputs are
        // user-entered entity fields - strip control characters before they reach
        // the provider's JSON body.
        String sanitized = textSanitizer.sanitize(prompt);

        AiProviderAdapter adapter = resolver.resolve(companyId);

        long start    = System.currentTimeMillis();
        String result = generateWithRetry(adapter, sanitized);
        long elapsed  = System.currentTimeMillis() - start;

        auditService.record(feature, adapter.getProviderType(), adapter.getModel(),
            sanitized, result, elapsed, user, company);

        return result;
    }

    @Override
    @Transactional
    public AiProviderConfigResponse saveProviderConfig(AiProviderConfigRequest request) {
        Long companyId = securityUtil.getCurrentCompanyId();

        // Upsert by (companyId, provider) - a company can save one config per
        // provider (uq_ai_config_company_provider). Previously this looked up
        // the single *active* row regardless of provider, so saving a second
        // provider (e.g. Gemini alongside an already-saved Claude) silently
        // overwrote the Claude row instead of creating its own.
        AiProviderConfig config = configRepository
            .findByCompanyIdAndAiProviderType(companyId, request.getAiProviderType())
            .orElseGet(() -> {
                AiProviderConfig c = new AiProviderConfig();
                c.setCompany(companyRef(companyId));
                c.setAiProviderType(request.getAiProviderType());
                return c;
            });

        config.setAiModel(request.getModel());

        // Trimmed before encrypting - a copy-pasted key very commonly carries an
        // invisible leading/trailing newline or space, which the provider's API
        // rejects outright (e.g. Anthropic's "invalid x-api-key") with no hint
        // that whitespace, not the key itself, was the problem.
        if (request.getApiKey() != null && !request.getApiKey().isBlank())
            config.setApiKeyEncrypted(keyDecryptor.encrypt(request.getApiKey().trim()));
        if (request.getTemperature() != null)
            config.setTemperature(request.getTemperature());
        if (request.getMaxTokens() != null)
            config.setMaxTokens(request.getMaxTokens());

        // Saving a config is "I want to use this now" - make it the active one
        // and deactivate whichever provider was active before, so exactly one
        // config drives AiProviderResolver.resolve() at all times.
        deactivateAllExcept(companyId, null);
        config.setActive(true);
        configRepository.save(config);

        return AiMapper.toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public AiProviderConfigResponse getProviderConfig() {
        return configRepository.findByCompanyIdAndActiveTrue(securityUtil.getCurrentCompanyId())
            .map(AiMapper::toConfigResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No AI provider config found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderConfigResponse> listProviderConfigs() {
        return configRepository.findByCompanyIdOrderByAiProviderType(securityUtil.getCurrentCompanyId())
            .stream()
            .map(AiMapper::toConfigResponse)
            .toList();
    }

    @Override
    @Transactional
    public AiProviderConfigResponse activateProviderConfig(Long id) {
        Long companyId = securityUtil.getCurrentCompanyId();
        AiProviderConfig config = configRepository.findById(id)
            .filter(c -> c.getCompany() != null && companyId.equals(c.getCompany().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Provider config not found: " + id));

        deactivateAllExcept(companyId, id);
        config.setActive(true);
        configRepository.save(config);
        return AiMapper.toConfigResponse(config);
    }

    @Override
    @Transactional
    public void deleteProviderConfig(Long id) {
        Long companyId = securityUtil.getCurrentCompanyId();
        AiProviderConfig config = configRepository.findById(id)
            .filter(c -> c.getCompany() != null && companyId.equals(c.getCompany().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Provider config not found: " + id));

        if (config.isActive()) {
            throw new com.businessos.shared.exception.BadRequestException(
                "Cannot delete the active provider - activate a different one first.");
        }
        configRepository.delete(config);
    }

    /** Deactivates every saved config for the company except (optionally) the one given. */
    private void deactivateAllExcept(Long companyId, Long keepId) {
        for (AiProviderConfig c : configRepository.findByCompanyIdOrderByAiProviderType(companyId)) {
            if (c.isActive() && !c.getId().equals(keepId)) {
                c.setActive(false);
                configRepository.save(c);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiGenerateResponse> listConversations(AiFeature feature, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        Long companyId = securityUtil.getCurrentCompanyId();

        /*
         * FIX: original code queried templateRepository (prompt templates) instead of
         * conversationRepository (actual AI call history). Both branches of the ternary
         * also did identical queries — the feature filter was completely ignored.
         *
         * Corrected to use conversationRepository with proper feature branching.
         */
        Page<AiConversation> page = (feature != null)
            ? conversationRepository.findByCompanyIdAndFeatureOrderByCreatedAtDesc(
                companyId, feature, pageable)
            : conversationRepository.findByCompanyIdOrderByCreatedAtDesc(
                companyId, pageable);

        return page.map(conv -> {
            AiGenerateResponse r = new AiGenerateResponse();
            r.setConversationUuid(conv.getConversationUuid());
            r.setFeature(conv.getFeature());
            r.setProvider(conv.getProvider());
            r.setModel(conv.getModel());
            r.setResult(conv.getResponsePayload());
            r.setExecutionTimeMs(conv.getExecutionTimeMs() != null
                ? conv.getExecutionTimeMs() : 0L);
            return r;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageSummaryResponse getUsageSummary(LocalDate date) {
        Long companyId   = securityUtil.getCurrentCompanyId();
        LocalDate target = date != null ? date : LocalDate.now();

        long totalRequests = usageLogRepository.countByCompanyAndDate(companyId, target);
        Long totalTokens   = usageLogRepository.totalTokensForPeriod(companyId, target, target);
        Double avgMs       = usageLogRepository.avgResponseTimeMs(companyId, target);

        List<Object[]> byFeature = usageLogRepository.aggregateByFeature(
            companyId, target, target);

        Map<String, Long> requestsByFeature = new LinkedHashMap<>();
        Map<String, Long> tokensByFeature   = new LinkedHashMap<>();
        for (Object[] row : byFeature) {
            String key = row[0].toString();
            requestsByFeature.put(key, ((Number) row[1]).longValue());
            tokensByFeature.put(key,   ((Number) row[2]).longValue());
        }

        AiUsageSummaryResponse summary = new AiUsageSummaryResponse();
        summary.setDate(target);
        summary.setTotalRequests(totalRequests);
        summary.setTotalTokens(totalTokens != null ? totalTokens : 0L);
        summary.setAvgResponseTimeMs(avgMs != null ? avgMs : 0.0);
        summary.setRequestsByFeature(requestsByFeature);
        summary.setTokensByFeature(tokensByFeature);
        return summary;
    }

    @Override
    @Transactional
    public AiPromptTemplateResponse savePromptTemplate(AiPromptTemplateRequest request) {
        Long companyId   = securityUtil.getCurrentCompanyId();
        User currentUser = securityUtil.getCurrentUser();

        List<AiPromptTemplate> existing = (companyId != null)
            ? templateRepository.findByCompanyIdOrderByFeatureAscVersionDesc(companyId, Pageable.unpaged())
                .stream()
                .filter(t -> t.getFeature() == request.getFeature() && t.isActive())
                .collect(Collectors.toList())
            : templateRepository.findByCompanyIsNullOrderByFeatureAscVersionDesc(Pageable.unpaged())
                .stream()
                .filter(t -> t.getFeature() == request.getFeature() && t.isActive())
                .collect(Collectors.toList());

        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;
        existing.forEach(t -> t.setActive(false));

        AiPromptTemplate template = AiPromptTemplate.builder()
            .feature(request.getFeature())
            .name(request.getName())
            .template(request.getTemplate())
            .version(nextVersion)
            .active(true)
            .changeNotes(request.getChangeNotes())
            .company(companyRef(companyId))
            .updatedBy(currentUser)
            .build();

        templateRepository.save(template);
        
        return AiMapper.toTemplateResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiPromptTemplateResponse> listPromptTemplates(Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        Page<AiPromptTemplate> page = (companyId != null)
            ? templateRepository.findByCompanyIdOrderByFeatureAscVersionDesc(companyId, pageable)
            : templateRepository.findByCompanyIsNullOrderByFeatureAscVersionDesc(pageable);
        return page.map(AiMapper::toTemplateResponse);
    }

    @Override
    @Transactional
    public void deletePromptTemplate(Long id) {
        authorizationService.checkPermission(PermissionCode.AI_ADMIN);
        Long companyId = securityUtil.getCurrentCompanyId();
        AiPromptTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prompt template not found: " + id));
        Long templateCompanyId = template.getCompany() != null ? template.getCompany().getId() : null;
        if (!java.util.Objects.equals(templateCompanyId, companyId)) {
            throw new ResourceNotFoundException("Prompt template not found: " + id);
        }
        template.softDelete();
    }

    @Override
    @Transactional
    public AiThreadResponse createThread(AiThreadCreateRequest request) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        Long companyId = securityUtil.getCurrentCompanyId();
        User user = securityUtil.getCurrentUser();

        AiConversationThread thread = AiConversationThread.builder()
            .feature(request.getFeature())
            .company(companyRef(companyId))
            .user(user)
            .build();
        threadRepository.save(thread);
        return AiMapper.toThreadResponse(thread);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiThreadResponse> listThreads(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        Long companyId = securityUtil.getCurrentCompanyId();
        Long userId = securityUtil.getCurrentUser().getId();
        return threadRepository.findByCompanyIdAndUserIdOrderByUpdatedAtDesc(companyId, userId, pageable)
            .map(AiMapper::toThreadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiGenerateResponse> getThreadMessages(Long threadId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        Long companyId = securityUtil.getCurrentCompanyId();
        Long userId = securityUtil.getCurrentUser().getId();
        threadRepository.findByIdAndCompanyIdAndUserId(threadId, companyId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));

        return conversationRepository.findByThreadIdOrderByCreatedAtAsc(threadId, pageable)
            .map(conv -> {
                AiGenerateResponse r = new AiGenerateResponse();
                r.setConversationUuid(conv.getConversationUuid());
                r.setFeature(conv.getFeature());
                r.setProvider(conv.getProvider());
                r.setModel(conv.getModel());
                r.setResult(conv.getResponsePayload());
                r.setExecutionTimeMs(conv.getExecutionTimeMs() != null ? conv.getExecutionTimeMs() : 0L);
                r.setThreadId(threadId);
                return r;
            });
    }

    @Override
    @Transactional
    public void deleteThread(Long threadId) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        Long companyId = securityUtil.getCurrentCompanyId();
        Long userId = securityUtil.getCurrentUser().getId();
        AiConversationThread thread = threadRepository.findByIdAndCompanyIdAndUserId(threadId, companyId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
        thread.softDelete();
    }

    private static final ObjectMapper AGENT_MAPPER = new ObjectMapper();

    @Override
    @Transactional
    public AiGenerateResponse runAgentTurn(AiAgentTurnRequest request) {
        authorizationService.checkPermission(PermissionCode.AI_CHAT);
        User user = securityUtil.getCurrentUser();
        Long companyId = securityUtil.getCurrentCompanyId();
        Company company = companyRef(companyId);

        enforceRateLimits(companyId, user.getId());

        AiConversationThread thread = threadRepository
            .findByIdAndCompanyIdAndUserId(request.getThreadId(), companyId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + request.getThreadId()));

        String message = textSanitizer.sanitize(request.getMessage());
        List<AiTool> availableTools = toolRegistry.availableForCurrentUser();
        AiProviderAdapter adapter = resolver.resolve(companyId);

        long start = System.currentTimeMillis();

        // A pending write-action from the *previous* turn takes priority over
        // treating this message as a fresh question - it's either a
        // confirm, a cancel, or (falling through below) a new question that
        // implicitly supersedes it.
        if (thread.getPendingAction() != null) {
            PendingAction pending = readPendingAction(thread.getPendingAction());
            if (pending != null) {
                if (looksLikeConfirmation(message)) {
                    return executeConfirmedAction(thread, pending, availableTools, adapter,
                        user, company, companyId, start);
                }
                if (looksLikeCancellation(message)) {
                    thread.setPendingAction(null);
                    threadRepository.save(thread);
                    return simpleAgentResponse(thread, adapter,
                        "Okay, cancelled - nothing was submitted.", start, false);
                }
                // Anything else: the employee moved on to a new question.
                // Drop the stale proposal rather than force them to
                // explicitly cancel it first.
                thread.setPendingAction(null);
            }
        }

        String promptWithHistory = withThreadHistory(thread, message);
        AiToolCallOrText firstPass = adapter.callWithTools(promptWithHistory, availableTools, List.of());

        if (!firstPass.isToolCall()) {
            return persistAgentExchange(thread, adapter, message, firstPass.getText(), start, false);
        }

        AiTool tool = toolRegistry.byName(firstPass.getToolName()).orElse(null);
        // Defense in depth: even though availableTools was already filtered
        // to what this user may use, a tool the caller isn't authorized for
        // must never execute even if a provider somehow names it anyway.
        boolean permitted = tool != null && availableTools.stream().anyMatch(t -> t.name().equals(tool.name()));
        if (!permitted) {
            return persistAgentExchange(thread, adapter, message,
                "I can't do that - it's not one of the things I'm able to help with for your account.",
                start, false);
        }

        if (tool.isWrite()) {
            PendingAction pending = new PendingAction(tool.name(), firstPass.getToolArgs(), firstPass.getCallId());
            thread.setPendingAction(writePendingAction(pending));
            String proposal = "I'll " + tool.describeProposal(firstPass.getToolArgs())
                + ". Reply to confirm, or tell me what to change.";
            return persistAgentExchange(thread, adapter, message, proposal, start, true);
        }

        // Read tool: safe to execute immediately.
        AiToolResult result = tool.execute(firstPass.getToolArgs(), user.getId(), companyId);
        logToolCall(thread, tool, firstPass.getToolArgs(), result, company, user);

        AiToolCallOrText secondPass = adapter.callWithTools(promptWithHistory, availableTools,
            List.of(new AiToolExchange(tool.name(), firstPass.getToolArgs(), firstPass.getCallId(), result.forModel())));
        // v1 doesn't chain a second tool call within one turn - if the model
        // tries anyway, fall back to the tool's own plain-text result rather
        // than silently dropping the answer.
        String finalText = secondPass.isToolCall() ? result.forModel() : secondPass.getText();

        return persistAgentExchange(thread, adapter, message, finalText, start, false);
    }

    private AiGenerateResponse executeConfirmedAction(AiConversationThread thread, PendingAction pending,
            List<AiTool> availableTools, AiProviderAdapter adapter,
            User user, Company company, Long companyId, long start) {
        AiTool tool = toolRegistry.byName(pending.toolName).orElse(null);
        boolean permitted = tool != null && availableTools.stream().anyMatch(t -> t.name().equals(tool.name()));
        thread.setPendingAction(null);

        if (!permitted) {
            return simpleAgentResponse(thread, adapter,
                "I couldn't complete that - it's no longer available for your account.", start, false);
        }

        AiToolResult result = tool.execute(pending.args, user.getId(), companyId);
        logToolCall(thread, tool, pending.args, result, company, user);
        return simpleAgentResponse(thread, adapter, result.forModel(), start, false);
    }

    private void logToolCall(AiConversationThread thread, AiTool tool, Map<String, Object> args,
            AiToolResult result, Company company, User user) {
        try {
            toolCallLogRepository.save(AiToolCallLog.builder()
                .toolName(tool.name())
                .toolArgs(AGENT_MAPPER.writeValueAsString(args))
                .success(result.isSuccess())
                .resultSummary(result.getMessage())
                .thread(thread)
                .company(company)
                .user(user)
                .build());
        } catch (Exception ignored) {
            // Logging the audit trail must never break the actual tool
            // execution that already happened.
        }
    }

    private AiGenerateResponse persistAgentExchange(AiConversationThread thread, AiProviderAdapter adapter,
            String userMessage, String replyText, long start, boolean awaitingConfirmation) {
        long elapsed = System.currentTimeMillis() - start;
        User user = securityUtil.getCurrentUser();
        Company company = companyRef(securityUtil.getCurrentCompanyId());

        String uuid = auditService.record(thread.getFeature(), adapter.getProviderType(), adapter.getModel(),
            userMessage, replyText, elapsed, user, company, thread);
        stampThreadActivity(thread, userMessage);

        AiGenerateResponse response = new AiGenerateResponse();
        response.setConversationUuid(uuid);
        response.setFeature(thread.getFeature());
        response.setProvider(adapter.getProviderType());
        response.setModel(adapter.getModel());
        response.setResult(replyText);
        response.setExecutionTimeMs(elapsed);
        response.setThreadId(thread.getId());
        response.setAwaitingConfirmation(awaitingConfirmation);
        return response;
    }

    /** Same as persistAgentExchange but for a turn with no new user-visible question (confirm/cancel replies). */
    private AiGenerateResponse simpleAgentResponse(AiConversationThread thread, AiProviderAdapter adapter,
            String replyText, long start, boolean awaitingConfirmation) {
        return persistAgentExchange(thread, adapter, "(confirmation)", replyText, start, awaitingConfirmation);
    }

    private static final List<String> CONFIRM_WORDS = List.of(
        "yes", "yeah", "yep", "confirm", "confirmed", "sure", "ok", "okay", "go ahead", "do it", "correct");
    private static final List<String> CANCEL_WORDS = List.of(
        "no", "nope", "cancel", "don't", "dont", "stop", "never mind", "nevermind");

    private boolean looksLikeConfirmation(String message) {
        String lower = message.trim().toLowerCase();
        return CONFIRM_WORDS.stream().anyMatch(lower::startsWith);
    }

    private boolean looksLikeCancellation(String message) {
        String lower = message.trim().toLowerCase();
        return CANCEL_WORDS.stream().anyMatch(lower::startsWith);
    }

    private record PendingAction(String toolName, Map<String, Object> args, String callId) {}

    private String writePendingAction(PendingAction pending) {
        try {
            return AGENT_MAPPER.writeValueAsString(Map.of(
                "toolName", pending.toolName(),
                "args", pending.args() != null ? pending.args() : Map.of(),
                "callId", pending.callId() != null ? pending.callId() : ""));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private PendingAction readPendingAction(String json) {
        try {
            Map<String, Object> raw = AGENT_MAPPER.readValue(json, Map.class);
            String callId = (String) raw.get("callId");
            return new PendingAction((String) raw.get("toolName"),
                (Map<String, Object>) raw.get("args"), callId != null && callId.isBlank() ? null : callId);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    /** Null threadId is the normal, stateless case - not an error. */
    private AiConversationThread resolveOwnedThread(Long threadId, Long companyId, Long userId) {
        if (threadId == null) return null;
        return threadRepository.findByIdAndCompanyIdAndUserId(threadId, companyId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
    }

    // Bounded to the last 10 exchanges - enough for a natural back-and-forth
    // without the prompt growing unbounded (and eating the provider's context
    // window / token budget) on a long-lived thread.
    private static final int THREAD_HISTORY_LIMIT = 10;

    // Reply in whichever language the employee actually writes in - all four
    // real providers (Claude, OpenAI, Gemini, Groq's Llama models) handle
    // Bangla natively as multilingual models, so this needs no translation
    // layer, just an instruction the model already knows how to follow.
    private static final String LANGUAGE_MIRROR_INSTRUCTION =
        "(Reply in the same language the user's latest message is written in - "
        + "Bangla or English. Do not translate or mix languages mid-reply.)\n\n";

    private String withThreadHistory(AiConversationThread thread, String newPrompt) {
        List<AiConversation> history = conversationRepository
            .findByThreadIdOrderByCreatedAtAsc(thread.getId(),
                org.springframework.data.domain.PageRequest.of(0, THREAD_HISTORY_LIMIT))
            .getContent();

        if (history.isEmpty()) return LANGUAGE_MIRROR_INSTRUCTION + newPrompt;

        StringBuilder sb = new StringBuilder(LANGUAGE_MIRROR_INSTRUCTION);
        for (AiConversation exchange : history) {
            sb.append("User: ").append(exchange.getRequestPayload()).append('\n');
            sb.append("Assistant: ").append(exchange.getResponsePayload()).append('\n');
        }
        sb.append("User: ").append(newPrompt);
        return sb.toString();
    }

    private void stampThreadActivity(AiConversationThread thread, String firstUserMessage) {
        if (thread.getTitle() == null) {
            thread.setTitle(firstUserMessage.length() > 60
                ? firstUserMessage.substring(0, 60) + "…"
                : firstUserMessage);
        }
        // updatedAt is refreshed by JPA auditing on save - no manual timestamp
        // needed, this save just needs to happen so that listener fires.
        threadRepository.save(thread);
    }

    private static final int MAX_ATTEMPTS = 3; // 1 initial + 2 retries
    private static final long[] BACKOFF_MS = {300, 900};

    // Retries transient provider failures (timeouts, 429, 5xx) with a short backoff.
    // Non-retryable failures (bad request, auth, malformed response) fail immediately.
    private String generateWithRetry(AiProviderAdapter adapter, String prompt) {
        AiProviderException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return adapter.generate(prompt);
            } catch (AiProviderException e) {
                lastFailure = e;
                if (!e.isRetryable() || attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleep(BACKOFF_MS[attempt - 1]);
            }
        }

        throw lastFailure;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Both limits come from AiProperties (ai.daily-company-limit /
     * ai.hourly-user-limit) rather than the literals that used to be hardcoded
     * here - those ignored the configured values entirely, and enforced the
     * per-user cap as 50/day instead of the declared per-hour window.
     */
    private void enforceRateLimits(Long companyId, Long userId) {
        int companyLimit = aiProperties.getDailyCompanyLimit();
        long companyCount = usageLogRepository.countByCompanyAndDate(companyId, LocalDate.now());
        if (companyCount >= companyLimit)
            throw new AiQuotaExceededException(
                "Daily AI request limit reached for this company (" + companyLimit
                    + "/day). Try again tomorrow.");

        int userLimit = aiProperties.getHourlyUserLimit();
        long userCount = usageLogRepository.countByUserSince(userId, LocalDateTime.now().minusHours(1));
        if (userCount >= userLimit)
            throw new AiQuotaExceededException(
                "Hourly AI request limit reached for your account (" + userLimit
                    + "/hour). Try again shortly.");
    }

    private String resolvePrompt(AiFeature feature, String callerPrompt, Long companyId) {
        List<AiPromptTemplate> templates =
            templateRepository.findActiveForFeature(feature, companyId);

        if (!templates.isEmpty()) {
            String tmpl = templates.get(0).getTemplate();
            return tmpl.contains("%s")
                ? tmpl.formatted(callerPrompt)
                : tmpl + "\n\nContext:\n" + callerPrompt;
        }

        return callerPrompt;
    }

    private Company companyRef(Long companyId) {
        if (companyId == null) return null;
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
