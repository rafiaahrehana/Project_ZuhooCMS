package com.businessos.modules.support.category;

public class SupportCategoryMapper {
    public static SupportCategoryResponse toResponse(SupportCategory entity) {
        if (entity == null) return null;
        return SupportCategoryResponse.builder()
                .id(entity.getId())
                .categoryName(entity.getCategoryName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .icon(entity.getIcon())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
