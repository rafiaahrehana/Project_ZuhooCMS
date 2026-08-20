package com.businessos.core.automation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Base cross-module event. All business events carry companyId for
 * tenant isolation — handlers must filter on it.
 */
@Getter
public abstract class BusinessEvent extends ApplicationEvent {

    private final Long companyId;
    private final String eventType;

    protected BusinessEvent(Object source, Long companyId, String eventType) {
        super(source);
        this.companyId = companyId;
        this.eventType = eventType;
    }
}
