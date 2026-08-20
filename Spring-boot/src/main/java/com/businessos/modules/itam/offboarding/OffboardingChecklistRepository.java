package com.businessos.modules.itam.offboarding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OffboardingChecklistRepository extends JpaRepository<OffboardingChecklist, Long> {

    Optional<OffboardingChecklist> findByIdAndCompanyId(Long id, Long companyId);

    Optional<OffboardingChecklist> findByEmployeeIdAndCompanyId(Long employeeId, Long companyId);

    Page<OffboardingChecklist> findByCompanyId(Long companyId, Pageable pageable);

    List<OffboardingChecklist> findByCompanyIdAndCompletedFalse(Long companyId);

    Page<OffboardingChecklist> findByCompanyIdAndOffboardingDateBetween(Long companyId, LocalDate start, LocalDate end, Pageable pageable);
}
