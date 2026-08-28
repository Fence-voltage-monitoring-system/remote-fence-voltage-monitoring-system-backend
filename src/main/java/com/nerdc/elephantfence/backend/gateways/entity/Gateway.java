package com.nerdc.elephantfence.backend.gateways.entity;

import com.nerdc.elephantfence.backend.fences.entity.Fence;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gateways")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String serial;

    @Column(nullable = false, unique = true, length = 100)
    private String imei;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "offline";

    @Builder.Default
    @Column(nullable = false)
    private Integer signal = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer power = 0;

    @Column(length = 50)
    private String firmware;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "gateway_fences",
        joinColumns = @JoinColumn(name = "gateway_id"),
        inverseJoinColumns = @JoinColumn(name = "fence_id")
    )
    @Builder.Default
    private Set<Fence> fences = new HashSet<>();
}
