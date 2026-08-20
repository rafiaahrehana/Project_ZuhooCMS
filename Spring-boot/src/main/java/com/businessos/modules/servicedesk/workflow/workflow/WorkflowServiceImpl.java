package com.businessos.modules.servicedesk.workflow.workflow;

import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageRepository;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageRequest;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageResponse;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplate;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateRepository;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateRequest;
import com.businessos.modules.servicedesk.workflow.template.WorkflowTemplateResponse;
import com.businessos.modules.company.Company;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.WorkflowSuggestionPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;

import java.util.List;

@Service
@RequiredArgsConstructor

public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowStageRepository stageRepository;
    private final SecurityUtil               securityUtil;
    private final AuthorizationService       authorizationService;
    private final AiService                  aiService;
    private final AiTransactionBoundary      aiTx;

    // ── Templates ─────────────────────────────────────────────────

    @Override
    @Transactional
    public WorkflowTemplateResponse createTemplate(WorkflowTemplateRequest request) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_CREATE);
        Long companyId = requireCompanyId();
        if (templateRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException(
                    "A workflow template named '" + request.getName() + "' already exists");
        }
        WorkflowTemplate template = WorkflowTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .company(companyRef(companyId))
                .version(1)
                .active(true)
                .build();

        templateRepository.save(template);
        
        return WorkflowMapper.toResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowTemplateResponse getTemplateById(Long id) {
        return WorkflowMapper.toResponse(findTemplate(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowTemplateResponse> listTemplates(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_VIEW);
        Long companyId = requireCompanyId();
        /*
         * BUG-FIX: was templateRepository.findAll(pageable) — leaked ALL tenants' templates.
         * Fixed to findByCompanyId() to enforce tenant isolation.
         */
        return templateRepository.findByCompanyId(companyId, pageable)
                .map(WorkflowMapper::toResponse);
    }

    // Deliberately NOT gated by WORKFLOW_VIEW here: this is the active-workflow picker
    // consumed by the Services admin page when attaching a workflow template to a
    // service - users with SERVICE_CATALOG_VIEW but not WORKFLOW_VIEW still need it.
    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTemplateResponse> listActiveTemplates() {
        Long companyId = requireCompanyId();
        /*
         * BUG-FIX: was templateRepository.findByActiveTrue() — leaked cross-tenant data.
         * Fixed to findByCompanyIdAndActiveTrue().
         */
        return templateRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(WorkflowMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkflowTemplateResponse updateTemplate(Long id, WorkflowTemplateRequest request) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_UPDATE);
        Long companyId = requireCompanyId();
        WorkflowTemplate template = findTemplate(id);

        if (!template.getName().equals(request.getName())
                && templateRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException(
                    "A workflow template named '" + request.getName() + "' already exists");
        }

        template.setName(request.getName());
        if (request.getDescription() != null)
            template.setDescription(request.getDescription());
        template.setVersion(template.getVersion() + 1);

        templateRepository.save(template);
        return WorkflowMapper.toResponse(template);
    }

    @Override
    @Transactional
    public WorkflowTemplateResponse toggleTemplate(Long id) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_UPDATE);
        WorkflowTemplate template = findTemplate(id);

        if (template.isActive()) {
            boolean hasActiveStages = template.getStages().stream()
                    .anyMatch(s -> !s.isDeleted());
            if (hasActiveStages) {
                throw new BadRequestException(
                    "Cannot deactivate a workflow template that still has stages. Remove all stages first.");
            }
        }

        template.setActive(!template.isActive());
        templateRepository.save(template);
        
        return WorkflowMapper.toResponse(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_DELETE);
        WorkflowTemplate template = findTemplate(id);

        if (!template.getStages().isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete a workflow template that still has stages. Remove all stages first.");
        }

        template.softDelete();
        templateRepository.save(template);
        
    }

    // ── Stages ────────────────────────────────────────────────────

    @Override
    @Transactional
    public WorkflowStageResponse addStage(Long templateId, WorkflowStageRequest request) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_UPDATE);
        Long companyId = requireCompanyId();
        WorkflowTemplate template = findTemplate(templateId);

        if (stageRepository.existsByWorkflowTemplateIdAndStageOrder(
                templateId, request.getStageOrder())) {
            throw new BadRequestException(
                    "Stage order " + request.getStageOrder() + " is already taken in this workflow");
        }

        WorkflowStage stage = WorkflowStage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .stageOrder(request.getStageOrder())
                .estimatedDays(request.getEstimatedDays())
                .slaHours(request.getSlaHours())
                .requiresApproval(request.getRequiresApproval())
                .assigneeRole(request.getAssigneeRole())
                .requiresPayment(request.getRequiresPayment())
                .paymentPercent(request.getPaymentPercent())
                .workflowTemplate(template)
                .company(companyRef(companyId))
                .build();

        stageRepository.save(stage);
        template.setVersion(template.getVersion() + 1);
        templateRepository.save(template);

        
        return WorkflowMapper.toStageResponse(stage);
    }

    @Override
    @Transactional
    public WorkflowStageResponse updateStage(Long templateId, Long stageId,
                                             WorkflowStageRequest request) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_UPDATE);
        findTemplate(templateId);

        WorkflowStage stage = stageRepository.findById(stageId)
                .filter(s -> s.getWorkflowTemplate().getId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found: " + stageId));

        if (!stage.getStageOrder().equals(request.getStageOrder())
                && stageRepository.existsByWorkflowTemplateIdAndStageOrder(
                templateId, request.getStageOrder())) {
            throw new BadRequestException(
                    "Stage order " + request.getStageOrder() + " is already taken");
        }

        stage.setName(request.getName());
        stage.setStageOrder(request.getStageOrder());
        stage.setEstimatedDays(request.getEstimatedDays());
        stage.setSlaHours(request.getSlaHours());
        stage.setRequiresApproval(request.getRequiresApproval());
        stage.setAssigneeRole(request.getAssigneeRole());
        stage.setRequiresPayment(request.getRequiresPayment());
        stage.setPaymentPercent(request.getPaymentPercent());
        if (request.getDescription() != null)
            stage.setDescription(request.getDescription());

        stageRepository.save(stage);

        WorkflowTemplate template = stage.getWorkflowTemplate();
        template.setVersion(template.getVersion() + 1);
        templateRepository.save(template);

        return WorkflowMapper.toStageResponse(stage);
    }

    @Override
    @Transactional
    public void deleteStage(Long templateId, Long stageId) {
        authorizationService.checkPermission(PermissionCode.WORKFLOW_UPDATE);
        WorkflowTemplate template = findTemplate(templateId);

        WorkflowStage stage = stageRepository.findById(stageId)
                .filter(s -> s.getWorkflowTemplate().getId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found: " + stageId));

        stage.softDelete();
        stageRepository.save(stage);

        template.setVersion(template.getVersion() + 1);
        templateRepository.save(template);

        
    }

    // No @Transactional here on purpose: the template reads run inside
    // aiTx.load(), which commits before the provider call so no DB connection is
    // held across it - see AiTransactionBoundary. t.getStages() is a lazy
    // collection, so the summary has to be built inside the callback.
    @Override
    public WorkflowSuggestionResponse suggest(WorkflowSuggestionRequest request) {
        Long companyId = requireCompanyId();

        String prompt = aiTx.load(() -> {
            List<WorkflowTemplate> activeTemplates = templateRepository.findByCompanyIdAndActiveTrue(companyId);

            String existingSummary = activeTemplates.stream()
                .map(t -> "- " + t.getName() + ": " + t.getStages().stream()
                    .map(WorkflowStage::getName)
                    .reduce((a, b) -> a + " -> " + b)
                    .orElse("(no stages)"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);

            return WorkflowSuggestionPromptBuilder.builder()
                .setGoal(request.getGoal())
                .setExistingTemplatesSummary(existingSummary)
                .build();
        });

        String raw = aiService.generateRaw(AiFeature.WORKFLOW_SUGGESTION, prompt);
        WorkflowSuggestionResponse response = parseSuggestion(raw);
        return response;
    }

    /**
     * The prompt demands bare JSON, but models routinely wrap it in ```json
     * fences or lead with a sentence anyway. Strip fences, cut to the outer
     * braces, parse; if it still isn't valid JSON, hand the raw text back so
     * the UI can at least show what came back instead of an error.
     */
    private WorkflowSuggestionResponse parseSuggestion(String raw) {
        WorkflowSuggestionResponse response = new WorkflowSuggestionResponse();
        String candidate = raw == null ? "" : raw.replaceAll("```(?:json)?", "").trim();
        int start = candidate.indexOf('{');
        int end = candidate.lastIndexOf('}');
        if (start >= 0 && end > start) {
            candidate = candidate.substring(start, end + 1);
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                WorkflowSuggestionResponse parsed = mapper.readValue(candidate, WorkflowSuggestionResponse.class);
                if (parsed.getName() != null && parsed.getStages() != null && !parsed.getStages().isEmpty()) {
                    response.setName(parsed.getName());
                    response.setStages(parsed.getStages());
                    return response;
                }
            } catch (com.fasterxml.jackson.core.JacksonException ignored) {
                // fall through to raw
            }
        }
        response.setSuggestion(raw);
        return response;
    }

    // ── Private helpers ───────────────────────────────────────────

    private WorkflowTemplate findTemplate(Long id) {
        return templateRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workflow template not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
