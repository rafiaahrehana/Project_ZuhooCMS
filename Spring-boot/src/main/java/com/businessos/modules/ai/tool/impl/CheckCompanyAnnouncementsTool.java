package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.announcement.AnnouncementResponse;
import com.businessos.modules.hrm.announcement.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckCompanyAnnouncementsTool implements AiTool {

    private final AnnouncementService announcementService;

    @Override
    public String name() {
        return "check_company_announcements";
    }

    @Override
    public String description() {
        return "List currently active (published, not expired) company announcements.";
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
        List<AnnouncementResponse> announcements = announcementService.listActive();

        if (announcements.isEmpty()) {
            return AiToolResult.ok("There are no active company announcements right now.", announcements);
        }

        StringBuilder sb = new StringBuilder("Active announcements:\n");
        for (AnnouncementResponse a : announcements) {
            sb.append("- ").append(a.getTitle()).append(": ").append(a.getBody()).append('\n');
        }
        return AiToolResult.ok(sb.toString(), announcements);
    }
}
