package com.businessos.modules.servicedesk.requestcomment;

import com.businessos.enums.CommentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * FIXES:
 * findByServiceRequestId* — resolves now that base relation is 'serviceRequest' (was 'request')
 * findBy*Visibility*      — resolves now that base field is 'visibility' (was 'boolean internal')
 */
public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {

    Page<RequestComment> findByServiceRequestIdOrderByCreatedAtDesc(
        Long serviceRequestId, Pageable pageable);

    Page<RequestComment> findByServiceRequestIdAndVisibilityOrderByCreatedAtDesc(
        Long serviceRequestId, CommentVisibility visibility, Pageable pageable);
}
