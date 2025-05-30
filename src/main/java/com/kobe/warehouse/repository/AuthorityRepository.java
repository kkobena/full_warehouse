package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Authority;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the {@link Authority} entity. */
@Repository
public interface AuthorityRepository extends JpaRepository<Authority, String> {
    @EntityGraph(attributePaths = "menus")
    Authority findOneByName(String name);

    @EntityGraph(attributePaths = "menus")
    List<Authority> findAll();
}
