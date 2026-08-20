package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.enums.LetterType;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import com.businessos.modules.ai.support.PreparedPrompt;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.EmploymentLetterPromptBuilder;
import com.businessos.modules.ai.service.AiService;

@Service
@RequiredArgsConstructor

public class OfferLetterServiceImpl implements OfferLetterService {

    private final OfferLetterRepository      letterRepository;
    private final EmployeeRepository         employeeRepository;
    private final JobApplicationRepository   jobApplicationRepository;
    private final CompanyRepository          companyRepository;
    private final SecurityUtil               securityUtil;
    private final AiService                  aiService;
    private final AiTransactionBoundary      aiTx;
    private final AuthorizationService       authorizationService;

    /** OFFER and APPOINTMENT letters go to a recruitment candidate, not an employee. */
    private boolean isPreEmploymentLetter(LetterType type) {
        return type == LetterType.OFFER || type == LetterType.APPOINTMENT;
    }

    /** Recipient resolved in the validation phase, carried across the AI call by id. */
    private record LetterRecipient(Long employeeId, Long applicationId,
                                  String recipientName, String recipientEmail) {}

    /*
     * Split into three phases so the AI call isn't inside a transaction - a single
     * @Transactional here held a pooled DB connection for as long as the provider
     * took to answer (see AiTransactionBoundary):
     *   1. validate + resolve the recipient, and build the prompt if needed  [tx]
     *   2. generate the letter body                                    [no tx]
     *   3. persist the letter                                               [tx]
     * Entities are carried between phases by id and re-read in phase 3, since
     * phase 1's are detached once it commits.
     */
    @Override
    public OfferLetterResponse create(OfferLetterRequest request) {
        authorizationService.checkPermission(PermissionCode.LETTER_CREATE);
        Long companyId = requireCompanyId();
        boolean candidateLetter = isPreEmploymentLetter(request.getLetterType());
        boolean needsAiContent = request.getContent() == null || request.getContent().isBlank();

        PreparedPrompt<LetterRecipient> prepared = aiTx.load(() -> {
            Employee employee = null;
            JobApplication application = null;
            String recipientName;
            String recipientEmail;

            if (candidateLetter) {
                // OFFER / APPOINTMENT — recipient is a recruitment candidate who hasn't joined yet.
                if (request.getJobApplicationId() == null) {
                    throw new BadRequestException(
                        request.getLetterType() + " letters must be addressed to a recruitment candidate");
                }
                application = jobApplicationRepository.findByIdAndCompanyId(request.getJobApplicationId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found: " + request.getJobApplicationId()));
                recipientName = application.getCandidate() != null ? application.getCandidate().getName() : null;
                recipientEmail = application.getCandidate() != null ? application.getCandidate().getEmail() : null;
            } else {
                // All other letters — recipient is an existing employee.
                if (request.getEmployeeId() == null) {
                    throw new BadRequestException(
                        request.getLetterType() + " letters must be addressed to an employee");
                }
                employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + request.getEmployeeId()));
                recipientName = employee.getUser() != null ? employee.getUser().getFullName() : null;
                recipientEmail = employee.getUser() != null ? employee.getUser().getEmail() : null;
            }

            if (request.getReferenceNumber() != null
                    && letterRepository.existsByCompanyIdAndReferenceNumber(companyId, request.getReferenceNumber())) {
                throw new BadRequestException("Reference number already exists: " + request.getReferenceNumber());
            }

            // Built here because both prompt builders read lazy associations
            // (application.getJobPosting(), employee.getDesignation()/getUser()).
            String prompt = !needsAiContent ? null
                : candidateLetter
                    ? buildCandidatePrompt(companyId, application, request.getLetterType().name())
                    : buildLetterPrompt(companyId, employee, request.getLetterType().name());

            return new PreparedPrompt<>(
                new LetterRecipient(
                    employee != null ? employee.getId() : null,
                    application != null ? application.getId() : null,
                    recipientName, recipientEmail),
                prompt);
        });

        LetterRecipient recipient = prepared.payload();
        String content = needsAiContent
            ? aiService.generateRaw(AiFeature.EMPLOYMENT_LETTER, prepared.prompt())
            : request.getContent();

        return aiTx.persist(() -> {
            OfferLetter letter = OfferLetter.builder()
                .employee(recipient.employeeId() == null ? null
                    : employeeRepository.getReferenceById(recipient.employeeId()))
                .jobApplication(recipient.applicationId() == null ? null
                    : jobApplicationRepository.getReferenceById(recipient.applicationId()))
                .recipientName(recipient.recipientName())
                .recipientEmail(recipient.recipientEmail())
                .company(companyRef(companyId))
                .letterType(request.getLetterType())
                .referenceNumber(request.getReferenceNumber())
                .issueDate(request.getIssueDate())
                .content(content)
                .signedBy(request.getSignedBy())
                .createdBy(securityUtil.getCurrentUser())
                .issued(false)
                .build();

            letterRepository.save(letter);
            return OfferletterMapper.toLetterResponse(letter);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OfferLetterResponse getById(Long id) {
        return OfferletterMapper.toLetterResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferLetterResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.LETTER_VIEW);
        return letterRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(OfferletterMapper::toLetterResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferLetterResponse> listForEmployee(Long employeeId, Pageable pageable) {
        return letterRepository.findByCompanyIdAndEmployeeId(requireCompanyId(), employeeId, pageable)
            .map(OfferletterMapper::toLetterResponse);
    }

    @Override
    @Transactional
    public OfferLetterResponse issue(Long id) {
        authorizationService.checkPermission(PermissionCode.LETTER_UPDATE);
        OfferLetter letter = findInTenant(id);
        if (letter.isIssued()) throw new BadRequestException("Letter is already issued");
        letter.setIssued(true);
        return OfferletterMapper.toLetterResponse(letter);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.LETTER_DELETE);
        OfferLetter letter = findInTenant(id);
        if (letter.isIssued()) throw new BadRequestException("Cannot delete an issued letter");
        letter.softDelete();
    }

    // No @Transactional here on purpose: the lookups and prompt building run
    // inside aiTx.load(), which commits before the provider call so no DB
    // connection is held across it - see AiTransactionBoundary.
    @Override
    public OfferLetterDraftResponse draftWithAi(OfferLetterDraftRequest request) {
        authorizationService.checkPermission(PermissionCode.LETTER_CREATE);
        Long companyId = requireCompanyId();

        String prompt = aiTx.load(() -> {
            if (isPreEmploymentLetter(request.getLetterType())) {
                if (request.getJobApplicationId() == null) {
                    throw new BadRequestException(
                        request.getLetterType() + " letters must be addressed to a recruitment candidate");
                }
                JobApplication application = jobApplicationRepository
                    .findByIdAndCompanyId(request.getJobApplicationId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found: " + request.getJobApplicationId()));
                return buildCandidatePrompt(companyId, application, request.getLetterType().name());
            }
            if (request.getEmployeeId() == null) {
                throw new BadRequestException(
                    request.getLetterType() + " letters must be addressed to an employee");
            }
            Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Employee not found: " + request.getEmployeeId()));
            return buildLetterPrompt(companyId, employee, request.getLetterType().name());
        });

        OfferLetterDraftResponse response = new OfferLetterDraftResponse();
        response.setContent(aiService.generateRaw(AiFeature.EMPLOYMENT_LETTER, prompt));
        return response;
    }

    private String buildCandidatePrompt(Long companyId, JobApplication application, String letterType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        return EmploymentLetterPromptBuilder.builder()
            .setCompanyName(company.getCompanyName())
            .setEmployeeName(application.getCandidate() != null ? application.getCandidate().getName() : null)
            .setDesignation(application.getJobPosting() != null ? application.getJobPosting().getTitle() : "Not specified")
            .setDepartment("Not specified")
            // The candidate hasn't joined, so there's no hire date yet — use today as
            // a placeholder proposed date for the draft (the content is editable).
            .setJoiningDate(LocalDate.now())
            .setLetterType(letterType)
            .build();
    }

    private String buildLetterPrompt(Long companyId, Employee employee, String letterType) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

        return EmploymentLetterPromptBuilder.builder()
            .setCompanyName(company.getCompanyName())
            .setEmployeeName(employee.getUser().getFullName())
            .setDesignation(employee.getDesignation() != null ? employee.getDesignation().getName() : employee.getJobTitle())
            .setDepartment(employee.getDepartment() != null ? employee.getDepartment().getName() : "Not specified")
            .setJoiningDate(employee.getHireDate())
            .setLetterType(letterType)
            .build();
    }

    private OfferLetter findInTenant(Long id) {
        return letterRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Employment letter not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }
}
