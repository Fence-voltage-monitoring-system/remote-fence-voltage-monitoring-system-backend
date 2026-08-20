package com.nerdc.elephantfence.backend.locations.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "districts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_districts_province_name", columnNames = {"province_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(nullable = false, length = 100)
    private String name;
}
