package com.businessos.modules.ai.mapper;

import com.businessos.modules.ai.dto.response.AiPromptTemplateResponse;
import com.businessos.modules.ai.dto.response.AiProviderConfigResponse;
import com.businessos.modules.ai.dto.response.AiThreadResponse;
import com.businessos.modules.ai.entity.AiConversationThread;
import com.businessos.modules.ai.entity.AiPromptTemplate;
import com.businessos.modules.ai.entity.AiProviderConfig;
import com.businessos.auth.user.User;

public  class AiMapper {

    public static AiProviderConfigResponse toConfigResponse(AiProviderConfig config) {
        AiProviderConfigResponse r = new AiProviderConfigResponse();
        r.setId(config.getId());
        r.setProvider(config.getAiProviderType());
        r.setModel(config.getAiModel());
        r.setTemperature(config.getTemperature() != null ? java.math.BigDecimal.valueOf(config.getTemperature()) : null);
        r.setMaxTokens(config.getMaxTokens());
        r.setActive(config.isActive());
        r.setCreatedAt(config.getCreatedAt());
        return r;
    }

    public static AiPromptTemplateResponse toTemplateResponse(AiPromptTemplate t) {
        User updatedBy = t.getUpdatedBy();
        AiPromptTemplateResponse r = new AiPromptTemplateResponse();
        r.setId(t.getId());
        r.setFeature(t.getFeature());
        r.setName(t.getName());
        r.setTemplate(t.getTemplate());
        r.setVersion(t.getVersion());
        r.setActive(t.isActive());
        r.setChangeNotes(t.getChangeNotes());
        r.setCompanyId(t.getCompany() != null ? t.getCompany().getId() : null);
        r.setUpdatedById(updatedBy != null ? updatedBy.getId() : null);
        r.setUpdatedByName(updatedBy != null ? updatedBy.getFullName() : null);
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    public static AiThreadResponse toThreadResponse(AiConversationThread thread) {
        AiThreadResponse r = new AiThreadResponse();
        r.setId(thread.getId());
        r.setFeature(thread.getFeature());
        r.setTitle(thread.getTitle());
        r.setCreatedAt(thread.getCreatedAt());
        r.setUpdatedAt(thread.getUpdatedAt());
        return r;
    }
}
