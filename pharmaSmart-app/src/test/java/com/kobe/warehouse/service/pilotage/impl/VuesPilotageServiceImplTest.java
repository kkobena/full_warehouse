package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.PilotageVue;
import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.repository.PilotageVueRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.pilotage.VuePilotageDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pilotage — vues enregistrées")
class VuesPilotageServiceImplTest {

    @Mock
    private PilotageVueRepository pilotageVueRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private VuesPilotageServiceImpl service;

    private final AppUser moi = utilisateur(1);

    @BeforeEach
    void connecter() {
        when(userService.getUser()).thenReturn(moi);
        when(pilotageVueRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    @DisplayName("une nouvelle vue appartient à son auteur, qui peut la modifier")
    void nouvelleVue() {
        VuePilotageDTO vue = service.enregistrerVue(dto(null));

        assertThat(vue.modifiable()).isTrue();
        assertThat(vue.livree()).isFalse();
        assertThat(vue.libelle()).isEqualTo("Mes antalgiques");
    }

    @Test
    @DisplayName("une vue livrée d'office n'est ni modifiable ni supprimable")
    void vueLivreeProtegee() {
        when(pilotageVueRepository.findById(7L)).thenReturn(Optional.of(new PilotageVue()));

        assertThatThrownBy(() -> service.enregistrerVue(dto(7L))).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.supprimerVue(7L)).isInstanceOf(ForbiddenOperationException.class);
        verify(pilotageVueRepository, never()).delete(any());
    }

    @Test
    @DisplayName("la vue partagée d'un collègue se lit, mais ne se modifie pas")
    void vueDUnCollegue() {
        PilotageVue vue = new PilotageVue().setProprietaire(utilisateur(2)).setPartagee(true).setIndicateurs(List.of(IndicateurPilotage.CA_TTC));
        when(pilotageVueRepository.findById(8L)).thenReturn(Optional.of(vue));
        when(pilotageVueRepository.listerVisibles(1)).thenReturn(List.of(vue));

        assertThat(service.listerVues()).extracting(VuePilotageDTO::modifiable).containsExactly(false);
        assertThatThrownBy(() -> service.supprimerVue(8L)).isInstanceOf(ForbiddenOperationException.class);
    }

    private static VuePilotageDTO dto(Long id) {
        return new VuePilotageDTO(id, " Mes antalgiques ", List.of(IndicateurPilotage.CA_TTC), AxeAnalyse.PRODUIT, null, 20, TriAnalyse.VALEUR, AffichageAnalyse.TABLEAU, false, false, false);
    }

    private static AppUser utilisateur(int id) {
        AppUser utilisateur = new AppUser();
        utilisateur.setId(id);
        return utilisateur;
    }
}
