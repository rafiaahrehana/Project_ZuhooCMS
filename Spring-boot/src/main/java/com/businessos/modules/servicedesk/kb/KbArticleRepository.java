package com.businessos.modules.servicedesk.kb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    Optional<KbArticle> findByIdAndCompanyId(Long id, Long companyId);

    // clientOnly=true forces PUBLISHED + clientVisible regardless of the requested status,
    // so a client can never see drafts/archived articles by passing a different status filter.
    // The :keyword casts are required - Postgres can't infer a bare parameter's type
    // when it's only ever used inside CONCAT(), and binding it as NULL (no keyword
    // filter applied) then fails with "could not determine data type of parameter"
    // before the "IS NULL" short-circuit is even evaluated.
    @Query("""
        SELECT a FROM KbArticle a
        WHERE a.companyId = :companyId
        AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(a.keywords) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        AND (
            (:clientOnly = true AND a.status = com.businessos.modules.servicedesk.kb.KbArticleStatus.PUBLISHED AND a.clientVisible = true)
            OR
            (:clientOnly = false AND (:status IS NULL OR a.status = :status))
        )
        """)
    Page<KbArticle> search(
        @Param("companyId") Long companyId,
        @Param("keyword") String keyword,
        @Param("status") KbArticleStatus status,
        @Param("clientOnly") boolean clientOnly,
        Pageable pageable
    );
}
