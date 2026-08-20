package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class ServiceRequestReplyDraftPromptBuilder {

    private String title;
    private String status;
    private String recentComments;
    private String roughNotes;

    public static ServiceRequestReplyDraftPromptBuilder builder() {
        return new ServiceRequestReplyDraftPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Draft a reply comment for this service request, based on the requester's rough notes.

            Request Title      : %s
            Status             : %s
            Recent Comments    :
            %s

            Rough notes for the reply:
            %s

            Output instructions:
            - Write ONLY the reply text - no preamble, no markdown, no signature.
            - Clear, professional, and courteous tone, 2-4 sentences.
            - Reflect the rough notes faithfully - do not invent facts not present in them.
            """.formatted(
                title,
                status,
                PromptSupport.orDefault(recentComments, "No comments yet"),
                roughNotes
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(title, "title", "service request reply draft");
        PromptSupport.requireNonBlank(roughNotes, "roughNotes", "service request reply draft");
    }
}
