package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.enums.ApplicationSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CandidateService {

    /**
     * Finds the existing candidate for this company+email (case-insensitive), or
     * creates one. Applying again with the same email reuses the same person
     * instead of creating a duplicate - contact details are refreshed from the
     * latest application, but the original source is left untouched.
     */
    Candidate findOrCreate(Long companyId, String name, String email, String phone,
                            ApplicationSource source, String resumeUrl, String linkedInUrl, String portfolioUrl);

    CandidateResponse getById(Long id);

    Page<CandidateResponse> list(String q, Pageable pageable);

    CandidateResponse update(Long id, CandidateRequest request);

    void delete(Long id);
}
