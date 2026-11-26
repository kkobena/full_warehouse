package com.kobe.warehouse.service.portee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.UserService;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le titulaire doit consulter les mouvements de son dépôt et de sa parapharmacie <b>sans s'y
 * connecter</b> : c'est la demande qui a ouvert ce chantier. D'où deux portées distinctes, et ces
 * essais tiennent ce qui les sépare.
 *
 * <p>Le point le plus facile à manquer : <b>le droit n'élargit que la lecture</b>. Le magasin
 * d'exploitation reste celui du compte, superviseur ou non. Confondre les deux donnerait à un
 * titulaire le pouvoir d'encaisser au nom de sa parapharmacie sans l'avoir voulu.
 */
@DisplayName("Portée — exploitation et supervision")
class PorteeMagasinTest {

    private UserService userService;
    private MagasinRepository magasinRepository;
    private NavAccessService navAccessService;
    private PorteeMagasinServiceImpl service;

    @BeforeEach
    void preparer() {
        userService = mock(UserService.class);
        magasinRepository = mock(MagasinRepository.class);
        navAccessService = mock(NavAccessService.class);
        service = new PorteeMagasinServiceImpl(userService, magasinRepository, navAccessService);
    }

    @Nested
    @DisplayName("sans le droit de supervision")
    class SansLeDroit {

        @BeforeEach
        void sansDroit() {
            when(navAccessService.isAllowed(any(Collection.class), any(NavAction.class)))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("la portée se limite à son propre magasin")
        void porteeLimitee() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));

            assertThat(service.resoudreLaPorteeDeSupervision()).containsExactly(1);
            assertThat(service.superviseDAutresMagasins()).isFalse();
        }

        /**
         * Sans le droit, on ne va même pas chercher les rattachés : la question ne se pose pas.
         */
        @Test
        @DisplayName("les rattachés ne sont même pas interrogés")
        void aucuneRequeteInutile() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));

            service.resoudreLaPorteeDeSupervision();

            verify(magasinRepository, never()).findAllByParentId(anyInt());
        }
    }

    @Nested
    @DisplayName("avec le droit de supervision")
    class AvecLeDroit {

        @BeforeEach
        void avecDroit() {
            when(navAccessService.isAllowed(any(Collection.class), any(NavAction.class)))
                    .thenReturn(true);
        }

        @Test
        @DisplayName("la portée couvre le magasin et ses rattachés")
        void porteeElargie() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findAllByParentId(1))
                    .thenReturn(List.of(magasin(5, TypeMagasin.DEPOT), magasin(7, TypeMagasin.UNITE_GESTION)));

            assertThat(service.resoudreLaPorteeDeSupervision()).containsExactlyInAnyOrder(1, 5, 7);
            assertThat(service.superviseDAutresMagasins()).isTrue();
        }

        /**
         * Le droit élargit la LECTURE. S'il changeait aussi le magasin d'exploitation, un titulaire
         * encaisserait au nom de sa parapharmacie sans l'avoir voulu.
         */
        @Test
        @DisplayName("le magasin d'exploitation ne bouge pas pour autant")
        void lExploitationNeBougePas() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findAllByParentId(1)).thenReturn(List.of(magasin(7, TypeMagasin.UNITE_GESTION)));

            assertThat(service.resoudreLaPorteeDeSupervision()).hasSize(2);
            assertThat(service.resoudreLeMagasinDExploitation().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("sans rattaché, la portée reste à un seul magasin")
        void sansRattache() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findAllByParentId(1)).thenReturn(List.of());

            assertThat(service.resoudreLaPorteeDeSupervision()).containsExactly(1);
            assertThat(service.superviseDAutresMagasins()).isFalse();
        }

        /**
         * Un rattachement circulaire posé à la main ne doit pas faire tourner la descente.
         */
        @Test
        @DisplayName("un cycle posé à la main ne fait pas tourner la descente")
        void cycleBorne() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findAllByParentId(1)).thenReturn(List.of(magasin(5, TypeMagasin.DEPOT)));
            when(magasinRepository.findAllByParentId(5)).thenReturn(List.of(magasin(1, TypeMagasin.OFFICINE)));

            assertThat(service.resoudreLaPorteeDeSupervision()).containsExactlyInAnyOrder(1, 5);
        }

        @Test
        @DisplayName("le droit interrogé est bien celui de la migration")
        void leBonDroit() {
            posterUtilisateurDans(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findAllByParentId(1)).thenReturn(List.of());

            service.resoudreLaPorteeDeSupervision();

            verify(navAccessService).isAllowed(List.of("supervision.magasins"), NavAction.ACCESS);
        }
    }

    // ===== jeu d'essai =====

    private void posterUtilisateurDans(Magasin magasin) {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLogin("titulaire");
        user.setMagasin(magasin);
        when(userService.getUser()).thenReturn(user);
    }

    private Magasin magasin(int id, TypeMagasin type) {
        Magasin magasin = new Magasin();
        magasin.setId(id);
        magasin.setName("MAGASIN " + id);
        magasin.setFullName("MAGASIN COMPLET " + id);
        magasin.setTypeMagasin(type);
        return magasin;
    }
}
