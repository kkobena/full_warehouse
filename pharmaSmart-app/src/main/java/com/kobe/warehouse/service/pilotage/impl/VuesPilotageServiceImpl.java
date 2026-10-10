package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.PilotageVue;
import com.kobe.warehouse.repository.PilotageVueRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.pilotage.VuePilotageDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.VuesPilotageService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VuesPilotageServiceImpl implements VuesPilotageService {

    private final PilotageVueRepository pilotageVueRepository;
    private final UserService userService;

    public VuesPilotageServiceImpl(PilotageVueRepository pilotageVueRepository, UserService userService) {
        this.pilotageVueRepository = pilotageVueRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VuePilotageDTO> listerVues() {
        AppUser utilisateur = userService.getUser();
        return pilotageVueRepository.listerVisibles(utilisateur.getId()).stream().map(vue -> versDTO(vue, utilisateur)).toList();
    }

    @Override
    public VuePilotageDTO enregistrerVue(VuePilotageDTO dto) {
        AppUser utilisateur = userService.getUser();
        PilotageVue vue = dto.id() == null ? new PilotageVue().setProprietaire(utilisateur) : lireModifiable(dto.id(), utilisateur);
        vue
            .setLibelle(dto.libelle().trim())
            .setIndicateurs(List.copyOf(dto.indicateurs()))
            .setAxe(dto.axe())
            .setAxeCroise(dto.axe2())
            .setNombreElements(Math.max(0, dto.top()))
            .setTri(dto.tri())
            .setAffichage(dto.affichage())
            .setPartagee(dto.partagee());
        return versDTO(pilotageVueRepository.save(vue), utilisateur);
    }

    @Override
    public void supprimerVue(Long id) {
        pilotageVueRepository.delete(lireModifiable(id, userService.getUser()));
    }

    private PilotageVue lireModifiable(Long id, AppUser utilisateur) {
        PilotageVue vue = pilotageVueRepository.findById(id).orElseThrow(() -> new GenericError("Vue introuvable"));
        if (!appartientA(vue, utilisateur)) {
            throw new ForbiddenOperationException("Seul l'auteur d'une vue peut la modifier ou la supprimer", "pilotageVueNonModifiable");
        }
        return vue;
    }

    private static boolean appartientA(PilotageVue vue, AppUser utilisateur) {
        return !vue.estLivree() && Objects.equals(vue.getProprietaire().getId(), utilisateur.getId());
    }

    private static VuePilotageDTO versDTO(PilotageVue vue, AppUser utilisateur) {
        return new VuePilotageDTO(
            vue.getId(),
            vue.getLibelle(),
            vue.getIndicateurs(),
            vue.getAxe(),
            vue.getAxeCroise(),
            vue.getNombreElements(),
            vue.getTri(),
            vue.getAffichage(),
            vue.estLivree(),
            vue.isPartagee(),
            appartientA(vue, utilisateur)
        );
    }
}
