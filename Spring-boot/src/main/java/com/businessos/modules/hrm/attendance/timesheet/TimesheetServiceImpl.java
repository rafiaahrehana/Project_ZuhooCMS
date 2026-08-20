package com.businessos.modules.hrm.attendance.timesheet;

import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.task.Task;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.servicedesk.task.TaskRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.TimesheetEntryPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class TimesheetServiceImpl implements TimesheetService {

    private static final ObjectMapper COMPOSE_MAPPER = new ObjectMapper();

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository  employeeRepository;
    private final TaskRepository      taskRepository;
    private final SecurityUtil        securityUtil;
    private final AuthorizationService authorizationService;
    private final AiService           aiService;

    @Override
    @Transactional
    public TimesheetResponse log(TimesheetRequest request) {
        Long companyId = requireCompanyId();
        Employee employee = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("Employee profile not found"));

        if (timesheetRepository.findByEmployeeIdAndWorkDate(employee.getId(), request.getWorkDate()).isPresent()) {
            throw new BadRequestException("Timesheet already logged for " + request.getWorkDate());
        }

        Timesheet ts = new Timesheet(); ts.setEmployee(employee); ts.setCompany(companyRef(companyId)); ts.setWorkDate(request.getWorkDate()); ts.setStartTime(request.getStartTime() != null ? request.getStartTime().toLocalTime() : null); ts.setEndTime(request.getEndTime() != null ? request.getEndTime().toLocalTime() : null); ts.setHoursWorked(request.getHoursWorked()); ts.setBillableHours(request.getBillableHours() != null ? request.getBillableHours() : 0.0); ts.setWorkSummary(request.getDescription()); ts.setProjectName(request.getProjectName()); ts.setTaskDescription(request.getTaskDescription());

        if (request.getTaskId() != null) {
            Task task = taskRepository.findByIdAndCompanyId(request.getTaskId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.getTaskId()));
            // ts.setTask(task);
        }

        timesheetRepository.save(ts);
        return TimesheetMapper.toTimesheetResponse(ts);
    }

    @Override
    @Transactional(readOnly = true)
    public TimesheetResponse getById(Long id) {
        return TimesheetMapper.toTimesheetResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetResponse> listMine(Pageable pageable) {
        Long companyId = requireCompanyId();
        Employee emp = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        return timesheetRepository.findByCompanyIdAndEmployeeId(companyId, emp.getId(), pageable)
            .map(TimesheetMapper::toTimesheetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimesheetResponse> listForEmployee(Long employeeId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.TIMESHEET_VIEW);
        return timesheetRepository.findByCompanyIdAndEmployeeId(requireCompanyId(), employeeId, pageable)
            .map(TimesheetMapper::toTimesheetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimesheetResponse> listByDateRange(Long employeeId, LocalDate from, LocalDate to) {
        return timesheetRepository.findByCompanyIdAndEmployeeIdAndWorkDateBetween(
                requireCompanyId(), employeeId, from, to)
            .stream().map(TimesheetMapper::toTimesheetResponse).toList();
    }

    @Override
    @Transactional
    public TimesheetResponse update(Long id, TimesheetRequest request) {
        Long companyId = requireCompanyId();
        Timesheet ts = findInTenant(id);
        if (ts.isApproved()) {
            throw new BadRequestException("Cannot edit an approved timesheet");
        }
        if (ts.isSubmitted()) {
            throw new BadRequestException("Cannot edit a timesheet that has been submitted for review");
        }
        if (request.getStartTime()    != null) ts.setStartTime(request.getStartTime().toLocalTime());
        if (request.getEndTime()      != null) ts.setEndTime(request.getEndTime().toLocalTime());
        if (request.getHoursWorked()  != null) ts.setHoursWorked(request.getHoursWorked());
        if (request.getBillableHours()!= null) ts.setBillableHours(request.getBillableHours());
        if (request.getDescription()  != null) ts.setWorkSummary(request.getDescription());
        if (request.getProjectName()  != null) ts.setProjectName(request.getProjectName());
        if (request.getTaskDescription() != null) ts.setTaskDescription(request.getTaskDescription());
        if (request.getTaskId() != null) {
            // Task relation removed from timesheet
        }
        return TimesheetMapper.toTimesheetResponse(ts);
    }

    @Override
    @Transactional
    public int submitForReview() {
        Long companyId = requireCompanyId();
        Employee employee = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        List<Timesheet> draft = timesheetRepository.findByCompanyIdAndEmployeeIdAndSubmittedFalseAndApprovedFalse(
            companyId, employee.getId());
        LocalDateTime now = LocalDateTime.now();
        draft.forEach(ts -> { ts.setSubmitted(true); ts.setSubmittedAt(now); });
        return draft.size();
    }

    @Override
    @Transactional
    public TimesheetResponse approve(Long id) {
        authorizationService.checkPermission(PermissionCode.TIMESHEET_APPROVE);
        Timesheet ts = findInTenant(id);
        if (!ts.isSubmitted()) {
            throw new BadRequestException("This timesheet has not been submitted for review yet");
        }
        Employee approver = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        ts.setApproved(true);
        ts.setApprovedBy(approver.getUser());
        // ts.setApprovedAt(LocalDateTime.now());
        return TimesheetMapper.toTimesheetResponse(ts);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Timesheet ts = findInTenant(id);
        if (ts.isApproved()) throw new BadRequestException("Cannot delete an approved timesheet");
        if (ts.isSubmitted()) throw new BadRequestException("Cannot delete a timesheet that has been submitted for review");
        ts.softDelete();
    }

    @Override
    public TimesheetComposeResponse composeEntry(TimesheetComposeRequest request) {
        String prompt = TimesheetEntryPromptBuilder.builder()
            .setProjectName(request.getProjectName())
            .setRoughNotes(request.getRoughNotes())
            .build();

        String raw = aiService.generateRaw(AiFeature.TIMESHEET_ENTRY, prompt);
        return parseCompose(raw, request.getRoughNotes());
    }

    private TimesheetComposeResponse parseCompose(String raw, String fallbackNotes) {
        TimesheetComposeResponse response = new TimesheetComposeResponse();
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
            }
            JsonNode node = COMPOSE_MAPPER.readTree(cleaned);
            response.setTaskDescription(node.path("taskDescription").asText(null));
            response.setDescription(node.path("description").asText(null));
        } catch (Exception ignored) {
            // Model didn't return valid JSON despite instructions - fall back to the
            // raw text as the description rather than failing the whole request.
        }
        if (response.getTaskDescription() == null || response.getTaskDescription().isBlank()) {
            response.setTaskDescription(fallbackNotes.length() > 80
                ? fallbackNotes.substring(0, 77) + "..." : fallbackNotes);
        }
        if (response.getDescription() == null || response.getDescription().isBlank()) {
            response.setDescription(raw);
        }
        return response;
    }

    private Timesheet findInTenant(Long id) {
        return timesheetRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found: " + id));
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

