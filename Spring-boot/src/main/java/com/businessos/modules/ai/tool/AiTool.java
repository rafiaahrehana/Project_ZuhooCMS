package com.businessos.modules.ai.tool;

import com.businessos.auth.role.enums.PermissionCode;

import java.util.Map;

/**
 * One action the AI agent can take on an employee's behalf. Every real
 * implementation is a thin wrapper around a service method that already
 * exists and already scopes itself to the caller via SecurityUtil - a tool
 * adds no new capability, it just gives that existing action a
 * natural-language front door.
 *
 * Non-negotiable rule enforced by every implementation, not just documented:
 * {@link #execute} must resolve *what to act on* only from the userId/
 * companyId parameters (which the agent loop always fills from the real
 * authenticated SecurityUtil values, never from anything the model said) -
 * never from a caller-suppliable id inside {@code args}.
 */
public interface AiTool {

    /** Stable, model-facing identifier, e.g. "check_leave_balance". Never renamed once shipped. */
    String name();

    /** One or two sentences the model uses to decide when this tool applies. */
    String description();

    /**
     * A small JSON-Schema-shaped description of the arguments this tool
     * accepts, e.g. {@code {"type":"object","properties":{"startDate":{"type":"string","format":"date"}}}}.
     * Translated per-provider by each AiHttpClient's callWithTools().
     */
    Map<String, Object> parametersSchema();

    /**
     * True for anything that changes real data (submitting a leave request,
     * logging a timesheet entry). Write tools are never executed on the
     * agent's first pass - see AiServiceImpl#runAgentTurn - regardless of
     * what this flag says, so it exists mainly for logging/UI labeling, not
     * as the actual safety gate.
     */
    boolean isWrite();

    /**
     * Null when any authenticated employee may use this tool. Non-null tools
     * are filtered out of what's even sent to the model for a user lacking
     * this permission - see AiToolRegistry#availableFor - so the model can
     * never be asked to call, and never attempts to call, a tool the caller
     * isn't authorized for.
     */
    default PermissionCode requiredPermission() {
        return null;
    }

    /**
     * Human-readable summary of what {@link #execute} would do with these
     * args, shown to the employee as the confirm/cancel proposal before a
     * write tool ever runs. Only called for write tools. Default is
     * functional but generic - override for anything where "apply_leave
     * with {startDate=..., endDate=...}" reads worse than a real sentence.
     */
    default String describeProposal(Map<String, Object> args) {
        return "run " + name() + " with " + args;
    }

    /**
     * Runs the wrapped action. userId/companyId are always the real
     * authenticated caller, resolved by the agent loop before this is
     * invoked - implementations must use them (or the SecurityUtil-backed
     * service they wrap) exclusively, never an id parsed out of {@code args}.
     */
    AiToolResult execute(Map<String, Object> args, Long userId, Long companyId);
}
