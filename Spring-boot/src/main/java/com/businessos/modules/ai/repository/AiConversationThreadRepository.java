package com.businessos.modules.ai.repository;

import com.businessos.modules.ai.entity.AiConversationThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationThreadRepository extends JpaRepository<AiConversationThread, Long> {

    Page<AiConversationThread> findByCompanyIdAndUserIdOrderByUpdatedAtDesc(
            Long companyId, Long userId, Pageable pageable);

    // Scoped by both companyId and userId, not just id - a thread belongs to
    // exactly the employee who started it, even though threads are also
    // tenant-filtered at the entity level.
    java.util.Optional<AiConversationThread> findByIdAndCompanyIdAndUserId(
            Long id, Long companyId, Long userId);
}
