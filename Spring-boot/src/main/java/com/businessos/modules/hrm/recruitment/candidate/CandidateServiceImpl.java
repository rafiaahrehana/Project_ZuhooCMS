package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.ApplicationSource;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public Candidate findOrCreate(Long companyId, String name, String email, String phone,
                                   ApplicationSource source, String resumeUrl, String linkedInUrl, String portfolioUrl) {
        String normalizedEmail = email.toLowerCase().trim();
        return candidateRepository.findByCompanyIdAndEmailIgnoreCase(companyId, normalizedEmail)
            .map(existing -> {
                // Refresh contact details from the latest application - the
                // original source stays as first recorded, not overwritten.
                if (name != null) existing.setName(name);
                if (phone != null) existing.setPhone(phone);
                if (resumeUrl != null) existing.setResumeUrl(resumeUrl);
                if (linkedInUrl != null) existing.setLinkedInUrl(linkedInUrl);
                if (portfolioUrl != null) existing.setPortfolioUrl(portfolioUrl);
                return existing;
            })
            .orElseGet(() -> {
                Company companyRef = new Company();
                companyRef.setId(companyId);
                Candidate candidate = Candidate.builder()
                    .company(companyRef)
                    .name(name)
                    .email(normalizedEmail)
                    .phone(phone)
                    .resumeUrl(resumeUrl)
                    .linkedInUrl(linkedInUrl)
                    .portfolioUrl(portfolioUrl)
                    .source(source)
                    .build();
                return candidateRepository.save(candidate);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        Candidate candidate = findInTenant(id);
        CandidateResponse response = CandidateResponse.from(candidate);
        response.setApplicationCount(applicationRepository.countByCompanyIdAndCandidateId(requireCompanyId(), id));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> list(String q, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_VIEW);
        Long companyId = requireCompanyId();
        Page<Candidate> page = (q != null && !q.isBlank())
            ? candidateRepository.search(companyId, q.trim(), pageable)
            : candidateRepository.findByCompanyId(companyId, pageable);
        return page.map(c -> {
            CandidateResponse r = CandidateResponse.from(c);
            r.setApplicationCount(applicationRepository.countByCompanyIdAndCandidateId(companyId, c.getId()));
            return r;
        });
    }

    @Override
    @Transactional
    public CandidateResponse update(Long id, CandidateRequest request) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_UPDATE);
        Candidate candidate = findInTenant(id);
        if (request.getName() != null && !request.getName().isBlank()) candidate.setName(request.getName().trim());
        if (request.getEmail() != null && !request.getEmail().isBlank()) candidate.setEmail(request.getEmail().toLowerCase().trim());
        candidate.setPhone(request.getPhone());
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setLinkedInUrl(request.getLinkedInUrl());
        candidate.setPortfolioUrl(request.getPortfolioUrl());
        candidate.setCurrentTitle(request.getCurrentTitle());
        candidate.setSkills(request.getSkills());
        if (request.getSource() != null) candidate.setSource(request.getSource());
        candidate.setNotes(request.getNotes());
        CandidateResponse response = CandidateResponse.from(candidate);
        response.setApplicationCount(applicationRepository.countByCompanyIdAndCandidateId(requireCompanyId(), id));
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.APPLICATION_DELETE);
        findInTenant(id).softDelete();
    }

    private Candidate findInTenant(Long id) {
        return candidateRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
