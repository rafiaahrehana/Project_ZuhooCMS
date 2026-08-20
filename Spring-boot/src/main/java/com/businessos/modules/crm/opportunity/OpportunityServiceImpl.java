package com.businessos.modules.crm.opportunity;

import com.businessos.core.automation.AutomationEventPublisher;
import com.businessos.enums.LeadSource;
import com.businessos.enums.NotificationType;
import com.businessos.modules.crm.activity.CrmActivityType;
import com.businessos.modules.crm.activity.CrmActivityService;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.crm.client.ClientRepository;
import com.businessos.modules.crm.contact.ClientContact;
import com.businessos.modules.crm.contact.ClientContactRepository;
import com.businessos.modules.crm.lead.Lead;
import com.businessos.modules.crm.lead.LeadRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.crm.duplicate.DuplicateDetectionService;
import com.businessos.modules.crm.duplicate.DuplicateMatch;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.enums.ClientStatus;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository clientContactRepository;
    private final LeadRepository leadRepository;
    private final EmployeeRepository employeeRepository;
    private final CrmActivityService crmActivityService;
    private final AutomationEventPublisher automationEventPublisher;
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final DuplicateDetectionService duplicateDetectionService;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.crm.tag.TagRepository tagRepository;

    @Override
    public OpportunityResponse create(OpportunityRequest request) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_CREATE);
        Long companyId = requireCompanyId();
        if (request.getClientId() == null) {
            throw new BadRequestException("Client is required when creating a deal directly");
        }
        Client client = clientRepository.findByIdAndCompanyId(request.getClientId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Opportunity opportunity = buildFromRequest(request, client, client.getCompany(), companyId);
        opportunity = opportunityRepository.save(opportunity);

        crmActivityService.logSystemActivity(CrmActivityType.NOTE,
                "Opportunity created",
                "Opportunity \"" + opportunity.getName() + "\" created in stage " + opportunity.getStage(),
                client.getId(), opportunity.getId());

        return OpportunityMapper.toResponse(opportunity);
    }

    // An Opportunity can now be created directly from a Lead with no Client yet - the Client is
    // created/linked later, when the Opportunity reaches Won (see changeStage). If the Lead was
    // already linked to a Client some other way (legacy data) or an explicit clientId is
    // passed, that Client is used immediately instead.
    @Override
    public OpportunityResponse createFromLead(Long leadId, OpportunityRequest request) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_CREATE);
        Long companyId = requireCompanyId();
        Lead lead = leadRepository.findByIdAndCompanyId(leadId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        Client client = null;
        if (lead.getConvertedClient() != null) {
            client = lead.getConvertedClient();
        } else if (request.getClientId() != null) {
            client = clientRepository.findByIdAndCompanyId(request.getClientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        }
        if (client != null) {
            request.setClientId(client.getId());
        }

        Opportunity opportunity = buildFromRequest(request, client, lead.getCompany(), companyId);
        opportunity.setSourceLead(lead);
        if (request.getSource() == null && lead.getSource() != null) {
            opportunity.setSource(lead.getSource());
        }
        if (request.getAmount() == null && lead.getEstimatedValue() != null) {
            opportunity.setAmount(lead.getEstimatedValue());
        }
        if (request.getExpectedCloseDate() == null && lead.getExpectedCloseDate() != null) {
            opportunity.setExpectedCloseDate(lead.getExpectedCloseDate());
        }
        opportunity = opportunityRepository.save(opportunity);

        crmActivityService.logSystemActivity(CrmActivityType.NOTE,
                "Opportunity created from lead",
                "Opportunity \"" + opportunity.getName() + "\" created from lead \"" + lead.getContactName() + "\"",
                client != null ? client.getId() : null, opportunity.getId());

        return OpportunityMapper.toResponse(opportunity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OpportunityResponse> listAll(OpportunityStage stage, Long clientId, Long ownerId, Long tagId, String keyword,
            Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_VIEW);
        Long companyId = requireCompanyId();
        Page<Opportunity> page;
        if (keyword != null && !keyword.isBlank()) {
            page = opportunityRepository.searchOpportunities(companyId, escapeLikeKeyword(keyword.trim()), pageable);
        } else if (stage != null) {
            page = opportunityRepository.findByCompanyIdAndStage(companyId, stage, pageable);
        } else if (clientId != null) {
            page = opportunityRepository.findByCompanyIdAndClientId(companyId, clientId, pageable);
        } else if (ownerId != null) {
            page = opportunityRepository.findByCompanyIdAndOwnerId(companyId, ownerId, pageable);
        } else if (tagId != null) {
            page = opportunityRepository.findByCompanyIdAndTagsId(companyId, tagId, pageable);
        } else {
            page = opportunityRepository.findByCompanyId(companyId, pageable);
        }
        return page.map(OpportunityMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityResponse getById(Long id) {
        return OpportunityMapper.toResponse(findOwned(id));
    }

    @Override
    public OpportunityResponse update(Long id, OpportunityRequest request) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_UPDATE);
        Long companyId = requireCompanyId();
        Opportunity opportunity = findOwned(id);

        if (opportunity.getStage().isClosed()) {
            throw new BadRequestException(
                    "Closed opportunities cannot be edited. Reopen it by changing the stage first");
        }

        if (request.getName() != null)
            opportunity.setName(request.getName());
        opportunity.setDescription(request.getDescription());
        opportunity.setNextStep(request.getNextStep());
        if (request.getAmount() != null)
            opportunity.setAmount(request.getAmount());
        if (request.getProbability() != null)
            opportunity.setProbability(request.getProbability());
        if (request.getExpectedCloseDate() != null)
            opportunity.setExpectedCloseDate(request.getExpectedCloseDate());
        if (request.getSource() != null)
            opportunity.setSource(request.getSource());

        if (request.getContactId() != null) {
            ClientContact contact = clientContactRepository.findByIdAndCompanyId(request.getContactId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
            if (opportunity.getClient() != null
                    && !contact.getClient().getId().equals(opportunity.getClient().getId())) {
                throw new BadRequestException("Contact does not belong to the opportunity's client");
            }
            opportunity.setContact(contact);
        }
        if (request.getOwnerId() != null) {
            opportunity.setOwner(findEmployee(request.getOwnerId(), companyId));
        }
        if (request.getTagIds() != null) {
            opportunity.setTags(request.getTagIds().isEmpty()
                    ? new java.util.ArrayList<>()
                    : tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }

        return OpportunityMapper.toResponse(opportunityRepository.save(opportunity));
    }

    @Override
    public OpportunityResponse changeStage(Long id, ChangeStageRequest request) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_UPDATE);
        Opportunity opportunity = findOwned(id);
        OpportunityStage from = opportunity.getStage();
        OpportunityStage to = request.getStage();

        if (from == to) {
            return OpportunityMapper.toResponse(opportunity);
        }
        if (to == OpportunityStage.LOST) {
            // The picklist code is the requirement - it is what win/loss
            // analysis aggregates. Free text is detail, mandatory only for
            // OTHER so that bucket still explains itself.
            if (request.getLostReasonCode() == null) {
                throw new BadRequestException("A lost reason is required when closing an opportunity as lost");
            }
            if (request.getLostReasonCode() == com.businessos.enums.LostReason.OTHER
                    && (request.getLostReason() == null || request.getLostReason().isBlank())) {
                throw new BadRequestException("Please describe the reason when choosing Other");
            }
        }

        opportunity.setStage(to);
        opportunity.setProbability(to.getDefaultProbability());
        opportunity.setStageChangedAt(LocalDateTime.now());

        DuplicateMatch autoLinkedDuplicate = null;
        if (to.isClosed()) {
            opportunity.setActualCloseDate(LocalDate.now());
            if (to == OpportunityStage.LOST) {
                opportunity.setLostReason(request.getLostReason());
                opportunity.setLostReasonCode(request.getLostReasonCode());
                notifyOwnerOpportunityLost(opportunity);
            } else {
                opportunity.setLostReason(null);
                opportunity.setLostReasonCode(null);
                if (opportunity.getClient() == null) {
                    autoLinkedDuplicate = resolveClientForWonOpportunity(opportunity, request);
                }
                creditClientLifetimeValue(opportunity);
                automationEventPublisher.publishOpportunityWon(
                        this, opportunity.getCompany().getId(),
                        opportunity.getId(), opportunity.getClient().getId(),
                        opportunity.getName());
                notifyOwnerOpportunityWon(opportunity);
            }
        } else {
            opportunity.setActualCloseDate(null);
            opportunity.setLostReason(null);
            opportunity.setLostReasonCode(null);
            // Reopening a previously-won deal must reverse the credit applied when it
            // closed, or a later reopen+reclose double-counts the client's lifetime value.
            if (from == OpportunityStage.WON && opportunity.getClient() != null) {
                debitClientLifetimeValue(opportunity);
            }
        }

        opportunity = opportunityRepository.save(opportunity);

        crmActivityService.logSystemActivity(CrmActivityType.STAGE_CHANGE,
                "Stage changed",
                "Stage moved from " + from + " to " + to,
                opportunity.getClient() != null ? opportunity.getClient().getId() : null,
                opportunity.getId());

        OpportunityResponse response = OpportunityMapper.toResponse(opportunity);
        response.setPossibleDuplicate(autoLinkedDuplicate);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineSummaryResponse getPipelineSummary() {
        Long companyId = requireCompanyId();
        List<OpportunityRepository.PipelineStageSummary> rows = opportunityRepository.summarizePipeline(companyId);

        PipelineSummaryResponse response = new PipelineSummaryResponse();
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal won = BigDecimal.ZERO;
        long openDeals = 0;

        EnumSet<OpportunityStage> closed = EnumSet.of(OpportunityStage.WON, OpportunityStage.LOST);

        response.setStages(rows.stream().map(row -> {
            PipelineSummaryResponse.StageSummary s = new PipelineSummaryResponse.StageSummary();
            s.setStage(row.getStage());
            s.setDealCount(row.getDealCount());
            s.setTotalAmount(row.getTotalAmount());
            s.setWeightedAmount(row.getWeightedAmount());
            return s;
        }).toList());

        for (OpportunityRepository.PipelineStageSummary row : rows) {
            if (row.getStage() == OpportunityStage.WON) {
                won = won.add(row.getTotalAmount());
            } else if (!closed.contains(row.getStage())) {
                open = open.add(row.getTotalAmount());
                weighted = weighted.add(row.getWeightedAmount());
                openDeals += row.getDealCount();
            }
        }
        response.setOpenPipelineValue(open);
        response.setWeightedForecast(weighted);
        response.setWonValue(won);
        response.setTotalOpenDeals(openDeals);
        return response;
    }

    @Override
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.OPPORTUNITY_DELETE);
        Opportunity opportunity = findOwned(id);
        opportunity.softDelete();
        opportunityRepository.save(opportunity);
    }

    // `client` may be null (Opportunity created straight from a Lead, pre-Client) - `company` is
    // passed separately since it can't be derived from a null client.
    private Opportunity buildFromRequest(OpportunityRequest request, Client client, Company company, Long companyId) {
        OpportunityStage stage = request.getStage() != null ? request.getStage() : OpportunityStage.QUALIFICATION;

        Opportunity opportunity = Opportunity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .stage(stage)
                .source(request.getSource() != null ? request.getSource() : LeadSource.OTHER)
                .amount(request.getAmount())
                .probability(
                        request.getProbability() != null ? request.getProbability() : stage.getDefaultProbability())
                .expectedCloseDate(request.getExpectedCloseDate())
                .nextStep(request.getNextStep())
                .client(client)
                .company(company)
                .build();

        if (request.getContactId() != null && client != null) {
            ClientContact contact = clientContactRepository.findByIdAndCompanyId(request.getContactId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
            if (!contact.getClient().getId().equals(client.getId())) {
                throw new BadRequestException("Contact does not belong to the selected client");
            }
            opportunity.setContact(contact);
        }
        if (request.getOwnerId() != null) {
            opportunity.setOwner(findEmployee(request.getOwnerId(), companyId));
        }
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            opportunity.setTags(tagRepository.findByIdInAndCompanyId(request.getTagIds(), companyId));
        }
        return opportunity;
    }

    private void creditClientLifetimeValue(Opportunity opportunity) {
        if (opportunity.getAmount() == null)
            return;
        Client client = opportunity.getClient();
        BigDecimal current = client.getLifetimeValue() != null ? client.getLifetimeValue() : BigDecimal.ZERO;
        client.setLifetimeValue(current.add(opportunity.getAmount()));
        clientRepository.save(client);
    }

    private void notifyOwnerOpportunityLost(Opportunity opportunity) {
        Employee owner = opportunity.getOwner();
        if (owner == null || owner.getUser() == null) return;
        notificationService.send(CreateNotificationRequest.of(
                NotificationType.OPPORTUNITY_LOST,
                "Opportunity Lost",
                "Opportunity \"" + opportunity.getName() + "\" was marked as lost.",
                "/crm/pipeline",
                owner.getUser().getId(),
                opportunity.getCompany().getId()
        ));
    }

    private void notifyOwnerOpportunityWon(Opportunity opportunity) {
        Employee owner = opportunity.getOwner();
        if (owner == null || owner.getUser() == null) return;
        notificationService.send(CreateNotificationRequest.of(
                NotificationType.OPPORTUNITY_WON,
                "Opportunity Won",
                "Opportunity \"" + opportunity.getName() + "\" was won.",
                "/crm/pipeline",
                owner.getUser().getId(),
                opportunity.getCompany().getId()
        ));
    }

    // Resolves the Client for an Opportunity that reaches Won with no Client yet (created
    // straight from a Lead). Explicit link/force choices from the frontend's duplicate-detection
    // modal take priority; otherwise runs duplicate detection and links a match, or creates a new
    // Client (without a portal login - see CreateClientRequest.provisionPortalLogin). Returns
    // the match that was auto-linked (if any), purely informational for the response.
    private DuplicateMatch resolveClientForWonOpportunity(Opportunity opportunity, ChangeStageRequest request) {
        Long companyId = opportunity.getCompany().getId();
        // Recorded on whichever Client the opportunity ends up with, regardless of
        // which of the three paths below produced it - previously only written by
        // nothing at all, so a converted Lead could never be traced to the Client
        // it became.
        Lead sourceLead = opportunity.getSourceLead();

        if (request.getLinkToExistingClientId() != null) {
            Client existing = clientRepository.findByIdAndCompanyId(request.getLinkToExistingClientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            opportunity.setClient(existing);
            linkLeadToConvertedClient(sourceLead, existing);
            return null;
        }

        if (!request.isForceCreateNewClient()) {
            Optional<DuplicateMatch> duplicate = findDuplicateForOpportunity(opportunity);
            if (duplicate.isPresent()) {
                Client existing = clientRepository.findByIdAndCompanyId(duplicate.get().getClientId(), companyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
                opportunity.setClient(existing);
                linkLeadToConvertedClient(sourceLead, existing);
                return duplicate.get();
            }
        }

        String companyName = sourceLead != null ? sourceLead.getCompanyName() : null;
        String fallbackName = companyName != null ? companyName
                : sourceLead != null ? sourceLead.getContactName()
                : opportunity.getName();
        Client client = Client.builder()
                .clientCompanyName(fallbackName)
                .company(companyRef(companyId))
                .status(ClientStatus.ACTIVE)
                .industry(sourceLead != null ? sourceLead.getIndustry() : null)
                .onboardedAt(LocalDate.now())
                .build();
        clientRepository.save(client);
        opportunity.setClient(client);
        linkLeadToConvertedClient(sourceLead, client);

        // Carry the lead's contact details across as the client's primary contact.
        //
        // This is not just convenience. findPossibleDuplicateClient() matches email
        // and phone against ClientContact rows - so a client created without one can
        // only ever be matched by company name. The same company's second deal would
        // then duplicate whenever the name was typed slightly differently
        // ("Bengal Textiles" vs "Bengal Textiles Ltd."), splitting its invoices and
        // service history across two records.
        createPrimaryContactFromLead(client, sourceLead, companyId);
        return null;
    }

    private void linkLeadToConvertedClient(Lead sourceLead, Client client) {
        if (sourceLead == null) return;
        sourceLead.setConvertedClient(client);
        leadRepository.save(sourceLead);
    }

    /**
     * Creates the client's first contact from the lead. No-op when the lead carries
     * neither an email nor a phone, since a contact with only a name adds nothing
     * that duplicate detection can use.
     */
    private void createPrimaryContactFromLead(Client client, Lead lead, Long companyId) {
        if (lead == null) return;

        boolean hasEmail = lead.getEmail() != null && !lead.getEmail().isBlank();
        boolean hasPhone = lead.getPhone() != null && !lead.getPhone().isBlank();
        if (!hasEmail && !hasPhone) return;

        // fullName is NOT NULL on ClientContact, so fall back through the same chain
        // used for the client's own name rather than risking a constraint violation.
        String contactName = lead.getContactName() != null && !lead.getContactName().isBlank()
                ? lead.getContactName()
                : client.getClientCompanyName();

        ClientContact contact = ClientContact.builder()
                .client(client)
                .company(companyRef(companyId))
                .fullName(contactName)
                .email(hasEmail ? lead.getEmail().trim() : null)
                .phone(hasPhone ? lead.getPhone().trim() : null)
                .jobTitle(lead.getJobTitle())
                .primaryContact(true)
                .notes("Created automatically when deal was won.")
                .build();

        clientContactRepository.save(contact);
    }

    private Optional<DuplicateMatch> findDuplicateForOpportunity(Opportunity opportunity) {
        Lead sourceLead = opportunity.getSourceLead();
        String companyName = sourceLead != null ? sourceLead.getCompanyName() : null;
        String email = sourceLead != null ? sourceLead.getEmail() : null;
        String phone = sourceLead != null ? sourceLead.getPhone() : null;
        return duplicateDetectionService.findPossibleDuplicateClient(companyName, email, phone);
    }

    // Read-only pre-check the frontend calls before committing a WON stage change on a
    // client-less Opportunity, so it can show a confirm modal (link existing / create new)
    // BEFORE the transition happens, rather than only finding out after the fact.
    @Override
    @Transactional(readOnly = true)
    public DuplicateMatch previewWonDuplicate(Long id) {
        Opportunity opportunity = findOwned(id);
        if (opportunity.getClient() != null) {
            return null;
        }
        return findDuplicateForOpportunity(opportunity).orElse(null);
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }

    /** Reverses creditClientLifetimeValue() when a won opportunity is reopened. */
    private void debitClientLifetimeValue(Opportunity opportunity) {
        if (opportunity.getAmount() == null)
            return;
        Client client = opportunity.getClient();
        BigDecimal current = client.getLifetimeValue() != null ? client.getLifetimeValue() : BigDecimal.ZERO;
        client.setLifetimeValue(current.subtract(opportunity.getAmount()).max(BigDecimal.ZERO));
        clientRepository.save(client);
    }

    private Employee findEmployee(Long employeeId, Long companyId) {
        return employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany() != null && e.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private Opportunity findOwned(Long id) {
        return opportunityRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }

    private Long requireCompanyId() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for current platformuser");
        }
        return companyId;
    }

    // '!' is the LIKE escape character used by OpportunityRepository's ESCAPE '!'
    // queries. Mirrors GlobalSearchServiceImpl.escapeLikeKeyword.
    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) return null;
        return keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
