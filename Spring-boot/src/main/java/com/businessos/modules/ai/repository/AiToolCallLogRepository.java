package com.businessos.modules.ai.repository;

import com.businessos.modules.ai.entity.AiToolCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiToolCallLogRepository extends JpaRepository<AiToolCallLog, Long> {

    Page<AiToolCallLog> findByCompanyIdAndUserIdOrderByCreatedAtDesc(
            Long companyId, Long userId, Pageable pageable);
}
