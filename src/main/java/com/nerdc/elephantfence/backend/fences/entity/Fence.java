package com.nerdc.elephantfence.backend.fences.entity;

import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(name = "length_km", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lengthKm = BigDecimal.ZERO;

    @Column(name = "average_voltage_kv", precision = 5, scale = 2)
    private BigDecimal averageVoltageKv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FenceHealth health = FenceHealth.OFFLINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_maintenance_user_id")
    private User primaryMaintenanceUser;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "fence_backup_maintenance_users",
        joinColumns = @JoinColumn(name = "fence_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> backupMaintenanceUsers = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
