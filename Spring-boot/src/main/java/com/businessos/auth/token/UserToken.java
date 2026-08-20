package com.businessos.auth.token;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken extends BaseEntity {

    @JsonIgnore
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType tokenType;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

}
