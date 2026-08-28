package com.nerdc.elephantfence.backend.configuration.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.nerdc.elephantfence.backend.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String section;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_data", nullable = false, columnDefinition = "jsonb")
    private JsonNode configData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @Column(columnDefinition = "text")
    private String reason;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
