package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Decondition;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the Decondition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DeconditionRepository extends JpaRepository<Decondition, Long> {

    @Query("select decondition from Decondition decondition where decondition.user.login = ?#{principal.username}")
    List<Decondition> findByUserIsCurrentUser();
}
