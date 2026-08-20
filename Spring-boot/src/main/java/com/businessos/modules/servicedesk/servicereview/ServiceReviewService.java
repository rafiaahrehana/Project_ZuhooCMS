package com.businessos.modules.servicedesk.servicereview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceReviewService {

    ServiceReviewResponse submitOrUpdate(ServiceReviewRequest request);

    ServiceReviewResponse getById(Long id);

    Page<ServiceReviewResponse> listAll(Pageable pageable);

    Page<ServiceReviewResponse> listByService(Long hubServiceId, Pageable pageable);

    Double getAverageRatingByService(Long hubServiceId);

    Double getAverageRatingByCompany();

    void delete(Long id);
}
