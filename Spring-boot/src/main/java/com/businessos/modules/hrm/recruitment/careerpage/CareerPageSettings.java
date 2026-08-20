package com.businessos.modules.hrm.recruitment.careerpage;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-company configuration of the PUBLIC careers page. The slug forms the
 * public URL (/careers/{slug}) and must be globally unique - it identifies the
 * tenant to unauthenticated visitors, so it is the only tenant key the public
 * endpoints accept.
 */
@Entity
@Table(name = "career_page_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CareerPageSettings extends BaseEntity {

    @Column(name = "company_id", nullable = false, unique = true)
    private Long companyId;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    private String headline;

    @Column(columnDefinition = "TEXT")
    private String about;

    /** Hex accent for the public page hero/buttons, e.g. #367C2B. */
    @Column(length = 9)
    private String brandColor;

    /** Off = the public page 404s without touching the postings. */
    @Builder.Default
    @Column(nullable = false)
    private boolean published = true;
}
