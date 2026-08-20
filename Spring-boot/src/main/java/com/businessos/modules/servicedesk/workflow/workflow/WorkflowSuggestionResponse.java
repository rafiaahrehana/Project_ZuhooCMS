package com.businessos.modules.servicedesk.workflow.workflow;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkflowSuggestionResponse {

    /** Raw model text - only set when the structured parse failed, as a fallback the UI can still show. */
    private String suggestion;

    /** Parsed suggestion - null when the model's output couldn't be parsed. */
    private String name;
    private List<SuggestedStage> stages;

    @Getter
    @Setter
    public static class SuggestedStage {
        private String name;
        private String purpose;
        private boolean needsApproval;
    }
}
