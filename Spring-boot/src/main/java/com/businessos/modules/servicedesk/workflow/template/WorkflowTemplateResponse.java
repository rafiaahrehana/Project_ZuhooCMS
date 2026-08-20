package com.businessos.modules.servicedesk.workflow.template;

import com.businessos.modules.servicedesk.workflow.stage.WorkflowStageResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class WorkflowTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private int version;
    private boolean active;
    private List<WorkflowStageResponse> stages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
