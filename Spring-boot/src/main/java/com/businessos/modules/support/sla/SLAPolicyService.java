package com.businessos.modules.support.sla;

import com.businessos.modules.support.ticket.TicketPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SLAPolicyService {
    SLAPolicyResponse create(SLAPolicyRequest request);
    SLAPolicyResponse getById(Long id);
    SLAPolicyResponse getByPriority(TicketPriority priority);
    Page<SLAPolicyResponse> getAll(Pageable pageable);
    List<SLAPolicyResponse> getActive();
    SLAPolicyResponse update(Long id, SLAPolicyRequest request);
    void updateStatus(Long id, boolean active);
    SLAPolicyResponse delete(Long id);
}
