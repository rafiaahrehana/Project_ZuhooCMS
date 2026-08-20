package com.businessos.modules.hrm.leave.holiday;

import com.businessos.enums.HolidayType;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.HolidayDraftPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.businessos.modules.ai.support.AiTransactionBoundary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor

public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final CompanyRepository    companyRepository;
    private final AiService            aiService;
    private final AiTransactionBoundary aiTx;
    private final ObjectMapper         objectMapper;
    private final SecurityUtil         securityUtil;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.hrm.employee.EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_CREATE);
        Long companyId = requireCompanyId();
        if (holidayRepository.existsByCompanyIdAndDate(companyId, request.getHolidayDate())) {
            throw new BadRequestException("A holiday already exists on " + request.getHolidayDate());
        }

        Holiday holiday = new Holiday(); holiday.setName(request.getName()); holiday.setDate(request.getHolidayDate()); holiday.setType(request.getHolidayType()); holiday.setDescription(request.getDescription()); holiday.setCompany(companyRef(companyId));

        holidayRepository.save(holiday);
        notifyCompanyOfNewHoliday(holiday, companyId);
        return HolidayMapper.toHolidayResponse(holiday);
    }

    // Holidays are always company-wide (see the removed department-targeting
    // control) but publishing one told nobody - only AnnouncementServiceImpl
    // called NotificationService anywhere in this slice.
    private void notifyCompanyOfNewHoliday(Holiday holiday, Long companyId) {
        int pageNum = 0;
        final int PAGE_SIZE = 100;
        Page<com.businessos.modules.hrm.employee.Employee> page;
        do {
            page = employeeRepository.findByCompanyId(companyId, PageRequest.of(pageNum, PAGE_SIZE));
            page.getContent().forEach(emp -> {
                if (emp.getUser() != null) {
                    notificationService.send(CreateNotificationRequest.of(
                            com.businessos.enums.NotificationType.HOLIDAY_PUBLISHED,
                            "New holiday added",
                            holiday.getName() + " on " + holiday.getDate() + " has been added to the company calendar.",
                            "/hrm/holidays",
                            emp.getUser().getId(),
                            companyId));
                }
            });
            pageNum++;
        } while (page.hasNext());
    }

    // No @Transactional here on purpose: the company lookup runs inside
    // aiTx.load(), which commits before the provider call so no DB connection is
    // held across it - see AiTransactionBoundary.
    @Override
    public HolidayDraftResponse draftWithAi(HolidayDraftRequest request) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_CREATE);
        Long companyId = requireCompanyId();

        String prompt = aiTx.load(() -> {
            Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));

            return HolidayDraftPromptBuilder.builder()
                .setCompanyName(company.getCompanyName())
                .setToday(LocalDate.now())
                .setInstructions(request.getInstructions())
                .build();
        });

        String raw = aiService.generateRaw(AiFeature.HOLIDAY_DRAFT, prompt);
        return parseDraft(raw, request.getInstructions());
    }

    private HolidayDraftResponse parseDraft(String raw, String fallbackInstructions) {
        HolidayDraftResponse response = new HolidayDraftResponse();
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
            }
            JsonNode node = objectMapper.readTree(cleaned);
            response.setName(node.path("name").asText(null));
            response.setDate(node.path("date").asText(null));
            response.setType(node.path("type").asText(null));
            response.setDescription(node.path("description").asText(null));
        } catch (Exception ignored) {
            // Model didn't return valid JSON despite instructions - fall back to a
            // best-effort name rather than failing the whole request.
        }
        if (response.getName() == null || response.getName().isBlank()) {
            response.setName(fallbackInstructions.length() > 150
                ? fallbackInstructions.substring(0, 147) + "..." : fallbackInstructions);
        }
        if (response.getType() == null || !isValidHolidayType(response.getType())) {
            response.setType(HolidayType.COMPANY.name());
        }
        return response;
    }

    private boolean isValidHolidayType(String type) {
        try {
            HolidayType.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HolidayResponse getById(Long id) {
        return HolidayMapper.toHolidayResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HolidayResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_VIEW);
        return holidayRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(HolidayMapper::toHolidayResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> listByYear(int year) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_VIEW);
        Long companyId = requireCompanyId();
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to   = LocalDate.of(year, 12, 31);
        return holidayRepository.findByCompanyAndDateRange(companyId, from, to)
            .stream().map(HolidayMapper::toHolidayResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> listByRange(LocalDate from, LocalDate to) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_VIEW);
        Long companyId = requireCompanyId();
        List<Holiday> holidays = holidayRepository.findByCompanyAndDateRange(companyId, from, to);
        return holidays.stream().map(HolidayMapper::toHolidayResponse).toList();
    }

    @Override
    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_UPDATE);
        Holiday holiday = findInTenant(id);
        holiday.setName(request.getName());
        holiday.setDate(request.getHolidayDate());
        holiday.setType(request.getHolidayType());
        if (request.getDescription() != null) holiday.setDescription(request.getDescription());
        return HolidayMapper.toHolidayResponse(holiday);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.HOLIDAY_DELETE);
        findInTenant(id).softDelete();
    }

    private Holiday findInTenant(Long id) {
        return holidayRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
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

