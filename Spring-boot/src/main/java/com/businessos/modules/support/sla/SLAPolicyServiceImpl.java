package com.businessos.modules.support.sla;

import com.businessos.modules.support.ticket.TicketPriority;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SLAPolicyServiceImpl implements SLAPolicyService {

    private final SLAPolicyRepository slaPolicyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public SLAPolicyResponse create(SLAPolicyRequest request) {
        if (slaPolicyRepository.findByApplicablePriorityAndDeletedFalse(request.getApplicablePriority()).isPresent()) {
            throw new BadRequestException("SLA policy already exists for priority: " + request.getApplicablePriority());
        }

        SLAPolicy policy = SLAPolicyMapper.toEntity(request);
        policy = slaPolicyRepository.save(policy);
        return SLAPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public SLAPolicyResponse getById(Long id) {
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found"));
        return SLAPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public SLAPolicyResponse getByPriority(TicketPriority priority) {
        SLAPolicy policy = slaPolicyRepository.findByApplicablePriority(priority)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found for priority: " + priority));
        return SLAPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SLAPolicyResponse> getAll(Pageable pageable) {
        checkTenantPermission();
        return slaPolicyRepository.findAll(pageable)
                .map(SLAPolicyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SLAPolicyResponse> getActive() {
        checkTenantPermission();
        return slaPolicyRepository.findByActiveTrue()
                .stream()
                .map(SLAPolicyMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SLAPolicyResponse update(Long id, SLAPolicyRequest request) {
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found"));

        policy.setPolicyName(request.getPolicyName());
        policy.setApplicablePriority(request.getApplicablePriority());
        policy.setFirstResponseTimeHours(request.getFirstResponseTimeHours());
        policy.setResolutionTimeHours(request.getResolutionTimeHours());
        policy.setBusinessHoursOnly(request.isBusinessHoursOnly());
        policy.setDescription(request.getDescription());

        policy = slaPolicyRepository.save(policy);
        return SLAPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, boolean active) {
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found"));
        policy.setActive(active);
        slaPolicyRepository.save(policy);
    }

    @Override
    @Transactional
    public SLAPolicyResponse delete(Long id) {
        SLAPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found"));
        policy.softDelete();
        slaPolicyRepository.save(policy);
        return SLAPolicyMapper.toResponse(policy);
    }

    // SLA policies are a shared platform-wide catalog - SUPPORT_AGENT/SUPPORT_MANAGER
    // (platform staff with no CustomRole) triage tickets across every company and must
    // not be blocked here; their existing role-based @PreAuthorize already gates access.
    private void checkTenantPermission() {
        User current = securityUtil.getCurrentUser();
        if (current != null && !current.isPlatformUser()) {
            authorizationService.checkPermission(PermissionCode.SLA_POLICY_VIEW);
        }
    }
}
