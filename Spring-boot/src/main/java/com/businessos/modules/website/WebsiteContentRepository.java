package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WebsiteContentRepository extends JpaRepository<WebsiteContent, Long> {
    Optional<WebsiteContent> findBySlugAndCompanyIdAndType(String slug, Long companyId, ContentType type);
    List<WebsiteContent> findByCompanyIdAndTypeOrderByPublishedAtDesc(Long companyId, ContentType type);
    List<WebsiteContent> findByCompanyIdAndTypeAndCategoryIgnoreCase(Long companyId, ContentType type, String category);

    /** Admin listing (blog posts or pages) - not filtered/sorted for public display. */
    List<WebsiteContent> findByCompanyIdAndType(Long companyId, ContentType type);
}
