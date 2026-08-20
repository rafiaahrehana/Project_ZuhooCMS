package com.businessos.modules.ai.enums;

public enum AiFeature {
    EMPLOYMENT_LETTER,
    LEAVE_POLICY,
    PERFORMANCE_REVIEW,
    CRM_LEAD_SUMMARY,
    CRM_ACTIVITY_SUMMARY,
    INVOICE_SUMMARY,
    SERVICE_REQUEST_SUMMARY,
    ANNOUNCEMENT_DRAFT,
    HOLIDAY_DRAFT,
    WORKFLOW_SUGGESTION,
    SEARCH_ANSWER,
    BUSINESS_INSIGHTS,
    GENERAL,
    // In-page "Compose" micro-assists: an employee is already on a form and
    // wants a quick draft from rough notes, not a conversation - same
    // generateRaw() path as the other *_DRAFT/*_SUMMARY features above.
    TIMESHEET_ENTRY,
    EXPENSE_ENTRY,
    DAILY_BRIEFING,
    // A thread created for the tool-calling agent (Leave/Attendance/
    // Timesheet/etc self-service) rather than a plain Q&A GENERAL thread -
    // cosmetic grouping only, the agent loop behaves identically either way.
    AGENT_TASK
}
