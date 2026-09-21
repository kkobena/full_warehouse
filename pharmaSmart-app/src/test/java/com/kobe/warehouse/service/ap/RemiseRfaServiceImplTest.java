package com.kobe.warehouse.service.ap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.dto.AvoirFournisseurRfaDTO;
import com.kobe.warehouse.service.dto.RemiseRfaFournisseurDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * La remise de fin d'année est un accord commercial : le grossiste ristourne un pourcentage du
 * chiffre d'affaires dès qu'un palier annuel est franchi. L'écran sert à savoir où l'on en est —
 * et, s'il est encore temps, à décider d'accélérer les commandes avant le 31 décembre.
 *
 * <p>Tout l'intérêt est donc dans <b>l'alerte</b>, qui est un conseil d'action, pas un constat. Elle
 * ne se déclenche qu'entre certaines bornes : inutile d'alerter une officine qui a déjà franchi son
 * palier, inutile aussi de la relancer sur un fournisseur qui n'a pas de palier négocié. Un seuil
 * mal placé produirait soit un écran muet quand il fallait agir, soit un bruit qu'on cesse de lire.
 */
@DisplayName("RemiseRfaService — remises de fin d'année")
class RemiseRfaServiceImplTest {

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final CommandeRepository commandeRepository = mock(CommandeRepository.class);
    private final AvoirFournisseurRepository avoirRepository = mock(AvoirFournisseurRepository.class);

    private final RemiseRfaService service = new RemiseRfaServiceImpl(commandeRepository, avoirRepository);

    @BeforeEach
    void aucunAccord() {
        when(commandeRepository.sumCaByFournisseur(any(), any(), any())).thenReturn(List.of());
        when(avoirRepository.sumParFournisseur(any())).thenReturn(List.of());
        when(avoirRepository.findAllWithFournisseur()).thenReturn(List.of());
    }

    // ===== exercice analysé =====

    @Nested
    @DisplayName("Exercice analysé")
    class ExerciceAnalyse {

        /** La remise est annuelle : elle se compte sur l'année civile en cours, du 1er janvier. */
        @Test
        @DisplayName("l'exercice court du premier janvier au premier janvier suivant")
        void exerciceCivil() {
            service.getRfaFournisseurs();

            ArgumentCaptor<LocalDate> debut = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> fin = ArgumentCaptor.forClass(LocalDate.class);
            verify(commandeRepository).sumCaByFournisseur(any(), debut.capture(), fin.capture());

            int annee = LocalDate.now().getYear();
            assertThat(debut.getValue()).isEqualTo(LocalDate.of(annee, 1, 1));
            assertThat(fin.getValue()).isEqualTo(LocalDate.of(annee + 1, 1, 1));
        }

        /** Seules les commandes clôturées comptent : une réception en cours n'est pas un achat ferme. */
        @Test
        @DisplayName("seules les commandes clôturées alimentent le chiffre d'affaires")
        void commandesCloturees() {
            service.getRfaFournisseurs();

            verify(commandeRepository).sumCaByFournisseur(org.mockito.ArgumentMatchers.eq(OrderStatut.CLOSED), any(), any());
        }

        /** Seuls les avoirs remboursés sont des remises acquises : un avoir en attente ne l'est pas. */
        @Test
        @DisplayName("seuls les avoirs remboursés comptent comme remise acquise")
        void avoirsRembourses() {
            service.getRfaFournisseurs();

            verify(avoirRepository).sumParFournisseur(AvoirFournisseurStatut.REMBOURSE);
        }

        @Test
        @DisplayName("aucun fournisseur sous accord rend une liste vide")
        void aucunFournisseur() {
            assertThat(service.getRfaFournisseurs()).isEmpty();
        }
    }

    // ===== avancement =====

    @Nested
    @DisplayName("Avancement vers le palier")
    class AvancementVersLePalier {

        @Test
        @DisplayName("chaque fournisseur porte son palier, son chiffre et son avancement")
        void contenuDeLaLigne() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 100_000_000L, 2));

            RemiseRfaFournisseurDTO ligne = service.getRfaFournisseurs().getFirst();

            assertThat(ligne.fournisseurId()).isEqualTo(3);
            assertThat(ligne.fournisseurName()).isEqualTo("LABOREX");
            assertThat(ligne.palierRfa()).isEqualTo(100_000_000L);
            assertThat(ligne.caCommandeN()).isEqualTo(60_000_000L);
            assertThat(ligne.pourcentageAtteint()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("la remise estimée applique le taux négocié au chiffre réalisé")
        void remiseEstimee() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 100_000_000L, 2));

            // Deux pour cent de soixante millions.
            assertThat(service.getRfaFournisseurs().getFirst().rfaEstimee()).isEqualTo(1_200_000L);
        }

        @Test
        @DisplayName("sans taux négocié, aucune remise n'est estimée")
        void sansTaux() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 100_000_000L, null));

            assertThat(service.getRfaFournisseurs().getFirst().rfaEstimee()).isZero();
        }

        /** Sans palier négocié, il n'y a pas d'avancement à mesurer. */
        @Test
        @DisplayName("sans palier négocié, l'avancement reste nul")
        void sansPalier() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, null, 2));

            RemiseRfaFournisseurDTO ligne = service.getRfaFournisseurs().getFirst();

            assertThat(ligne.palierRfa()).isNull();
            assertThat(ligne.pourcentageAtteint()).isZero();
        }

        @Test
        @DisplayName("un palier à zéro ne fait pas diviser par zéro")
        void palierNul() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 0L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().pourcentageAtteint()).isZero();
        }

        @Test
        @DisplayName("un chiffre d'affaires non renseigné vaut zéro")
        void chiffreAbsent() {
            donneChiffres(new Object[] { 3, "LABOREX", null, 100_000_000L, 2 });

            assertThat(service.getRfaFournisseurs().getFirst().caCommandeN()).isZero();
        }

        @Test
        @DisplayName("la remise déjà remboursée est rapprochée du fournisseur")
        void remiseDejaRecue() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 100_000_000L, 2));
            when(avoirRepository.sumParFournisseur(any())).thenReturn(
                List.<Object[]>of(new Object[] { 3, "LABOREX", 800_000L })
            );

            assertThat(service.getRfaFournisseurs().getFirst().rfaRecue()).isEqualTo(800_000L);
        }

        @Test
        @DisplayName("un fournisseur sans avoir remboursé n'a rien reçu")
        void aucuneRemiseRecue() {
            donneChiffres(fournisseur(3, "LABOREX", 60_000_000L, 100_000_000L, 2));
            when(avoirRepository.sumParFournisseur(any())).thenReturn(
                List.<Object[]>of(new Object[] { 9, "AUTRE", 800_000L })
            );

            assertThat(service.getRfaFournisseurs().getFirst().rfaRecue()).isZero();
        }
    }

    // ===== alerte =====

    @Nested
    @DisplayName("Alerte d'avancement")
    class AlerteDAvancement {

        /** Au-delà de quatre-vingts pour cent, le palier est à portée : c'est le moment d'accélérer. */
        @Test
        @DisplayName("à partir de quatre-vingts pour cent, l'écran invite à accélérer")
        void palierAPortee() {
            donneChiffres(fournisseur(3, "LABOREX", 85_000_000L, 100_000_000L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte())
                .contains("85%")
                .contains("accélérer les commandes");
        }

        /** Le palier franchi, il n'y a plus rien à décider : l'alerte disparaît. */
        @Test
        @DisplayName("le palier franchi, il n'y a plus d'alerte")
        void palierFranchi() {
            donneChiffres(fournisseur(3, "LABOREX", 100_000_000L, 100_000_000L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();
        }

        @Test
        @DisplayName("un palier largement dépassé n'alerte pas davantage")
        void palierDepasse() {
            donneChiffres(fournisseur(3, "LABOREX", 250_000_000L, 100_000_000L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();
        }

        /** En deçà de la moitié, le constat remplace le conseil : le palier est hors d'atteinte. */
        @Test
        @DisplayName("en deçà de la moitié, l'écran constate le retard")
        void palierLoin() {
            donneChiffres(fournisseur(3, "LABOREX", 30_000_000L, 100_000_000L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).contains("30%").contains("Seulement");
        }

        @Test
        @DisplayName("entre la moitié et quatre-vingts pour cent, rien à signaler")
        void avancementOrdinaire() {
            donneChiffres(fournisseur(3, "LABOREX", 65_000_000L, 100_000_000L, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();
        }

        @Test
        @DisplayName("les bornes de l'alerte appartiennent au message le plus pressant")
        void bornesDesSeuils() {
            donneChiffres(fournisseur(3, "LABOREX", 80_000_000L, 100_000_000L, 2));
            assertThat(service.getRfaFournisseurs().getFirst().alerte()).contains("accélérer");

            donneChiffres(fournisseur(3, "LABOREX", 79_000_000L, 100_000_000L, 2));
            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();

            donneChiffres(fournisseur(3, "LABOREX", 49_000_000L, 100_000_000L, 2));
            assertThat(service.getRfaFournisseurs().getFirst().alerte()).contains("Seulement");
        }

        /** Sans palier, il n'y a rien à atteindre : pas d'alerte, même à chiffre nul. */
        @Test
        @DisplayName("un fournisseur sans palier n'est jamais alerté")
        void sansPalierAucuneAlerte() {
            donneChiffres(fournisseur(3, "LABOREX", 0L, null, 2));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();
        }

        /** Sans taux négocié, le constat de retard n'a pas d'objet : il n'y a rien à gagner. */
        @Test
        @DisplayName("sans taux négocié, le retard n'est pas signalé")
        void sansTauxAucunConstat() {
            donneChiffres(fournisseur(3, "LABOREX", 30_000_000L, 100_000_000L, null));

            assertThat(service.getRfaFournisseurs().getFirst().alerte()).isNull();
        }
    }

    // ===== avoirs =====

    @Nested
    @DisplayName("Avoirs fournisseurs")
    class AvoirsFournisseurs {

        @Test
        @DisplayName("chaque avoir porte son fournisseur, sa référence, sa date et son statut")
        void contenuDeLAvoir() {
            when(avoirRepository.findAllWithFournisseur()).thenReturn(
                List.of(avoir(12, "LABOREX", "AV-2026-001", LocalDate.of(2026, 3, 15), 800_000, AvoirFournisseurStatut.REMBOURSE))
            );

            AvoirFournisseurRfaDTO dto = service.getAvoirsFournisseurs().getFirst();

            assertThat(dto.id()).isEqualTo(12);
            assertThat(dto.fournisseurName()).isEqualTo("LABOREX");
            assertThat(dto.numAvoir()).isEqualTo("AV-2026-001");
            assertThat(dto.dateAvoir()).isEqualTo("2026-03-15");
            assertThat(dto.montant()).isEqualTo(800_000L);
            assertThat(dto.statut()).isEqualTo("REMBOURSE");
        }

        @Test
        @DisplayName("aucun avoir rend une liste vide")
        void aucunAvoir() {
            assertThat(service.getAvoirsFournisseurs()).isEmpty();
        }
    }

    // ===== utilitaires =====

    private void donneChiffres(Object[]... lignes) {
        when(commandeRepository.sumCaByFournisseur(any(), any(), any())).thenReturn(List.of(lignes));
    }

    private static Object[] fournisseur(Integer id, String nom, Long chiffreAffaires, Long palier, Integer tauxPct) {
        return new Object[] { id, nom, chiffreAffaires, palier, tauxPct };
    }

    private static AvoirFournisseur avoir(
        int id,
        String nomFournisseur,
        String reference,
        LocalDate date,
        int montant,
        AvoirFournisseurStatut statut
    ) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setLibelle(nomFournisseur);

        AvoirFournisseur avoir = new AvoirFournisseur();
        ReflectionTestUtils.setField(avoir, "id", id);
        avoir.setFournisseur(fournisseur);
        avoir.setReference(reference);
        avoir.setDateMtv(date.atTime(10, 0));
        avoir.setMontant(montant);
        avoir.setStatut(statut);
        return avoir;
    }
}
