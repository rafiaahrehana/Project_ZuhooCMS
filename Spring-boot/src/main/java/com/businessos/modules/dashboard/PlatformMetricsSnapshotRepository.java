package com.businessos.modules.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlatformMetricsSnapshotRepository extends JpaRepository<PlatformMetricsSnapshot, Long> {

    Optional<PlatformMetricsSnapshot> findBySnapshotDate(LocalDate date);

    List<PlatformMetricsSnapshot> findBySnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(LocalDate from);
}
