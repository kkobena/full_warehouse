package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
}
