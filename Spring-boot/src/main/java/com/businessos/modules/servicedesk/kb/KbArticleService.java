package com.businessos.modules.servicedesk.kb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KbArticleService {
    KbArticleResponse create(KbArticleRequest request);
    KbArticleResponse update(Long id, KbArticleRequest request);
    KbArticleResponse getById(Long id);
    Page<KbArticleResponse> list(String keyword, String status, Pageable pageable);
    KbArticleResponse publish(Long id);
    KbArticleResponse archive(Long id);
    KbArticleResponse markHelpful(Long id);
}
