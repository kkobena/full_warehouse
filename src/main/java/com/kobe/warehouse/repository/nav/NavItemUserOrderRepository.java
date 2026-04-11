package com.kobe.warehouse.repository.nav;

import com.kobe.warehouse.domain.nav.NavItemUserOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavItemUserOrderRepository extends JpaRepository<NavItemUserOrder, Integer> {

    List<NavItemUserOrder> findAllByUserLogin(String userLogin);

    Optional<NavItemUserOrder> findByUserLoginAndNavItemId(String userLogin, Integer navItemId);
}

