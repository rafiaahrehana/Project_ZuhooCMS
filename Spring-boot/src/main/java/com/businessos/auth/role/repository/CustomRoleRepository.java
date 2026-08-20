package com.businessos.auth.role.repository;

import com.businessos.auth.role.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {

    Optional<CustomRole> findByIdAndCompanyId(Long id, Long companyId);

    List<CustomRole> findByCompanyId(Long companyId);

    List<CustomRole> findByCompanyIdAndActiveTrue(Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);

}