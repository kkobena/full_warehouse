package com.kobe.warehouse.service.portee;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PorteeMagasinServiceImpl implements PorteeMagasinService {

    /** Code {@code nav_item} du droit, posé par V2.1.51. */
    public static final String DROIT_SUPERVISION = "supervision.magasins";

    /**
     * L'arbre est profond d'un niveau — une officine ne dépend pas d'une autre. La descente est
     * bornée quand même : un parent mal posé à la main ne doit pas faire tourner le serveur.
     */
    private static final int PROFONDEUR_MAX = 3;

    private final UserService userService;
    private final MagasinRepository magasinRepository;
    private final NavAccessService navAccessService;

    public PorteeMagasinServiceImpl(
            UserService userService, MagasinRepository magasinRepository, NavAccessService navAccessService) {
        this.userService = userService;
        this.magasinRepository = magasinRepository;
        this.navAccessService = navAccessService;
    }

    @Override
    public Magasin resoudreLeMagasinDExploitation() {
        return userService.getUser().getMagasin();
    }

    @Override
    public Set<Integer> resoudreLaPorteeDeSupervision() {
        Magasin exploitation = resoudreLeMagasinDExploitation();
        Set<Integer> portee = new LinkedHashSet<>();
        portee.add(exploitation.getId());
        if (!detientLeDroitDeSupervision()) {
            return portee;
        }
        Deque<Integer> aExplorer = new ArrayDeque<>();
        aExplorer.add(exploitation.getId());
        for (int profondeur = 0; profondeur < PROFONDEUR_MAX && !aExplorer.isEmpty(); profondeur++) {
            Deque<Integer> suivants = new ArrayDeque<>();
            while (!aExplorer.isEmpty()) {
                List<Magasin> rattaches = magasinRepository.findAllByParentId(aExplorer.poll());
                for (Magasin rattache : rattaches) {
                    if (portee.add(rattache.getId())) {
                        suivants.add(rattache.getId());
                    }
                }
            }
            aExplorer = suivants;
        }
        return portee;
    }

    @Override
    public boolean superviseDAutresMagasins() {
        return resoudreLaPorteeDeSupervision().size() > 1;
    }

    private boolean detientLeDroitDeSupervision() {
        return navAccessService.isAllowed(List.of(DROIT_SUPERVISION), NavAction.ACCESS);
    }
}
