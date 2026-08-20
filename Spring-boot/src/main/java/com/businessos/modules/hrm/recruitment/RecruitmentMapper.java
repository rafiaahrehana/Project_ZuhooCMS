package com.businessos.modules.hrm.recruitment;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.recruitment.candidate.Candidate;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationResponse;
import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;

public class RecruitmentMapper {

    public static JobApplicationResponse toJobApplicationResponse(JobApplication a) {
        JobPosting jp = a.getJobPosting();
        User reviewerUser = a.getReviewedBy();
        Candidate c = a.getCandidate();

        JobApplicationResponse r = new JobApplicationResponse();
        r.setId(a.getId());
        r.setCandidateId(c != null ? c.getId() : null);
        r.setCandidateName(c != null ? c.getName() : null);
        r.setCandidateEmail(c != null ? c.getEmail() : null);
        r.setCandidatePhone(c != null ? c.getPhone() : null);
        r.setResumeUrl(c != null ? c.getResumeUrl() : null);
        r.setLinkedInUrl(c != null ? c.getLinkedInUrl() : null);
        r.setPortfolioUrl(c != null ? c.getPortfolioUrl() : null);
        r.setCoverLetter(a.getCoverLetter());
        r.setSource(a.getSource());
        r.setStatus(a.getStatus());
        r.setNotes(a.getInterviewNotes());
        r.setJobPostingId(jp != null ? jp.getId() : null);
        r.setJobPostingTitle(jp != null ? jp.getTitle() : null);
        r.setReviewedById(reviewerUser != null ? reviewerUser.getId() : null);
        r.setReviewedByName(reviewerUser != null ? reviewerUser.getFullName() : null);
        r.setConvertedEmployeeId(a.getConvertedEmployee() != null ? a.getConvertedEmployee().getId() : null);
        r.setConvertedAt(a.getConvertedAt());
        r.setScoreEducation(a.getScoreEducation());
        r.setScoreExperience(a.getScoreExperience());
        r.setScoreTechnicalSkills(a.getScoreTechnicalSkills());
        r.setScoreInterview(a.getScoreInterview());
        r.setScoreCommunication(a.getScoreCommunication());
        r.setOverallScore(a.getOverallScore());
        r.setAtsScore(a.getAtsScore());
        r.setAtsMatchedRequiredSkills(a.getAtsMatchedRequiredSkills());
        r.setAtsMissingRequiredSkills(a.getAtsMissingRequiredSkills());
        r.setAtsMatchedPreferredSkills(a.getAtsMatchedPreferredSkills());
        r.setAtsExtractedExperienceYears(a.getAtsExtractedExperienceYears());
        r.setAtsMeetsEducationRequirement(a.getAtsMeetsEducationRequirement());
        r.setAtsParseStatus(a.getAtsParseStatus());
        r.setAtsParsedAt(a.getAtsParsedAt());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
