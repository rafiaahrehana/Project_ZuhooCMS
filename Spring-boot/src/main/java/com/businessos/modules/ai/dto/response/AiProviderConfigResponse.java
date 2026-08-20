package com.businessos.modules.ai.dto.response;

import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AiProviderConfigResponse {

    private Long id;
    private AiProviderType provider;
    private AiModel model;
    private BigDecimal temperature;
    private int maxTokens;
    private boolean active;
    private LocalDateTime createdAt;
}
