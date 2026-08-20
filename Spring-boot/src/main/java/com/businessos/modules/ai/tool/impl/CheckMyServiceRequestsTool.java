package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestResponse;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckMyServiceRequestsTool implements AiTool {

    private final ServiceRequestService serviceRequestService;

    @Override
    public String name() {
        return "check_my_service_requests";
    }

    @Override
    public String description() {
        return "List the service requests currently assigned to this employee, with status and SLA.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of("type", "object", "properties", Map.of());
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        List<ServiceRequestResponse> requests = serviceRequestService
            .listAssignedToMe(PageRequest.of(0, 10)).getContent();

        if (requests.isEmpty()) {
            return AiToolResult.ok("No service requests are currently assigned to you.", requests);
        }

        StringBuilder sb = new StringBuilder("Assigned to you:\n");
        for (ServiceRequestResponse r : requests) {
            sb.append("- #").append(r.getId()).append(" \"").append(r.getTitle()).append("\": ")
              .append(r.getStatus());
            if (r.isSlaBreach()) sb.append(" (SLA BREACHED)");
            sb.append('\n');
        }
        return AiToolResult.ok(sb.toString(), requests);
    }
}
