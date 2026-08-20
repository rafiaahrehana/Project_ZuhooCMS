package com.businessos.modules.support.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupportAgentService {

    SupportAgentResponse create(SupportAgentRequest request);
    SupportAgentResponse getById(Long id);
    SupportAgentResponse getByUserId(Long userId);
    Page<SupportAgentResponse> getAll(Pageable pageable);
    Page<SupportAgentResponse> getByStatus(SupportAgentStatus status, Pageable pageable);
    List<SupportAgentResponse> getAvailableAgents();

    SupportAgentResponse update(Long id, SupportAgentRequest request);
    void updateStatus(Long id, SupportAgentStatus status);
    void updateAcceptingTickets(Long id, boolean accepting);

    void updateMetrics(Long agentId);

    SupportAgentResponse delete(Long id);
}
