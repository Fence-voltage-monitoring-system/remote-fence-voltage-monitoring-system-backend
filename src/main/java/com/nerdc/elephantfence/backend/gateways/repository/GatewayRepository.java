package com.nerdc.elephantfence.backend.gateways.repository;

import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GatewayRepository extends JpaRepository<Gateway, Long> {
    boolean existsBySerialIgnoreCase(String serial);
    boolean existsBySerialIgnoreCaseAndIdNot(String serial, Long id);
    boolean existsByImeiIgnoreCase(String imei);
    boolean existsByImeiIgnoreCaseAndIdNot(String imei, Long id);
}
