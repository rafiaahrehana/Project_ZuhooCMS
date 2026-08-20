package com.businessos.modules.ai.audit;

import com.businessos.modules.ai.entity.AiConversation;
import com.businessos.modules.ai.entity.AiConversationThread;
import com.businessos.modules.ai.entity.AiUsageLog;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import com.businessos.modules.ai.repository.AiConversationRepository;
import com.businessos.modules.ai.repository.AiUsageLogRepository;
import com.businessos.modules.ai.util.AiTokenCounter;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class AiAuditService {

    private final AiConversationRepository conversationRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final AiTokenCounter tokenCounter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String record(AiFeature feature, AiProviderType provider, AiModel model,
                         String prompt, String response, long executionTimeMs,
                         User user, Company company) {
        return record(feature, provider, model, prompt, response, executionTimeMs, user, company, null);
    }

    /** Same as the five-arg overload, plus linking the exchange to a resumable thread. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String record(AiFeature feature, AiProviderType provider, AiModel model,
                         String prompt, String response, long executionTimeMs,
                         User user, Company company, AiConversationThread thread) {
        String uuid = UUID.randomUUID().toString();

        AiConversation conversation = AiConversation.builder()
                .conversationUuid(uuid)
                .feature(feature)
                .provider(provider)
                .model(model)
                .requestPayload(prompt)
                .responsePayload(response)
                .executionTimeMs(executionTimeMs)
                .company(company)
                .thread(thread)
                .build();
        conversationRepository.save(conversation);

        AiUsageLog usageLog = AiUsageLog.builder()
             .aiFeature(feature)
            .provider(provider)
            .model(model)
            .inputTokens(tokenCounter.estimate(prompt))
            .outputTokens(tokenCounter.estimate(response))
            .executionTimeMs(executionTimeMs)
            .logDate(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .user(user)
            .company(company)
            .build();
        usageLogRepository.save(usageLog);

        return uuid;
    }
}

