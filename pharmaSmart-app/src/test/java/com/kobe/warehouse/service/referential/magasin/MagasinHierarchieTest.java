package com.kobe.warehouse.service.referential.magasin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.service.RayonService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Un dépôt n'existe pas seul : la réglementation ivoirienne exige, à son ouverture, une copie de la
 * licence de l'officine <b>dont il dépendra</b>. Le rattachement est donc un lien opposable, et ces
 * essais tiennent les règles qui l'encadrent.
 *
 * <p>Trois d'entre elles ne sont pas de l'arithmétique mais de la gestion :
 *
 * <ul>
 *   <li><b>Une officine est racine.</b> La loi interdit à un pharmacien de posséder plus d'une
 *       officine : une grappe d'officines n'a donc pas de sens.
 *   <li><b>Le parent absent n'est pas une erreur</b> s'il n'existe qu'une officine : le cas est
 *       certain, et imposer un champ de plus aux écrans n'apporterait rien. Avec deux officines en
 *       revanche, deviner serait pire que demander.
 *   <li><b>Seule la racine porte une identité FNE.</b> Un dépôt ne vend pas au comptoir et une
 *       unité de gestion facture sous l'identité de son officine. Laisser renseigner un point de
 *       vente ailleurs produirait des factures normalisées au mauvais nom — silencieusement.
 * </ul>
 */
@DisplayName("Magasin — le rattachement à l'officine")
class MagasinHierarchieTest {

    private MagasinRepository magasinRepository;
    private MagasinServiceImpl service;

    @BeforeEach
    void preparer() {
        magasinRepository = mock(MagasinRepository.class);
        StorageService storageService = mock(StorageService.class);
        when(storageService.createStorageForNewMagasin(any())).thenReturn(Set.<Storage>of());
        service = new MagasinServiceImpl(
            magasinRepository,
            mock(UserService.class),
            storageService,
            mock(RayonService.class),
            mock(PorteeMagasinService.class)
        );
        when(magasinRepository.save(any(Magasin.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Nested
    @DisplayName("ce qui est refusé")
    class CeQuiEstRefuse {

        @Test
        @DisplayName("une officine ne dépend d'aucun autre magasin")
        void officineSansParent() {
            assertThatThrownBy(() -> service.save(dto(TypeMagasin.OFFICINE, 1)))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("racine");
        }

        @Test
        @DisplayName("un magasin ne peut pas dépendre de lui-même")
        void pasDeParentCirculaire() {
            MagasinDTO dto = dto(TypeMagasin.DEPOT, 7);
            dto.setId(7);

            assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("lui-même");
        }

        @Test
        @DisplayName("un dépôt ne peut pas dépendre d'un autre dépôt")
        void parentForcementOfficine() {
            when(magasinRepository.findById(9)).thenReturn(Optional.of(magasin(9, TypeMagasin.DEPOT)));

            assertThatThrownBy(() -> service.save(dto(TypeMagasin.DEPOT, 9)))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("que d'une officine");
        }

        @Test
        @DisplayName("avec deux officines, le rattachement doit être précisé")
        void deuxOfficinesExigentUnChoix() {
            when(magasinRepository.findAllByTypeMagasin(TypeMagasin.OFFICINE))
                .thenReturn(List.of(magasin(1, TypeMagasin.OFFICINE), magasin(2, TypeMagasin.OFFICINE)));

            assertThatThrownBy(() -> service.save(dto(TypeMagasin.DEPOT, null)))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Préciser l'officine");
        }

        @Test
        @DisplayName("un dépôt ne porte pas d'identité FNE")
        void fneReserveeALOfficine() {
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));
            MagasinDTO dto = dto(TypeMagasin.DEPOT, 1);
            dto.setFnePointOfSale("PV-002");

            assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("identité FNE");
        }

        @Test
        @DisplayName("une unité de gestion non plus : elle facture sous l'identité de son officine")
        void fneRefuseeAUneUniteDeGestion() {
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));
            MagasinDTO dto = dto(TypeMagasin.UNITE_GESTION, 1);
            dto.setFneSecretKey("secret");

            assertThatThrownBy(() -> service.save(dto)).isInstanceOf(GenericError.class);
        }
    }

    @Nested
    @DisplayName("ce qui est accepté")
    class CeQuiEstAccepte {

        @Test
        @DisplayName("avec une seule officine, le dépôt lui est rattaché sans qu'on le demande")
        void rattachementImplicite() {
            Magasin officine = magasin(1, TypeMagasin.OFFICINE);
            when(magasinRepository.findAllByTypeMagasin(TypeMagasin.OFFICINE)).thenReturn(List.of(officine));

            MagasinDTO enregistre = service.save(dto(TypeMagasin.DEPOT, null));

            assertThat(enregistre.getParentId()).isEqualTo(1);
        }

        @Test
        @DisplayName("un dépôt rattaché explicitement à son officine")
        void rattachementExplicite() {
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));

            MagasinDTO enregistre = service.save(dto(TypeMagasin.DEPOT, 1));

            assertThat(enregistre.getParentId()).isEqualTo(1);
        }

        @Test
        @DisplayName("l'officine garde son identité FNE : c'est elle qui certifie")
        void officineGardeSaFne() {
            MagasinDTO dto = dto(TypeMagasin.OFFICINE, null);
            dto.setFnePointOfSale("PV-001");

            assertThatCode(() -> service.save(dto)).doesNotThrowAnyException();
        }

        /**
         * Commander en propre ne se déduit pas du type : une parapharmacie commande sa cosmétique
         * alors qu'elle n'est pas un établissement, un dépôt ne commande pas alors qu'il en est un.
         */
        @Test
        @DisplayName("une unité de gestion commande en propre, un dépôt non")
        void defautDeLaCapaciteDeCommander() {
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));

            assertThat(service.save(dto(TypeMagasin.UNITE_GESTION, 1)).getCommandeEnPropre()).isTrue();
            assertThat(service.save(dto(TypeMagasin.DEPOT, 1)).getCommandeEnPropre()).isFalse();
        }

        /**
         * L'écran des dépôts n'envoie pas ce champ. S'il écrasait la valeur enregistrée, chaque
         * modification d'un dépôt le rendrait commandeur — silencieusement.
         */
        @Test
        @DisplayName("un écran qui ignore le champ n'écrase pas la valeur enregistrée")
        void absenceNEcrasePas() {
            Magasin existant = magasin(5, TypeMagasin.DEPOT).setCommandeEnPropre(false);
            existant.setParent(magasin(1, TypeMagasin.OFFICINE));
            when(magasinRepository.findById(5)).thenReturn(Optional.of(existant));
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));

            MagasinDTO dto = dto(TypeMagasin.DEPOT, 1);
            dto.setId(5);
            dto.setCommandeEnPropre(null);

            assertThat(service.save(dto).getCommandeEnPropre()).isFalse();
        }
    }

    @Nested
    @DisplayName("résoudre l'officine racine")
    class ResoudreLaRacine {

        @Test
        @DisplayName("depuis un dépôt, on remonte à son officine")
        void depuisUnDepot() {
            Magasin officine = magasin(1, TypeMagasin.OFFICINE);
            Magasin depot = magasin(5, TypeMagasin.DEPOT).setParent(officine);
            when(magasinRepository.findById(5)).thenReturn(Optional.of(depot));

            assertThat(service.resoudreOfficineRacine(5).getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("depuis l'officine, c'est elle-même")
        void depuisLOfficine() {
            when(magasinRepository.findById(1)).thenReturn(Optional.of(magasin(1, TypeMagasin.OFFICINE)));

            assertThat(service.resoudreOfficineRacine(1).getId()).isEqualTo(1);
        }

        /**
         * Un parent mal posé à la main ne doit pas faire tourner le serveur indéfiniment : la
         * remontée est bornée plutôt que supposée acyclique.
         */
        @Test
        @DisplayName("un cycle posé à la main ne fait pas tourner la remontée")
        void cycleBorne() {
            Magasin a = magasin(1, TypeMagasin.OFFICINE);
            Magasin b = magasin(2, TypeMagasin.DEPOT);
            a.setParent(b);
            b.setParent(a);
            when(magasinRepository.findById(1)).thenReturn(Optional.of(a));

            assertThatCode(() -> service.resoudreOfficineRacine(1)).doesNotThrowAnyException();
        }
    }

    // ===== jeu d'essai =====

    private MagasinDTO dto(TypeMagasin type, Integer parentId) {
        MagasinDTO dto = new MagasinDTO();
        dto.setTypeMagasin(type);
        dto.setParentId(parentId);
        dto.setName("MAGASIN");
        dto.setFullName("MAGASIN COMPLET");
        return dto;
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
