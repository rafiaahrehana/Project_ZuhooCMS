package com.businessos.modules.hrm.announcement;

import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.AnnouncementDraftPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.department.Department;
import com.businessos.enums.AnnouncementAudience;
import com.businessos.enums.NotificationType;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.department.DepartmentRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.NotificationService;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor

public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final CompanyRepository companyRepository;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final EmailBranding emailBranding;

    @Override
    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_CREATE);
        Long companyId = requireCompanyId();

        rejectUnsupportedAudience(request.getAudience());
        Department targetDept = null;
        if (request.getTargetDepartmentId() != null) {
            targetDept = departmentRepository.findByIdAndCompanyId(request.getTargetDepartmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Department not found: " + request.getTargetDepartmentId()));
        }

        Announcement announcement = Announcement.builder()
            .title(request.getTitle())
            .body(request.getBody())
            .audience(request.getAudience() != null ? request.getAudience() : AnnouncementAudience.ALL)
            .targetDepartment(targetDept)
            .expiresAt(request.getExpiresAt())
            .scheduledAt(request.getScheduledAt())
            .notifyAll(request.isNotifyAll())
            .priority(request.getPriority() != null ? request.getPriority() : 0)
            .attachmentUrl(request.getAttachmentUrl())
            .published(false)
            .company(companyRef(companyId))
            .createdBy(securityUtil.getCurrentUser())
            .build();

        announcementRepository.save(announcement);
        return AnnouncementMapper.toAnnouncementResponse(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        return AnnouncementMapper.toAnnouncementResponse(findInTenant(id));
    }



    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_VIEW);
        return announcementRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(AnnouncementMapper::toAnnouncementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listActive() {
        // Read-only "notice board" feed - any logged-in employee can see active
        // announcements for their own company, regardless of ANNOUNCEMENT_VIEW.
        // Creating/editing/publishing announcements stays permission-gated.
        //
        // Previously this returned every active announcement regardless of its
        // audience - a DEPARTMENT-targeted announcement was readable company-wide
        // even though only its notification (if notifyAll was checked) was
        // actually scoped. The feed itself is now filtered the same way.
        Long companyId = requireCompanyId();
        List<Announcement> active = announcementRepository.findActiveByCompanyId(companyId, LocalDateTime.now());

        com.businessos.auth.user.User currentUser = securityUtil.getCurrentUser();
        com.businessos.modules.hrm.employee.Employee me = currentUser != null
                ? employeeRepository.findByUserId(currentUser.getId()).orElse(null) : null;
        // No employee profile (e.g. the company owner) sees everything - there is
        // no meaningful "my department"/"am I a manager" to scope them by.
        if (me == null) {
            return active.stream().map(AnnouncementMapper::toAnnouncementResponse).toList();
        }
        boolean iAmManager = employeeRepository.existsByCompanyIdAndReportingManagerIdAndActiveTrue(companyId, me.getId());

        return active.stream()
            .filter(a -> appliesToViewer(a, me, iAmManager))
            .map(AnnouncementMapper::toAnnouncementResponse).toList();
    }

    private boolean appliesToViewer(Announcement a, com.businessos.modules.hrm.employee.Employee me, boolean iAmManager) {
        return switch (a.getAudience()) {
            case MANAGERS -> iAmManager;
            case EMPLOYEES -> !iAmManager;
            case DEPARTMENT -> a.getTargetDepartment() != null && me.getDepartment() != null
                    && me.getDepartment().getId().equals(a.getTargetDepartment().getId());
            case ALL, SPECIFIC -> true; // SPECIFIC has no recipient-list model yet - see rejectUnsupportedAudience
        };
    }

    /** SPECIFIC has no targetEmployeeIds model on the entity yet - reject rather than silently notifying/showing everyone. */
    private void rejectUnsupportedAudience(AnnouncementAudience audience) {
        if (audience == AnnouncementAudience.SPECIFIC) {
            throw new BadRequestException(
                    "Targeting specific recipients isn't available yet - choose All, Employees, Managers, or Department");
        }
    }

    @Override
    @Transactional
    public AnnouncementResponse publish(Long id) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_UPDATE);
        Long companyId = requireCompanyId();
        Announcement announcement = findInTenant(id);
        if (announcement.isPublished()) throw new BadRequestException("Announcement is already published");
        doPublish(announcement, companyId);
        return AnnouncementMapper.toAnnouncementResponse(announcement);
    }

    /**
     * System entry point for AnnouncementScheduledPublishScheduler - no security
     * context, so unlike publish(id) it takes the company id from the entity
     * itself rather than SecurityUtil, and skips the permission check (there is
     * no acting user to check permissions against).
     */
    @Override
    @Transactional
    public void publishDueScheduled() {
        for (Announcement announcement : announcementRepository
                .findByPublishedFalseAndDeletedFalseAndScheduledAtLessThanEqual(LocalDateTime.now())) {
            doPublish(announcement, announcement.getCompany().getId());
        }
    }

    private void doPublish(Announcement announcement, Long companyId) {
        announcement.setPublished(true);
        announcement.setPublishedAt(LocalDateTime.now());

        if (announcement.isNotifyAll()) {
            // The checkbox is labelled "Send Email Notification to All" but this
            // used to only create in-app NotificationService entries - nothing
            // was ever emailed, so the label promised something the code never did.
            EmailBranding.Data branding = null;
            try {
                Company fullCompany = companyRepository.findById(companyId).orElse(null);
                if (fullCompany != null) branding = emailBranding.from(fullCompany);
            } catch (Exception ex) {
                log.warn("Could not load branding for announcement email (company {}): {}", companyId, ex.getMessage());
            }

            int pageNum = 0;
            final int PAGE_SIZE = 100;
            org.springframework.data.domain.Page<com.businessos.modules.hrm.employee.Employee> page;
            do {
                // Audience-scoped notification fan-out: EMPLOYEES/MANAGERS used to
                // fall through to "everyone" here, same as DEPARTMENT without a
                // target used to. Each branch now queries only its real audience.
                page = switch (announcement.getAudience()) {
                    case DEPARTMENT -> announcement.getTargetDepartment() != null
                            ? employeeRepository.findByCompanyIdAndDepartmentId(
                                companyId, announcement.getTargetDepartment().getId(), PageRequest.of(pageNum, PAGE_SIZE))
                            : employeeRepository.findByCompanyId(companyId, PageRequest.of(pageNum, PAGE_SIZE));
                    case MANAGERS -> employeeRepository.findManagersByCompanyId(companyId, PageRequest.of(pageNum, PAGE_SIZE));
                    case EMPLOYEES -> employeeRepository.findNonManagersByCompanyId(companyId, PageRequest.of(pageNum, PAGE_SIZE));
                    default -> employeeRepository.findByCompanyId(companyId, PageRequest.of(pageNum, PAGE_SIZE));
                };
                EmailBranding.Data brandingForLoop = branding;
                page.getContent().forEach(emp -> {
                    if (emp.getUser() != null) {
                        notificationService.send(CreateNotificationRequest.of(
                            NotificationType.ANNOUNCEMENT,
                            announcement.getTitle(),
                            announcement.getBody().length() > 150
                                ? announcement.getBody().substring(0, 147) + "..."
                                : announcement.getBody(),
                            "/announcements/" + announcement.getId(),
                            emp.getUser().getId(),
                            companyId
                        ));
                        if (brandingForLoop != null && emp.getUser().getEmail() != null) {
                            try {
                                emailService.sendAnnouncementEmail(
                                    emp.getUser().getEmail(), emp.getUser().getFirstName(),
                                    announcement.getTitle(), announcement.getBody(), brandingForLoop);
                            } catch (Exception ex) {
                                log.warn("Announcement email failed for {}: {}", emp.getUser().getEmail(), ex.getMessage());
                            }
                        }
                    }
                });
                pageNum++;
            } while (page.hasNext());
        }
    }

    @Override
    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_UPDATE);
        Long companyId = requireCompanyId();
        Announcement announcement = findInTenant(id);
        if (announcement.isPublished()) throw new BadRequestException("Cannot edit a published announcement");
        if (request.getTitle()!= null) announcement.setTitle(request.getTitle());
        if (request.getBody()!= null) announcement.setBody(request.getBody());
        if (request.getAudience()!= null) {
            rejectUnsupportedAudience(request.getAudience());
            announcement.setAudience(request.getAudience());
        }
        if (request.getAttachmentUrl()!= null) announcement.setAttachmentUrl(request.getAttachmentUrl());
        if (request.getPriority()!= null) announcement.setPriority(request.getPriority());
        announcement.setExpiresAt(request.getExpiresAt());
        announcement.setScheduledAt(request.getScheduledAt());
        announcement.setNotifyAll(request.isNotifyAll());
        if (request.getTargetDepartmentId() != null) {
            announcement.setTargetDepartment(
                departmentRepository.findByIdAndCompanyId(request.getTargetDepartmentId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }
        return AnnouncementMapper.toAnnouncementResponse(announcement);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_DELETE);
        Announcement a = findInTenant(id);
        if (a.isPublished()) throw new BadRequestException("Cannot delete a published announcement");
        a.softDelete();
    }

    // No @Transactional here on purpose: the company lookup runs inside
    // aiTx.load(), which commits before the provider call so no DB connection is
    // held across it - see AiTransactionBoundary.
    @Override
    public AnnouncementDraftResponse draftWithAi(AnnouncementDraftRequest request) {
        authorizationService.checkPermission(PermissionCode.ANNOUNCEMENT_CREATE);
        Long companyId = requireCompanyId();

        String prompt = aiTx.load(() -> {
            Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

            return AnnouncementDraftPromptBuilder.builder()
                .setCompanyName(company.getCompanyName())
                .setToday(LocalDate.now())
                .setInstructions(request.getInstructions())
                .build();
        });

        String raw = aiService.generateRaw(AiFeature.ANNOUNCEMENT_DRAFT, prompt);
        return parseDraft(raw, request.getInstructions());
    }

    private AnnouncementDraftResponse parseDraft(String raw, String fallbackInstructions) {
        AnnouncementDraftResponse response = new AnnouncementDraftResponse();
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
            }
            JsonNode node = objectMapper.readTree(cleaned);
            response.setTitle(node.path("title").asText(null));
            response.setBody(node.path("body").asText(null));
        } catch (Exception ignored) {
            // Model didn't return valid JSON despite instructions - fall back to the
            // raw text as the body rather than failing the whole request.
        }
        if (response.getTitle() == null || response.getTitle().isBlank()) {
            response.setTitle(fallbackInstructions.length() > 80
                ? fallbackInstructions.substring(0, 77) + "..." : fallbackInstructions);
        }
        if (response.getBody() == null || response.getBody().isBlank()) {
            response.setBody(raw);
        }
        return response;
    }

    private Announcement findInTenant(Long id) {
        return announcementRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + id));
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

