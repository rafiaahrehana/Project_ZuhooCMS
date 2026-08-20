package com.businessos.modules.support.agent;

import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.modules.support.ticket.SupportTicketRepository;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportAgentServiceImpl implements SupportAgentService {

    private final SupportAgentRepository agentRepository;
    private final UserRepository userRepository;
    private final SupportTicketRepository ticketRepository;

    @Override
    @Transactional
    public SupportAgentResponse create(SupportAgentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (agentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("User is already a support agent");
        }

        SupportAgent agent = SupportAgent.builder()
                .user(user)
                .department(request.getDepartment())
                .specialization(request.getSpecialization())
                .status(request.getStatus() != null ? request.getStatus() : SupportAgentStatus.ACTIVE)
                .maxConcurrentTickets(request.getMaxConcurrentTickets())
                .notes(request.getNotes())
                .build();

        agent = agentRepository.save(agent);
        return SupportAgentMapper.toResponse(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportAgentResponse getById(Long id) {
        SupportAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));
        return SupportAgentMapper.toResponse(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportAgentResponse getByUserId(Long userId) {
        SupportAgent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));
        return SupportAgentMapper.toResponse(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAgentResponse> getAll(Pageable pageable) {
        return agentRepository.findAll(pageable)
                .map(SupportAgentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportAgentResponse> getByStatus(SupportAgentStatus status, Pageable pageable) {
        return agentRepository.findByStatus(status, pageable)
                .map(SupportAgentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportAgentResponse> getAvailableAgents() {
        return agentRepository.findByStatusAndAcceptingTicketsTrue(SupportAgentStatus.ACTIVE)
                .stream()
                .map(SupportAgentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SupportAgentResponse update(Long id, SupportAgentRequest request) {
        SupportAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));

        agent.setDepartment(request.getDepartment());
        agent.setSpecialization(request.getSpecialization());
        agent.setMaxConcurrentTickets(request.getMaxConcurrentTickets());
        agent.setNotes(request.getNotes());

        agent = agentRepository.save(agent);
        return SupportAgentMapper.toResponse(agent);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, SupportAgentStatus status) {
        SupportAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));

        agent.setStatus(status);
        agent.setLastActiveTime(LocalDateTime.now());
        agentRepository.save(agent);
    }

    @Override
    @Transactional
    public void updateAcceptingTickets(Long id, boolean accepting) {
        SupportAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));

        agent.setAcceptingTickets(accepting);
        agentRepository.save(agent);
    }

    @Override
    @Transactional
    public void updateMetrics(Long agentId) {
        SupportAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));

        // Calculate metrics from tickets assigned to this agent
        long totalTickets = ticketRepository.countByAssignedToAgentId(agentId);
        agent.setTotalTicketsHandled((int) totalTickets);

        // Calculate average response and resolution times
        // Implementation would query GL for average response/resolution times

        agent = agentRepository.save(agent);
    }

    @Override
    @Transactional
    public SupportAgentResponse delete(Long id) {
        SupportAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support agent not found"));

        agent.softDelete();
        agentRepository.save(agent);
        return SupportAgentMapper.toResponse(agent);
    }
}


