package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.enums.ApplicationSource;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CandidateResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String resumeUrl;
    private String linkedInUrl;
    private String portfolioUrl;
    private String currentTitle;
    private String skills;
    private ApplicationSource source;
    private String notes;
    private long applicationCount;
    private LocalDateTime createdAt;

    public static CandidateResponse from(Candidate c) {
        CandidateResponse r = new CandidateResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.email = c.getEmail();
        r.phone = c.getPhone();
        r.resumeUrl = c.getResumeUrl();
        r.linkedInUrl = c.getLinkedInUrl();
        r.portfolioUrl = c.getPortfolioUrl();
        r.currentTitle = c.getCurrentTitle();
        r.skills = c.getSkills();
        r.source = c.getSource();
        r.notes = c.getNotes();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}
