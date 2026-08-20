package com.businessos.modules.ai.repository;

import com.businessos.modules.ai.entity.AiDailyBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AiDailyBriefingRepository extends JpaRepository<AiDailyBriefing, Long> {
    Optional<AiDailyBriefing> findByCompanyIdAndUserIdAndBriefingDate(Long companyId, Long userId, LocalDate briefingDate);
}
