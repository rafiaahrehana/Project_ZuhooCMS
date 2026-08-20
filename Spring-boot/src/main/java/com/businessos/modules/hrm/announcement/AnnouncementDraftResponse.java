package com.businessos.modules.hrm.announcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDraftResponse {
    private String title;
    private String body;
}
