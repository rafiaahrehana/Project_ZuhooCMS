package com.businessos.modules.servicedesk.servicecategory;

public final class ServiceCategoryMapper {

    public static ServiceCategoryResponse toResponse(ServiceCategory cat) {
        ServiceCategoryResponse r = new ServiceCategoryResponse();
        r.setId(cat.getId());
        r.setName(cat.getName());
        r.setNameBn(cat.getNameBn());
        r.setDescription(cat.getDescription());
        r.setIconUrl(cat.getIconUrl());
        r.setSortOrder(cat.getSortOrder());
        r.setActive(cat.isActive());
        return r;
    }
}
