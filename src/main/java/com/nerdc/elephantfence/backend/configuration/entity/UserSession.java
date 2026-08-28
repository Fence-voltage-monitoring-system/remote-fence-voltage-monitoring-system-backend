package com.nerdc.elephantfence.backend.configuration.entity;

import com.nerdc.elephantfence.backend.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @Column(length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String device;

    @Column(length = 100)
    private String browser;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "approximate_location", length = 150)
    private String approximateLocation;

    @Builder.Default
    @Column(name = "is_suspicious", nullable = false)
    private boolean suspicious = false;

    @CreationTimestamp
    @Column(name = "signed_in_at", nullable = false, updatable = false)
    private OffsetDateTime signedInAt;

    @UpdateTimestamp
    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
