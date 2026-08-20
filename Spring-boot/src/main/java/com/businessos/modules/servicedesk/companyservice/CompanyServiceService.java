package com.businessos.modules.servicedesk.companyservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompanyServiceService {

    CompanyServiceResponse create(CompanyServiceRequest request);

    CompanyServiceResponse getById(Long id);

    Page<CompanyServiceResponse> listAll(Long categoryId, Pageable pageable);

    List<CompanyServiceResponse> listActive();

    CompanyServiceResponse update(Long id, CompanyServiceRequest request);

    CompanyServiceResponse toggleActive(Long id);

    void delete(Long id);
}
