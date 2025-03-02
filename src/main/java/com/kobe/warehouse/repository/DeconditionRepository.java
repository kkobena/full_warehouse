package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Decondition;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Decondition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DeconditionRepository extends JpaRepository<Decondition, Long> {
    @Query("select decondition from Decondition decondition where decondition.user.login = ?#{principal.username}")
    List<Decondition> findByUserIsCurrentUser();
}
