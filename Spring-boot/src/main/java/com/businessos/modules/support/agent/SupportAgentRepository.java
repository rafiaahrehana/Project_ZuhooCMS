package com.businessos.modules.support.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportAgentRepository extends JpaRepository<SupportAgent, Long> {

    Optional<SupportAgent> findByUserId(Long userId);

    Page<SupportAgent> findByStatus(SupportAgentStatus status, Pageable pageable);

    List<SupportAgent> findByStatusAndAcceptingTicketsTrue(SupportAgentStatus status);

    Page<SupportAgent> findAll(Pageable pageable);
}