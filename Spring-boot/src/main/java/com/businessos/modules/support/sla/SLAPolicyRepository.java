package com.businessos.modules.support.sla;


import com.businessos.modules.support.ticket.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SLAPolicyRepository extends JpaRepository<SLAPolicy, Long> {

    Optional<SLAPolicy> findByApplicablePriorityAndActiveTrue(TicketPriority priority);

    List<SLAPolicy> findByActiveTrue();

    Optional<SLAPolicy> findByApplicablePriority(TicketPriority priority);

    /** Used for the create-time duplicate check - a soft-deleted policy must not block re-creating one for the same priority. */
    Optional<SLAPolicy> findByApplicablePriorityAndDeletedFalse(TicketPriority priority);
}