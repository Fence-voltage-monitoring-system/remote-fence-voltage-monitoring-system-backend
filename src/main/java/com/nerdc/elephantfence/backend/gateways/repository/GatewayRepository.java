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
    boolean existsBySerialIgnoreCaseAndIdNot(String serial, Long id);
    boolean existsByImeiIgnoreCase(String imei);
    boolean existsByImeiIgnoreCaseAndIdNot(String imei, Long id);
    boolean existsByImei(String imei);

    Optional<Gateway> findByNameIgnoreCase(String name);

    @Query(value = "SELECT COUNT(*) FROM devices WHERE gateway_id = :gatewayId", nativeQuery = true)
    int countDevicesByGatewayId(@Param("gatewayId") Long gatewayId);

    @Query(value = "SELECT name FROM fences WHERE gateway_id = :gatewayId " +
                   "UNION " +
                   "SELECT f.name FROM fences f JOIN gateway_fences gf ON f.id = gf.fence_id WHERE gf.gateway_id = :gatewayId", nativeQuery = true)
    List<String> findFenceNamesByGatewayId(@Param("gatewayId") Long gatewayId);

    @Query(value = "SELECT g.* FROM gateways g JOIN gateway_fences gf ON g.id = gf.gateway_id WHERE gf.fence_id = :fenceId LIMIT 1", nativeQuery = true)
    Optional<Gateway> findGatewayByFenceId(@Param("fenceId") Long fenceId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM gateway_fences WHERE fence_id = :fenceId", nativeQuery = true)
    void unlinkFenceFromAllGateways(@Param("fenceId") Long fenceId);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT INTO gateway_fences (gateway_id, fence_id) VALUES (:gatewayId, :fenceId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void linkFenceToGateway(@Param("gatewayId") Long gatewayId, @Param("fenceId") Long fenceId);
}
