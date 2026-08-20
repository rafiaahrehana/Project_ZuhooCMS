package com.businessos.modules.hrm.leave.holiday;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByIdAndCompanyId(Long id, Long companyId);

    Page<Holiday> findByCompanyId(Long companyId, Pageable pageable);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDate date);

    @Query("""
        SELECT h FROM Holiday h
        WHERE h.company.id = :companyId
          AND h.date BETWEEN :from AND :to
          AND h.deleted = false
        ORDER BY h.date ASC
        """)
    List<Holiday> findByCompanyAndDateRange(
        @Param("companyId") Long companyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("SELECT COUNT(h) FROM Holiday h WHERE h.company.id = :companyId AND YEAR(h.date) = :year AND h.deleted = false")
    long countByCompanyAndYear(@Param("companyId") Long companyId, @Param("year") int year);
}
