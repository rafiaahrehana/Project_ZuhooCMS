package com.businessos.modules.support.message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    Page<SupportMessage> findByTicketId(Long ticketId, Pageable pageable);

    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    long countByTicketIdAndIsInternal(Long ticketId, boolean isInternal);

    List<SupportMessage> findByTicketIdAndIsInternalFalse(Long ticketId);

    List<SupportMessage> findByTicketIdAndIsInternalTrue(Long ticketId);
}
