package com.businessos.modules.support.contextswitch;

import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.enums.AuditAction;
import com.businessos.enums.AuditEntityType;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.audit.AuditService;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportContextSwitchServiceImpl implements SupportContextSwitchService {

    private final SupportContextSwitchRepository contextSwitchRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;

    @Override
    @Transactional
    public SupportContextSwitchResponse switchContext(SupportContextSwitchRequest request, String ipAddress, String userAgent) {
        // The actor and IP/user-agent used to come straight from the client's
        // JSON body - a support agent (or a compromised client) could claim to
        // be anyone and log any IP for this highly sensitive "view a client's
        // company as staff" action, corrupting the one record meant to hold
        // them accountable for it. Both are now derived server-side.
        User user = securityUtil.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException("Current user not found");
        }

        Company company = companyRepository.findById(request.getViewedCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // End any active switches
        Optional<SupportContextSwitch> activeSwitchOpt = contextSwitchRepository
                .findBySupportAgentIdAndStillActiveTrue(user.getId());

        activeSwitchOpt.ifPresent(activeSwitch -> {
            activeSwitch.setSwitchedOutTime(LocalDateTime.now());
            activeSwitch.setStillActive(false);
            contextSwitchRepository.save(activeSwitch);
        });

        // Create new context switch
        SupportContextSwitch contextSwitch = SupportContextSwitch.builder()
                .supportAgent(user)
                .viewedCompany(company)
                .switchedInTime(LocalDateTime.now())
                .purpose(request.getPurpose())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .stillActive(true)
                .build();

        contextSwitch = contextSwitchRepository.save(contextSwitch);

        // Context switches had their own dedicated table (with IP) but never
        // appeared in the shared audit log the Support Audit Logs page reads
        // from, so this sensitive action was invisible from the one screen
        // built to review sensitive actions.
        auditService.log(AuditEntityType.COMPANY, company.getId(), AuditAction.ASSIGN,
                null, "Support context switch by " + user.getEmail(), user, company.getId(), ipAddress);

        return SupportContextSwitchMapper.toResponse(contextSwitch);
    }

    @Override
    @Transactional
    public void endContextSwitch(Long contextSwitchId) {
        SupportContextSwitch contextSwitch = contextSwitchRepository.findById(contextSwitchId)
                .orElseThrow(() -> new ResourceNotFoundException("Context switch not found"));

        contextSwitch.setSwitchedOutTime(LocalDateTime.now());
        contextSwitch.setStillActive(false);
        contextSwitchRepository.save(contextSwitch);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportContextSwitchResponse getActiveContextSwitch(Long supportAgentId) {
        SupportContextSwitch contextSwitch = contextSwitchRepository
                .findBySupportAgentIdAndStillActiveTrue(supportAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("No active context switch found"));

        return SupportContextSwitchMapper.toResponse(contextSwitch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportContextSwitchResponse> getContextSwitchHistory(Long supportAgentId, Pageable pageable) {
        return contextSwitchRepository.findBySupportAgentId(supportAgentId, pageable)
                .map(SupportContextSwitchMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportContextSwitchResponse> getActiveContextSwitches() {
        return contextSwitchRepository.findByStillActiveTrue()
                .stream()
                .map(SupportContextSwitchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SupportContextSwitchResponse getById(Long id) {
        SupportContextSwitch contextSwitch = contextSwitchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Context switch not found"));
        return SupportContextSwitchMapper.toResponse(contextSwitch);
    }
}

