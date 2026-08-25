package com.nerdc.elephantfence.backend.gateways.repository;

import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayRepository extends JpaRepository<Gateway, Long> {
    Optional<Gateway> findBySerialIgnoreCase(String serial);
    Optional<Gateway> findByImei(String imei);
    boolean existsBySerialIgnoreCase(String serial);
    boolean existsByImei(String imei);

    @Query(value = "SELECT COUNT(*) FROM devices WHERE gateway_id = :gatewayId", nativeQuery = true)
    int countDevicesByGatewayId(@Param("gatewayId") Long gatewayId);

    @Query(value = "SELECT name FROM fences WHERE gateway_id = :gatewayId " +
                   "UNION " +
                   "SELECT f.name FROM fences f JOIN gateway_fences gf ON f.id = gf.fence_id WHERE gf.gateway_id = :gatewayId", nativeQuery = true)
    List<String> findFenceNamesByGatewayId(@Param("gatewayId") Long gatewayId);
}
