package com.nerdc.elephantfence.backend.sections.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sections", uniqueConstraints = @UniqueConstraint(
        name = "uk_sections_fence_code", columnNames = {"fence_id", "code"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fence_id", nullable = false)
    private Long fenceId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "start_gps", length = 100)
    private String startGps;

    @Column(name = "end_gps", length = 100)
    private String endGps;

    @Column(name = "length_km", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lengthKm = BigDecimal.ZERO;

    @Column(name = "voltage_kv", precision = 5, scale = 2)
    private BigDecimal voltageKv;

    private Integer battery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SectionStatus status = SectionStatus.OFFLINE;

    @Column(name = "province_id")
    private Long provinceId;

    @Column(name = "district_id")
    private Long districtId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
