package com.businessos.modules.crm.tag;

import java.util.List;

public interface TagService {
    List<TagResponse> list();
    TagResponse create(TagRequest request);
    TagResponse update(Long id, TagRequest request);
    void delete(Long id);
}
