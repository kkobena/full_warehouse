package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for the {@link Menu} entity. */
public interface MenuRepository extends JpaRepository<Menu, Long> {
  @EntityGraph(attributePaths = "menus")
  List<Menu> findAll();

  Optional<Menu> findMenuByName(String name);
}
