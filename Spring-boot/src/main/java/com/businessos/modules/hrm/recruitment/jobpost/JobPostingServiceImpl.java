package com.businessos.modules.hrm.recruitment.jobpost;

import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import com.businessos.enums.JobPostingStatus;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.department.DepartmentRepository;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final EmployeeRepository   employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SecurityUtil         securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public JobPostingResponse create(JobPostingRequest request) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_CREATE);
        Long companyId = requireCompanyId();
        User currentUser = securityUtil.getCurrentUser();

        Employee creator = employeeRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new BadRequestException(
                "Only employees can create job postings"));

        JobPosting posting = JobPosting.builder()
            .title(request.getTitle())
            .jobTitle(request.getJobTitle())
            .description(request.getDescription())
            .requirements(request.getRequirements())
            .responsibilities(request.getResponsibilities())
            .location(request.getLocation())
            .employmentType(request.getEmploymentType())
            .status(request.getStatus() != null ? request.getStatus() : JobPostingStatus.DRAFT)
            .vacancies(request.getVacancies() != null ? request.getVacancies() : 1)
            .salaryMin(request.getSalaryMin())
            .salaryMax(request.getSalaryMax())
            .deadline(request.getDeadline())
            .remote(request.isRemote())
            .requiredSkills(request.getRequiredSkills())
            .preferredSkills(request.getPreferredSkills())
            .minExperienceYears(request.getMinExperienceYears())
            .minEducationLevel(request.getMinEducationLevel())
            .company(companyRef(companyId))
            .createdBy(creator)
            .build();

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository
                .findByIdAndCompanyId(request.getDepartmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Department not found: " + request.getDepartmentId()));
            posting.setDepartment(dept);
        }

        jobPostingRepository.save(posting);
        
        return JobPostingMapper.toResponse(posting);
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingResponse getById(Long id) {
        return JobPostingMapper.toResponse(findInTenant(id));
    }



    @Override
    @Transactional(readOnly = true)
    public Page<JobPostingResponse> listAll(JobPostingStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_VIEW);
        Long companyId = requireCompanyId();
        Page<JobPosting> page = status != null
            ? jobPostingRepository.findByCompanyIdAndStatus(companyId, status, pageable)
            : jobPostingRepository.findByCompanyId(companyId, pageable);
        return page.map(JobPostingMapper::toResponse);
    }

    // Deliberately NOT gated by JOB_POSTING_VIEW: this is the open-posting picker
    // consumed by the Applications page - users with APPLICATION_VIEW but not
    // JOB_POSTING_VIEW still need it to populate that dropdown.
    @Override
    @Transactional(readOnly = true)
    public List<JobPostingResponse> listOpen() {
        return jobPostingRepository.findByCompanyIdAndStatus(requireCompanyId(), JobPostingStatus.OPEN)
            .stream().map(JobPostingMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public JobPostingResponse update(Long id, JobPostingRequest request) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_UPDATE);
        Long companyId = requireCompanyId();
        JobPosting posting = findInTenant(id);

        if (posting.getStatus() == JobPostingStatus.CLOSED) {
            throw new BadRequestException("Cannot edit a closed job posting");
        }

        if (request.getTitle()        != null) posting.setTitle(request.getTitle());
        if (request.getJobTitle()     != null) posting.setJobTitle(request.getJobTitle());
        if (request.getDescription()  != null) posting.setDescription(request.getDescription());
        if (request.getRequirements() != null) posting.setRequirements(request.getRequirements());
        if (request.getResponsibilities() != null) posting.setResponsibilities(request.getResponsibilities());
        if (request.getLocation()     != null) posting.setLocation(request.getLocation());
        if (request.getEmploymentType()!= null) posting.setEmploymentType(request.getEmploymentType());
        if (request.getVacancies()    != null) posting.setVacancies(request.getVacancies());
        if (request.getSalaryMin()    != null) posting.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax()    != null) posting.setSalaryMax(request.getSalaryMax());
        if (request.getDeadline()     != null) posting.setDeadline(request.getDeadline());
        posting.setRemote(request.isRemote());
        if (request.getRequiredSkills()   != null) posting.setRequiredSkills(request.getRequiredSkills());
        if (request.getPreferredSkills()  != null) posting.setPreferredSkills(request.getPreferredSkills());
        if (request.getMinExperienceYears()!= null) posting.setMinExperienceYears(request.getMinExperienceYears());
        if (request.getMinEducationLevel()!= null) posting.setMinEducationLevel(request.getMinEducationLevel());

        if (request.getDepartmentId() != null) {
            posting.setDepartment(departmentRepository
                .findByIdAndCompanyId(request.getDepartmentId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Department not found: " + request.getDepartmentId())));
        }

        return JobPostingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public JobPostingResponse publish(Long id) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_UPDATE);
        JobPosting posting = findInTenant(id);
        if (posting.getStatus() == JobPostingStatus.CLOSED) {
            throw new BadRequestException("Cannot publish a closed job posting");
        }
        posting.setStatus(JobPostingStatus.OPEN);

        return JobPostingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public JobPostingResponse close(Long id) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_UPDATE);
        JobPosting posting = findInTenant(id);
        posting.setStatus(JobPostingStatus.CLOSED);

        return JobPostingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public JobPostingResponse assignRecruiter(Long id, Long recruiterId) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_UPDATE);
        Long companyId = requireCompanyId();
        JobPosting posting = findInTenant(id);
        if (recruiterId == null) {
            posting.setAssignedRecruiter(null);
        } else {
            Employee recruiter = employeeRepository.findByIdAndCompanyId(recruiterId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + recruiterId));
            posting.setAssignedRecruiter(recruiter);
        }
        return JobPostingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.JOB_POSTING_DELETE);
        JobPosting posting = findInTenant(id);
        posting.softDelete();

    }

    // ── Private helpers ───────────────────────────────────────────

    private JobPosting findInTenant(Long id) {
        return jobPostingRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Job posting not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
