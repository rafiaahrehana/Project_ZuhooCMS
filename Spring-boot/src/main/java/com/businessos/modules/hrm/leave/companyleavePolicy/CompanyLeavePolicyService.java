package com.businessos.modules.hrm.leave.companyleavePolicy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompanyLeavePolicyService {
    /** ADMIN / OWNER: create a leave entitlement policy for a leave type */
    CompanyLeavePolicyResponse create(CompanyLeavePolicyRequest request);

    /** ADMIN / OWNER: get leave policy by id */
    CompanyLeavePolicyResponse getById(Long id);

    /** ADMIN / OWNER: list all leave policies with pagination */
    Page<CompanyLeavePolicyResponse> listAll(Pageable pageable);

    /** ALL: list active leave policies — used when applying for leave */
    List<CompanyLeavePolicyResponse> listActive();

    /** ADMIN / OWNER: update an existing leave policy */
    CompanyLeavePolicyResponse update(Long id, CompanyLeavePolicyRequest request);

    /** ADMIN / OWNER: soft-delete a leave policy */
    void delete(Long id);

    /** ADMIN / OWNER: draft a leave policy document with AI from the company's real configured entitlements */
    LeavePolicyDraftResponse draftWithAi(LeavePolicyDraftRequest request);
}
