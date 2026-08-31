package com.nerdc.elephantfence.backend.support.repository;

import com.nerdc.elephantfence.backend.support.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
}
