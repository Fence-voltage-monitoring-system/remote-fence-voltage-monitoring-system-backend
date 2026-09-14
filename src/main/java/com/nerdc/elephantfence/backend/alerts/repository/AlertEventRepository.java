package com.nerdc.elephantfence.backend.alerts.repository;

import com.nerdc.elephantfence.backend.alerts.entity.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    List<AlertEvent> findByAlertIdOrderByOccurredAtAsc(Long alertId);
}
