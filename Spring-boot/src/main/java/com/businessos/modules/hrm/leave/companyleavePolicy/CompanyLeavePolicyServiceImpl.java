package com.businessos.modules.hrm.leave.companyleavePolicy;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.LeavePolicyPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyLeavePolicy;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.LeaveType;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;

import java.util.List;

@Service
@RequiredArgsConstructor

public class CompanyLeavePolicyServiceImpl implements CompanyLeavePolicyService {

    private final CompanyLeavePolicyRepository policyRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;

    @Override
    @Transactional
    public CompanyLeavePolicyResponse create(CompanyLeavePolicyRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_CREATE);
        Long companyId = requireCompanyId();
        CompanyLeavePolicy policy = CompanyLeavePolicy.builder()
            .leaveType(request.getLeaveType())
            .employmentType(request.getEmploymentType())
            .annualEntitlement(request.getAnnualEntitlement())
            .maxCarryForward(request.getMaxCarryForward() != null ? request.getMaxCarryForward() : 0)
            .maxConsecutiveDays(request.getMaxConsecutiveDays())
            .requiresApproval(request.isRequiresApproval())
            .canCarryForward(request.isCanCarryForward())
            .paid(request.isPaid())
            .applicableFromMonths(request.getApplicableFromMonths() != null ? request.getApplicableFromMonths() : 0)
            .company(companyRef(companyId))
            .build();
        policyRepository.save(policy);
        return CompanyLeavePolicyMapper.toLeavePolicyResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyLeavePolicyResponse getById(Long id) {
        return CompanyLeavePolicyMapper.toLeavePolicyResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyLeavePolicyResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_VIEW);
        return policyRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(CompanyLeavePolicyMapper::toLeavePolicyResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyLeavePolicyResponse> listActive() {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_VIEW);
        return policyRepository.findByCompanyIdAndActiveTrue(requireCompanyId())
            .stream().map(CompanyLeavePolicyMapper::toLeavePolicyResponse).toList();
    }

    @Override
    @Transactional
    public CompanyLeavePolicyResponse update(Long id, CompanyLeavePolicyRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_UPDATE);
        CompanyLeavePolicy policy = findInTenant(id);
        policy.setLeaveType(request.getLeaveType());
        policy.setEmploymentType(request.getEmploymentType());
        policy.setAnnualEntitlement(request.getAnnualEntitlement());
        if (request.getMaxCarryForward()      != null) policy.setMaxCarryForward(request.getMaxCarryForward());
        if (request.getMaxConsecutiveDays()   != null) policy.setMaxConsecutiveDays(request.getMaxConsecutiveDays());
        policy.setRequiresApproval(request.isRequiresApproval());
        policy.setCanCarryForward(request.isCanCarryForward());
        policy.setPaid(request.isPaid());
        if (request.getApplicableFromMonths() != null) policy.setApplicableFromMonths(request.getApplicableFromMonths());
        return CompanyLeavePolicyMapper.toLeavePolicyResponse(policy);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_DELETE);
        findInTenant(id).softDelete();
    }

    // No @Transactional here on purpose: the company and policy lookups run
    // inside aiTx.load(), which commits before the provider call so no DB
    // connection is held across it - see AiTransactionBoundary.
    @Override
    public LeavePolicyDraftResponse draftWithAi(LeavePolicyDraftRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAVE_POLICY_CREATE);
        Long companyId = requireCompanyId();

        String prompt = aiTx.load(() -> {
            Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

            CompanyLeavePolicy annual = policyRepository
                .findApplicablePolicy(companyId, LeaveType.ANNUAL, EmploymentType.FULL_TIME)
                .orElseThrow(() -> new BadRequestException(
                    "Configure at least an Annual leave policy for Full-time employees before drafting a policy document"));
            CompanyLeavePolicy sick = policyRepository
                .findApplicablePolicy(companyId, LeaveType.SICK, EmploymentType.FULL_TIME)
                .orElse(null);

            return LeavePolicyPromptBuilder.builder()
                .setCompanyName(company.getCompanyName())
                .setAnnualLeaveDays(annual.getAnnualEntitlement())
                .setSickLeaveDays(sick != null ? sick.getAnnualEntitlement() : 0)
                .setRemoteWorkAllowed(request.isRemoteWorkAllowed())
                .setAdditionalContext(request.getAdditionalContext())
                .build();
        });

        LeavePolicyDraftResponse response = new LeavePolicyDraftResponse();
        response.setDocument(aiService.generateRaw(AiFeature.LEAVE_POLICY, prompt));
        return response;
    }

    private CompanyLeavePolicy findInTenant(Long id) {
        return policyRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Leave policy not found: " + id));
    }

    private Long requireCompanyId() {
        Long cid = securityUtil.getCurrentCompanyId();
        if (cid == null) throw new BadRequestException("No company context");
        return cid;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }
}
