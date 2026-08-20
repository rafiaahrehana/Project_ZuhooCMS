package com.businessos.modules.hrm.payroll.components;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StructureExtraComponentRepository extends JpaRepository<StructureExtraComponent, Long> {
    List<StructureExtraComponent> findByStructureIdOrderByIdAsc(Long structureId);
    void deleteByStructureId(Long structureId);
}
