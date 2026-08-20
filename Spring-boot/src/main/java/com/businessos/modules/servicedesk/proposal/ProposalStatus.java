package com.businessos.modules.servicedesk.proposal;

// The pre-sales lifecycle for a customized project, ahead of the formal
// Quotation: staff drafts what they intend to build (stack, timeline,
// mockups), sends it, and the client either accepts (staff then submits a
// binding Quotation) or asks for changes (staff edits and re-sends).
public enum ProposalStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    CHANGES_REQUESTED
}
