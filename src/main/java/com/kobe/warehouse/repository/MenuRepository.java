package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.service.dto.MenuSpecialisation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data JPA repository for the {@link Menu} entity. */
public interface MenuRepository extends JpaRepository<Menu, Integer> {
    @EntityGraph(attributePaths = "menus")
    List<Menu> findAll();

    Optional<Menu> findMenuByName(String name);

    @Query(
        value = "SELECT m.id,m.libelle, m.name,m.racine AS root, m.parent_id AS parent FROM menu m JOIN authority_menu am on m.id = am.menu_id WHERE am.authority_name IN ?1 AND m.enable = true ORDER BY m.ordre",
        nativeQuery = true
    )
    List<MenuSpecialisation> getRoleMenus(Set<String> roleNames);
}
