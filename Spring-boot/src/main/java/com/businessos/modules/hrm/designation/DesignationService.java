package com.businessos.modules.hrm.designation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DesignationService {

    DesignationResponse create(DesignationRequest request);
    DesignationResponse getById(Long id);
    Page<DesignationResponse> listAll(Pageable pageable);
    List<DesignationResponse> listActive();
    DesignationResponse update(Long id, DesignationRequest request);
    DesignationResponse toggleActive(Long id);
    void delete(Long id);

}
