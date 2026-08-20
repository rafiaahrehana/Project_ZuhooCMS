package com.businessos.auth.role.service;

import com.businessos.auth.role.entity.Permission;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void initializePermissions() {
        for (PermissionCode code : PermissionCode.values()) {
            if (!permissionRepository.existsByCode(code.name())) {

                Permission permission = Permission.builder()
                        .code(code.name())
                        .name(formatName(code.name()))
                        .description(code.name())
                        .build();
                permissionRepository.save(permission);
            }
        }
    }

    private String formatName(String code) {
        String[] words = code.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return builder.toString().trim();
    }
}
