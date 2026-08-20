package com.businessos.modules.hrm.recruitment.offerletter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfferLetterService {
    /** ADMIN / OWNER: create a draft employment letter */
    OfferLetterResponse create(OfferLetterRequest request);

    /** ADMIN / OWNER / EMPLOYEE: get letter by id */
    OfferLetterResponse getById(Long id);

    /** ADMIN / OWNER: list all letters with pagination */
    Page<OfferLetterResponse> listAll(Pageable pageable);

    /** ADMIN / OWNER / EMPLOYEE: list letters for a specific employee */
    Page<OfferLetterResponse> listForEmployee(Long employeeId, Pageable pageable);

    /** ADMIN / OWNER: issue letter — locks content from further edits */
    OfferLetterResponse issue(Long id);

    /** ADMIN / OWNER: soft-delete an unissued letter */
    void delete(Long id);

    /** ADMIN / OWNER: draft letter content with AI from real employee/company data - not persisted */
    OfferLetterDraftResponse draftWithAi(OfferLetterDraftRequest request);
}
