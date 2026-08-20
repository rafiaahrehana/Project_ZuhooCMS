package com.raf.zuhoo.data.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The small pure rules that drive dashboard counts and signup validation.
 */
public class StatusAndPolicyTest {

    // ── ServiceRequestStatus ─────────────────────────────────────

    @Test
    public void openServiceRequestStatuses() {
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.PENDING));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.QUOTATION_PENDING));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.ASSIGNED));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.IN_PROGRESS));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.WAITING_CLIENT));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.UNDER_REVIEW));
        assertTrue(ServiceRequestStatus.isOpen(ServiceRequestStatus.RESUBMITTED));
    }

    @Test
    public void terminalServiceRequestStatuses() {
        assertFalse(ServiceRequestStatus.isOpen(ServiceRequestStatus.COMPLETED));
        assertFalse(ServiceRequestStatus.isOpen(ServiceRequestStatus.REJECTED));
        assertFalse(ServiceRequestStatus.isOpen(ServiceRequestStatus.CANCELLED));
    }

    @Test
    public void nullStatusIsNotOpen() {
        // Guards the dashboard counters against a null slipping in from a partial response.
        assertFalse(ServiceRequestStatus.isOpen(null));
    }

    @Test
    public void unknownStatusCountsAsOpen() {
        // The backend enum has grown before and will again — a new in-progress state should be
        // counted as open rather than silently disappearing from the dashboard.
        assertTrue(ServiceRequestStatus.isOpen("SOME_FUTURE_STATE"));
    }

    // ── TicketStatus ─────────────────────────────────────────────

    @Test
    public void openTicketStatuses() {
        assertTrue(TicketStatus.isOpen(TicketStatus.NEW));
        assertTrue(TicketStatus.isOpen(TicketStatus.OPEN));
        assertTrue(TicketStatus.isOpen(TicketStatus.IN_PROGRESS));
        assertTrue(TicketStatus.isOpen(TicketStatus.WAITING));
        assertTrue(TicketStatus.isOpen(TicketStatus.ON_HOLD));
        assertTrue(TicketStatus.isOpen(TicketStatus.REOPENED));
    }

    @Test
    public void terminalTicketStatuses() {
        assertFalse(TicketStatus.isOpen(TicketStatus.RESOLVED));
        assertFalse(TicketStatus.isOpen(TicketStatus.CLOSED));
    }

    // ── PasswordPolicy ───────────────────────────────────────────

    @Test
    public void acceptsAPasswordMeetingEveryRule() {
        assertTrue(PasswordPolicy.isValid("Passw0rd!"));
        assertTrue(PasswordPolicy.isValid("An0ther#Good1"));
    }

    @Test
    public void rejectsPasswordsMissingACharacterClass() {
        assertFalse("no uppercase", PasswordPolicy.isValid("passw0rd!"));
        assertFalse("no lowercase", PasswordPolicy.isValid("PASSW0RD!"));
        assertFalse("no digit", PasswordPolicy.isValid("Password!"));
        assertFalse("no symbol", PasswordPolicy.isValid("Passw0rdd"));
    }

    @Test
    public void rejectsShortPasswords() {
        assertFalse(PasswordPolicy.isValid("Pw0rd!"));
    }

    @Test
    public void rejectsNull() {
        assertFalse(PasswordPolicy.isValid(null));
    }

    // ── Priority ─────────────────────────────────────────────────

    @Test
    public void serviceRequestPriorityUsesNormalNotMedium() {
        // ServiceRequestPriority and TicketPriority differ here; sending MEDIUM to a service
        // request is a 400 that reads like a mystery.
        org.junit.Assert.assertEquals("NORMAL", ServiceRequestPriority.NORMAL);
    }
}
