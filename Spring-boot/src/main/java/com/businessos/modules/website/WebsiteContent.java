package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;

/**
 * Merges the former CmsPage and BlogPost entities: both are "a piece of published website
 * content addressed by slug". {@link #type} picks which set of fields applies - PAGE only
 * uses slug/title/body, POST additionally uses excerpt/coverImageUrl/author/publishedAt/
 * category/readMinutes; those fields are simply left null for pages.
 */
@Entity
@Table(name = "website_content")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteContent extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    @Column(unique = true)
    private String slug;
    private String title;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String body;

    // POST-only fields
    @Column(length = 600)
    private String excerpt;
    private String coverImageUrl;
    private String author;
    private LocalDateTime publishedAt;
    private String category;
    private int readMinutes;
}
