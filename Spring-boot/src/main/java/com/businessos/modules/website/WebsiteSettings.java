package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "website_settings")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteSettings extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    private String companyName;
    private String logoUrl;
    private String faviconUrl;
    private String tagline;

    // Theme / branding
    private String primaryColor;
    private String secondaryColor;
    private String gradient;
    private String font;
    private Integer radius;
    private String buttonStyle;      // rounded | pill | square
    private boolean darkMode;
    private String navbarStyle;      // solid | transparent | glass
    private String footerStyle;      // dark | light
    private boolean animations;
    private Integer spacing;

    // Hero
    private String heroHeading;
    private String heroSubheading;
    private String heroImageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "website_hero_images", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> heroImages = new ArrayList<>();

    // Content
    @Column(columnDefinition = "TEXT")
    private String aboutText;
    @Column(columnDefinition = "TEXT")
    private String mission;
    @Column(columnDefinition = "TEXT")
    private String vision;

    // Contact
    private String email;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(name = "map_embed_url", columnDefinition = "TEXT")
    private String mapEmbedUrl;
    private String whatsapp;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "website_social_links", joinColumns = @JoinColumn(name = "settings_id"))
    @Builder.Default
    private List<SocialLink> socialLinks = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "website_settings_stats", joinColumns = @JoinColumn(name = "settings_id"))
    @Builder.Default
    private List<Stat> stats = new ArrayList<>();

    private String copyright;

    // SEO
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String ogImage;
}
