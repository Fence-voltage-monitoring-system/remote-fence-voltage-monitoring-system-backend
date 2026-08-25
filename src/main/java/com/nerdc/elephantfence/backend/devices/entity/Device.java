package com.nerdc.elephantfence.backend.devices.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String serial;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "Voltage Monitor";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.offline;

    private Double voltage;

    @Column(nullable = false)
    @Builder.Default
    private Integer signal = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer battery = 0; // Maps to database column 'battery'

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;

    @Column(name = "gateway_id")
    private Long gatewayId;

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "section_id")
    private Long sectionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
