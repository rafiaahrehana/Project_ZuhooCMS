package com.businessos.modules.support.category;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "support_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportCategory extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String categoryName;

    private String description;

    @Builder.Default
    private boolean active = true;

    private String icon;
}
