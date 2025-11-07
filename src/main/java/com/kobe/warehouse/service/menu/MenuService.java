package com.kobe.warehouse.service.menu;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.repository.MenuRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.MenuDTO;
import com.kobe.warehouse.service.dto.MenuSpecialisation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final UserService userService;

    public MenuService(MenuRepository menuRepository, UserService userService) {
        this.menuRepository = menuRepository;
        this.userService = userService;
    }

    public Set<MenuDTO> getConnectedUserMenus() {
        var menus = new HashSet<MenuDTO>();
        Set<MenuSpecialisation> menuSpecialisations = new HashSet<>();
        this.userService.getUserWithAuthorities()
            .ifPresent(user -> {
                menuSpecialisations.addAll(
                    this.menuRepository.getRoleMenus(
                            user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet())
                           )

                );
            });
        Map<Boolean, List<MenuSpecialisation>> partitioned = menuSpecialisations
            .stream()
            .collect(Collectors.partitioningBy(MenuSpecialisation::isRoot));
        List<MenuSpecialisation> roots = partitioned.get(true);
        List<MenuSpecialisation> children = partitioned.get(false);
        roots.forEach(root -> {
            MenuDTO menu = new MenuDTO();

            menu.setLibelle(root.getLibelle());
            menu.setName(root.getName());

            menu.setMenus(
                children
                    .stream()
                    .filter(
                        menuSpecialisation -> menuSpecialisation.getParent() != null && menuSpecialisation.getParent().equals(root.getId())
                    )
                    .map(menuSpecialisation -> {
                        MenuDTO child = new MenuDTO();
                        child.setLibelle(menuSpecialisation.getLibelle());
                        child.setName(menuSpecialisation.getName());

                        return child;
                    })
                    .collect(Collectors.toSet())
            );
            menus.add(menu);
        });

        return menus;
    }
}
