package com.nerdc.elephantfence.backend.alerts.repository;

import com.nerdc.elephantfence.backend.alerts.entity.AlertComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertCommentRepository extends JpaRepository<AlertComment, Long> {
    List<AlertComment> findByAlertIdOrderByCreatedAtAsc(Long alertId);
}
