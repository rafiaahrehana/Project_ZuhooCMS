package com.businessos.modules.hrm.announcement;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.department.Department;

public class AnnouncementMapper {
    public static AnnouncementResponse toAnnouncementResponse(Announcement a) {
        User createdBy = a.getCreatedBy();
        Department dept = a.getTargetDepartment();
        AnnouncementResponse r = new AnnouncementResponse();
        r.setId(a.getId());
        r.setTitle(a.getTitle());
        r.setBody(a.getBody());
        r.setAudience(a.getAudience());
        r.setTargetDepartmentId(dept != null ? dept.getId() : null);
        r.setTargetDepartmentName(dept != null ? dept.getName() : null);
        r.setPublishedAt(a.getPublishedAt());
        r.setExpiresAt(a.getExpiresAt());
        r.setScheduledAt(a.getScheduledAt());
        r.setPublished(a.isPublished());
        r.setNotifyAll(a.isNotifyAll());
        r.setPriority(a.getPriority());
        r.setAttachmentUrl(a.getAttachmentUrl());
        r.setCreatedById(createdBy != null ? createdBy.getId() : null);
        r.setCreatedByName(createdBy != null ? createdBy.getFullName() : null);
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
