package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findAllBySaleId(Long id);

    Optional<List<Ticket>> findBySaleId(Long id);
}
