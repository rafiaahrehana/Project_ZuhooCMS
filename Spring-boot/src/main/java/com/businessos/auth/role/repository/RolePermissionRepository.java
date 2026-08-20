package com.businessos.auth.role.repository;

import com.businessos.auth.role.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByCustomRoleId(Long customRoleId);
    void deleteByCustomRoleId(Long customRoleId);
    
    boolean existsByCustomRoleIdAndPermission_Code(Long customRoleId, String code);
}
