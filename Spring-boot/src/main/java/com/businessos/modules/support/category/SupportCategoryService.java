package com.businessos.modules.support.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SupportCategoryService {
    SupportCategoryResponse create(SupportCategoryRequest request);
    SupportCategoryResponse getById(Long id);
    SupportCategoryResponse getByName(String name);
    Page<SupportCategoryResponse> getAll(Pageable pageable);
    List<SupportCategoryResponse> getActive();
    SupportCategoryResponse update(Long id, SupportCategoryRequest request);
    void updateStatus(Long id, boolean active);
    SupportCategoryResponse delete(Long id);
}
