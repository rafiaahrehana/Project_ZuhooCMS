package com.businessos.shared.feature;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String flagKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
