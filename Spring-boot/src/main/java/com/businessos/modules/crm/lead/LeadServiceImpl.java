package com.businessos.modules.crm.lead;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.CrmSummaryPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.servicedesk.companyservice.CompanyServiceRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.enums.*;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import com.businessos.modules.ai.support.PreparedPrompt;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LeadServiceImpl implements LeadService {

    private final EmployeeRepository employeeRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final AiService aiService;
    private final AiTransactionBoundary aiTx;
    private final LeadRepository leadRepository;
    private final com.businessos.modules.crm.activity.CrmActivityRepository leadActivityRepository;
    private final LeadMapper leadMapper;
    private final NotificationService notificationService;
    private final com.businessos.modules.crm.duplicate.DuplicateDetectionService duplicateDetectionService;
    private final com.businessos.modules.crm.tag.TagRepository tagRepository;
    private final com.businessos.modules.crm.opportunity.OpportunityService opportunityService;

    @Override
    @Transactional
    public LeadResponse createLead(LeadRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAD_CREATE);
        validateLeadRequest(request);
        Long companyId = requireCompanyId();

        // Check for duplicate email/phone - CSV import (LeadCsvImportService) already
        // checks both; manual/API creation only checked email, so two reps entering
        // the same walk-in lead by phone around the same time went undetected.
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (leadRepository.existsByEmailAndCompanyIdAndDeletedFalse(request.getEmail(), companyId)) {
                throw new BadRequestException("A lead with this email already exists in your company");
            }
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (leadRepository.existsByPhoneAndCompanyIdAndDeletedFalse(request.getPhone(), companyId)) {
                throw new BadRequestException("A lead with this phone number already exists in your company");
            }
        }

        Lead lead = Lead.builder()
                .contactName(request.getContactName())
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .industry(request.getIndustry())
                .jobTitle(request.getJobTitle())
                .notes(request.getNotes())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : LeadStatus.NEW)
                .source(request.getSource() != null ? request.getSource() : LeadSource.OTHER)
                .sourceOther(request.getSourceOther())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .estimatedValue(request.getEstimatedValue())
                .expectedCloseDate(request.getExpectedCloseDate())
                .company(companyRef(companyId))
                .build();

        if (request.getAssignedToId() != null) {
            Employee assignee = findEmployee(request.getAssignedToId(), companyId);
            lead.setAssignedTo(assignee);
        }

        if (request.getInterestedServiceId() != null) {
            lead.setInterestedService(
                    companyServiceRepository.findByIdAndCompanyId(request.getInterestedServiceId(), companyId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Service not found: " + request.getInterestedServiceId())));
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            lead.setTags(tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }

        Lead saved = leadRepository.save(lead);
        notifyAssignee(saved);

        LeadResponse response = leadMapper.toLeadResponse(saved);
        duplicateDetectionService
                .findPossibleDuplicateClient(saved.getCompanyName(), saved.getEmail(), saved.getPhone())
                .ifPresent(response::setPossibleDuplicate);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        return leadMapper.toLeadResponse(findLeadInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> listLeads(LeadStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.LEAD_VIEW);
        Long companyId = requireCompanyId();
        return (status != null
                ? leadRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                : leadRepository.findByCompanyId(companyId, pageable))
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> listMyLeads(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.LEAD_VIEW);
        Long companyId = requireCompanyId();
        Employee emp = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        return leadRepository.findByCompanyIdAndAssignedToId(companyId, emp.getId(), pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional
    public LeadResponse updateLead(Long id, LeadRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAD_UPDATE);
        validateLeadRequest(request);
        Long companyId = requireCompanyId();
        Lead lead = findLeadInTenant(id);

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(lead.getEmail())) {
            if (leadRepository.existsByEmailAndCompanyIdAndDeletedFalse(request.getEmail(), companyId)) {
                throw new BadRequestException("A lead with this email already exists in your company");
            }
        }

        // Prevent editing closed leads
        if (lead.isConverted() || lead.getStatus() == LeadStatus.DISQUALIFIED) {
            throw new BadRequestException("Cannot edit a closed lead");
        }

        // Update fields if provided
        if (request.getContactName() != null)
            lead.setContactName(request.getContactName());
        if (request.getCompanyName() != null)
            lead.setCompanyName(request.getCompanyName());
        if (request.getEmail() != null)
            lead.setEmail(request.getEmail());
        if (request.getPhone() != null)
            lead.setPhone(request.getPhone());
        if (request.getIndustry() != null)
            lead.setIndustry(request.getIndustry());
        if (request.getJobTitle() != null)
            lead.setJobTitle(request.getJobTitle());
        if (request.getNotes() != null)
            lead.setNotes(request.getNotes());
        if (request.getDescription() != null)
            lead.setDescription(request.getDescription());
        if (request.getStatus() != null)
            lead.setStatus(request.getStatus());
        if (request.getSource() != null)
            lead.setSource(request.getSource());
        if (request.getSourceOther() != null)
            lead.setSourceOther(request.getSourceOther());
        if (request.getPriority() != null)
            lead.setPriority(request.getPriority());
        if (request.getEstimatedValue() != null)
            lead.setEstimatedValue(request.getEstimatedValue());
        if (request.getExpectedCloseDate() != null)
            lead.setExpectedCloseDate(request.getExpectedCloseDate());

        boolean reassigned = false;
        if (request.getAssignedToId() != null) {
            Employee assignee = findEmployee(request.getAssignedToId(), companyId);
            reassigned = lead.getAssignedTo() == null || !lead.getAssignedTo().getId().equals(assignee.getId());
            lead.setAssignedTo(assignee);
        }

        if (request.getTagIds() != null) {
            lead.setTags(request.getTagIds().isEmpty()
                    ? new java.util.ArrayList<>()
                    : tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }

        Lead saved = leadRepository.save(lead);
        if (reassigned) notifyAssignee(saved);
        return leadMapper.toLeadResponse(saved);
    }

    @Override
    @Transactional
    public void deleteLead(Long id) {
        authorizationService.checkPermission(PermissionCode.LEAD_DELETE);
        Lead lead = findLeadInTenant(id);
        lead.setDeleted(true);
        lead.setDeletedAt(LocalDateTime.now());
        leadRepository.save(lead);

        // Also soft delete associated activities
        lead.getActivities().forEach(activity -> {
            activity.setDeleted(true);
            activity.setDeletedAt(LocalDateTime.now());
        });
    }

    // ==================== Search & Filter ====================

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> searchLeads(String keyword, Pageable pageable) {
        Long companyId = requireCompanyId();
        return leadRepository.searchLeads(companyId, escapeLikeKeyword(keyword), pageable)
                .map(leadMapper::toLeadResponse);
    }

    // '!' is the LIKE escape character used by LeadRepository's ESCAPE '!' queries -
    // an unescaped keyword containing '!' throws, and '%'/'_' match wrong rows.
    // Mirrors GlobalSearchServiceImpl.escapeLikeKeyword.
    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) return null;
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> filterLeads(LeadFilterRequest filter, Pageable pageable) {
        Long companyId = requireCompanyId();

        // Handle custom sorting
        if (filter.getSortBy() != null && filter.getSortDirection() != null) {
            Sort.Direction direction = Sort.Direction.fromString(filter.getSortDirection());
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(direction, filter.getSortBy()));
        }

        // If keyword is provided, use search instead
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            return searchLeads(filter.getKeyword(), pageable);
        }

        // Handle special filters
        if (Boolean.TRUE.equals(filter.getIsUnassigned())) {
            return findUnassignedLeads(companyId, pageable);
        }

        if (Boolean.TRUE.equals(filter.getIsHighPriority())) {
            return findHighPriorityOpenLeads(companyId, pageable);
        }

        // Standard filter
        return leadRepository.filterLeads(
                companyId,
                filter.getStatus(),
                filter.getSource(),
                filter.getPriority(),
                filter.getAssignedToId(),
                filter.getTagId(),
                pageable).map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findLeadsBySource(LeadSource source, Pageable pageable) {
        Long companyId = requireCompanyId();
        return leadRepository.findByCompanyIdAndSource(companyId, source, pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findLeadsByPriority(Priority priority, Pageable pageable) {
        Long companyId = requireCompanyId();
        return leadRepository.findByCompanyIdAndPriority(companyId, priority, pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findUnassignedLeads(Long companyId, Pageable pageable) {
        if (companyId == null) {
            companyId = requireCompanyId();
        }
        List<LeadStatus> closedStatuses = List.of(LeadStatus.DISQUALIFIED);
        return leadRepository.findUnassignedLeads(companyId, closedStatuses, pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findHighPriorityOpenLeads(Long companyId, Pageable pageable) {
        if (companyId == null) {
            companyId = requireCompanyId();
        }
        List<LeadStatus> closedStatuses = List.of(LeadStatus.DISQUALIFIED);
        return leadRepository.findHighPriorityOpenLeads(companyId, closedStatuses, pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findNeverContactedLeads(Pageable pageable) {
        Long companyId = requireCompanyId();
        return leadRepository.findNeverContactedLeads(companyId, pageable)
                .map(leadMapper::toLeadResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadResponse> findStalLeads(Pageable pageable) {
        Long companyId = requireCompanyId();
        java.time.LocalDate stalDate = java.time.LocalDate.now().minusDays(30);
        List<LeadStatus> closedStatuses = List.of(LeadStatus.DISQUALIFIED);
        return leadRepository.findStalLeads(companyId, stalDate, closedStatuses, pageable)
                .map(leadMapper::toLeadResponse);
    }

    // ==================== Lead Conversion ====================

    // Converts a Qualified Lead into an Opportunity - no Client or portal login is
    // created here. A Client is created/linked later, when the Opportunity reaches Won
    // (see OpportunityServiceImpl.changeStage).
    @Override
    @Transactional
    public com.businessos.modules.crm.opportunity.OpportunityResponse convertToOpportunity(Long id, ConvertToOpportunityRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAD_UPDATE);
        Lead lead = findLeadInTenant(id);

        if (lead.isConverted()) {
            throw new BadRequestException("Lead is already converted");
        }
        if (lead.getStatus() != LeadStatus.QUALIFIED) {
            throw new BadRequestException("Only a Qualified lead can be converted to an opportunity");
        }

        com.businessos.modules.crm.opportunity.OpportunityRequest opportunityRequest =
                new com.businessos.modules.crm.opportunity.OpportunityRequest();
        opportunityRequest.setName(request.getOpportunityName());
        opportunityRequest.setAmount(request.getExpectedValue());
        opportunityRequest.setExpectedCloseDate(request.getExpectedCloseDate());
        if (lead.getAssignedTo() != null) {
            opportunityRequest.setOwnerId(lead.getAssignedTo().getId());
        }

        com.businessos.modules.crm.opportunity.OpportunityResponse opportunity =
                opportunityService.createFromLead(id, opportunityRequest);

        lead.setConverted(true);
        lead.setConvertedAt(LocalDateTime.now());
        leadRepository.save(lead);

        return opportunity;
    }

    // ==================== Activity Management ====================

    @Override
    @Transactional
    public com.businessos.modules.crm.activity.CrmActivityResponse addActivity(Long leadId, com.businessos.modules.crm.activity.CrmActivityRequest request) {
        Long companyId = requireCompanyId();
        Lead lead = findLeadInTenant(leadId);

        com.businessos.modules.crm.activity.CrmActivity activity = com.businessos.modules.crm.activity.CrmActivity.builder()
                .lead(lead)
                .performedBy(securityUtil.getCurrentUser())
                .type(request.getType() != null ? request.getType() : com.businessos.modules.crm.activity.CrmActivityType.NOTE)
                .subject(request.getSubject())
                .description(request.getDescription())
                .activityDate(request.getActivityDate() != null ? request.getActivityDate() : LocalDateTime.now())
                .company(companyRef(companyId))
                .build();

        com.businessos.modules.crm.activity.CrmActivity saved = leadActivityRepository.save(activity);

        // Update lead's last activity
        lead.setLastActivityAt(LocalDateTime.now());
        lead.setStaleNotifiedAt(null);
        if (request.getType() == com.businessos.modules.crm.activity.CrmActivityType.CALL ||
                request.getType() == com.businessos.modules.crm.activity.CrmActivityType.MEETING ||
                request.getType() == com.businessos.modules.crm.activity.CrmActivityType.EMAIL) {
            lead.setLastContactDate(LocalDateTime.now().toLocalDate());
        }
        leadRepository.save(lead);

        return leadMapper.toActivityResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.businessos.modules.crm.activity.CrmActivityResponse> getActivities(Long leadId, Pageable pageable) {
        findLeadInTenant(leadId);
        return leadActivityRepository.findByLeadIdAndCompanyId(leadId, requireCompanyId(), pageable)
                .map(leadMapper::toActivityResponse);
    }

    @Override
    @Transactional
    public void deleteActivity(Long leadId, Long activityId) {
        findLeadInTenant(leadId);
        com.businessos.modules.crm.activity.CrmActivity activity = leadActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        if (!activity.getLead().getId().equals(leadId)) {
            throw new BadRequestException("Activity does not belong to this lead");
        }

        activity.setDeleted(true);
        activity.setDeletedAt(LocalDateTime.now());
        leadActivityRepository.save(activity);
    }

    // ==================== Dashboard & Reporting ====================

    @Override
    @Transactional(readOnly = true)
    public long countLeadsByStatus(LeadStatus status) {
        Long companyId = requireCompanyId();
        return leadRepository.countByCompanyIdAndStatusAndConvertedFalse(companyId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveLeads() {
        Long companyId = requireCompanyId();
        List<LeadStatus> closedStatuses = List.of(LeadStatus.DISQUALIFIED);
        return leadRepository.countActiveByCompanyId(companyId, closedStatuses);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyActiveLeads() {
        Long companyId = requireCompanyId();
        Employee emp = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        List<LeadStatus> closedStatuses = List.of(LeadStatus.DISQUALIFIED);
        return leadRepository.countActiveByAssignee(companyId, emp.getId(), closedStatuses);
    }

    // NOT_SUPPORTED overrides this class's @Transactional so the provider call
    // isn't wrapped in a transaction - see AiTransactionBoundary. The reads and
    // the mapping happen inside aiTx.load(), which commits before the AI call;
    // lead.getActivities() is lazy, so it has to be touched in there too.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LeadResponse summariseLead(Long id) {
        PreparedPrompt<LeadResponse> prepared = aiTx.load(() -> {
            Lead lead = findLeadInTenant(id);
            return new PreparedPrompt<>(
                    leadMapper.toLeadResponse(lead),
                    CrmSummaryPromptBuilder.builder()
                            .setContactName(lead.getContactName())
                            .setCompanyName(lead.getCompanyName())
                            .setCurrentStatus(lead.getStatus().name())
                            .setInterestedService(lead.getInterestedService() != null ? lead.getInterestedService().getName() : null)
                            .setActivityHistory("Activities count: " + lead.getActivities().size())
                            .build());
        });

        LeadResponse response = prepared.payload();
        try {
            response.setAiSummary(aiService.generateRaw(AiFeature.CRM_LEAD_SUMMARY, prepared.prompt()));
        } catch (Exception e) {
            log.error("Failed to generate AI summary for lead with ID {}: {}", id, e.getMessage(), e);
            throw new BadRequestException("Failed to generate AI summary: " + e.getMessage());
        }

        return response;
    }

    private void notifyAssignee(Lead lead) {
        Employee assignee = lead.getAssignedTo();
        if (assignee == null || assignee.getUser() == null) return;
        notificationService.send(CreateNotificationRequest.of(
                NotificationType.LEAD_ASSIGNED,
                "Lead Assigned",
                "Lead \"" + lead.getContactName() + "\" has been assigned to you.",
                "/crm/leads",
                assignee.getUser().getId(),
                lead.getCompany().getId()
        ));
    }

    private Lead findLeadInTenant(Long id) {
        Long companyId = requireCompanyId();
        return leadRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context found");
        }
        return companyId;
    }

    private Employee findEmployee(Long employeeId, Long companyId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private Company companyRef(Long companyId) {
        Company company = new Company();
        company.setId(companyId);
        return company;
    }

    private void validateLeadRequest(LeadRequest request) {
        if (request.getContactName() == null || request.getContactName().isBlank()) {
            throw new BadRequestException("Contact name is required");
        }
        if (request.getContactName().length() < 2) {
            throw new BadRequestException("Contact name must be at least 2 characters");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && !isValidEmail(request.getEmail())) {
            throw new BadRequestException("Email format is invalid");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
