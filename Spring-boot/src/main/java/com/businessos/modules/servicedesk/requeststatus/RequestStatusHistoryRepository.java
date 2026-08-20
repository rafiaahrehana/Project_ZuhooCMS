package com.businessos.modules.servicedesk.requeststatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, Long> {

    List<RequestStatusHistory> findByServiceRequestIdOrderByChangedAtAsc(Long serviceRequestId);
}
