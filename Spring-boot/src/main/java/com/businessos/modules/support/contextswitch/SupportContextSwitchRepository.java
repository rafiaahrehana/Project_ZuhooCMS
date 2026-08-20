package com.businessos.modules.support.contextswitch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportContextSwitchRepository extends JpaRepository<SupportContextSwitch, Long> {
    Optional<SupportContextSwitch> findBySupportAgentIdAndStillActiveTrue(Long agentId);
    Page<SupportContextSwitch> findBySupportAgentId(Long agentId, Pageable pageable);
    List<SupportContextSwitch> findByStillActiveTrue();
}
