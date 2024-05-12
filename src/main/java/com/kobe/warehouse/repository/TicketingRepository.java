package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Ticketing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the Ticketing entity. */
@Repository
public interface TicketingRepository extends JpaRepository<Ticketing, Long> {}
