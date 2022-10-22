package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the Commande entity. */
@SuppressWarnings("unused")
@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

}
