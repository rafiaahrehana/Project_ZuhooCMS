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

/**
 * Merges the former TeamMember and Testimonial entities: both are "a person shown on the
 * public website" and shared most of their fields. {@link #type} picks which set of
 * fields applies - TEAM_MEMBER uses bio/photoUrl/email, TESTIMONIAL uses company/quote/
 * avatarUrl/rating; the other side's fields are simply left null.
 */
@Entity
@Table(name = "website_people")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsitePerson extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PersonType type;

    private String name;
    private String role;

    // TEAM_MEMBER fields
    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;
    private String photoUrl;
    private String email;

    // TESTIMONIAL fields
    private String company;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String quote;
    private String avatarUrl;
    private int rating;
}
