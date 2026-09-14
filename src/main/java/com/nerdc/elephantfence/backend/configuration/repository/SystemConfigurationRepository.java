package com.nerdc.elephantfence.backend.configuration.repository;

import com.nerdc.elephantfence.backend.configuration.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
    Optional<SystemConfiguration> findBySection(String section);
}
