package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirFournisseur;
import com.kobe.warehouse.domain.AvoirFournisseurLine;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.enumeration.AvoirFournisseurStatut;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AvoirEncoursFournisseurDTO;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand.AvoirLigneCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvoirFournisseurServiceImpl")
class AvoirFournisseurServiceImplTest {

    @Mock
    private AvoirFournisseurRepository avoirFournisseurRepository;

    @Mock
    private RetourBonRepository retourBonRepository;

    @Mock
    private RetourBonItemRepository retourBonItemRepository;

    @Mock
    private UserService userService;

    private AvoirFournisseurServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AvoirFournisseurServiceImpl(
            avoirFournisseurRepository,
            retourBonRepository,
            retourBonItemRepository,
            userService
        );
    }

    private static Fournisseur fournisseur(int id, String libelle) {
        Fournisseur f = new Fournisseur();
        f.setId(id);
        f.setLibelle(libelle);
        return f;
    }

    private static RetourBon retourBon(RetourStatut statut) {
        return new RetourBon().setId(31).setStatut(statut).setReference("RET-0031");
    }

    private static RetourBonItem item(int id, int qtyMvt, Integer prixAchat) {
        RetourBonItem item = new RetourBonItem();
        item.setId(id);
        item.setQtyMvt(qtyMvt);
        item.setPrixAchat(prixAchat);
        return item;
    }

    private static AvoirFournisseur avoir(AvoirFournisseurStatut statut) {
        return new AvoirFournisseur()
            .setId(5)
            .setStatut(statut)
            .setFournisseur(fournisseur(2, "LABOREX"))
            .setRetourBon(retourBon(RetourStatut.CLOSED));
    }

    /** Rejoue l'attribution d'identifiant faite par la base au premier save. */
    private void saveAssignsId(int id) {
        when(avoirFournisseurRepository.save(any(AvoirFournisseur.class))).thenAnswer(inv -> {
            AvoirFournisseur a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(id);
            }
            return a;
        });
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("leve une erreur quand le retour est introuvable")
        void retourIntrouvable() {
            AvoirFournisseurCommand command = new AvoirFournisseurCommand(99, null, List.of());
            when(retourBonRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("RetourBon non trouvé: 99");
        }

        @ParameterizedTest(name = "un retour {0} est refuse")
        @EnumSource(value = RetourStatut.class, names = { "CLOSED", "PARTIALLY_ACCEPTED" })
        void retourDejaTraite(RetourStatut statut) {
            RetourBon bon = retourBon(statut);
            AvoirFournisseurCommand command = new AvoirFournisseurCommand(31, null, List.of());
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));

            assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce retour est déjà traité");

            verify(avoirFournisseurRepository, never()).save(any());
        }

        @ParameterizedTest(name = "un retour {0} est accepte")
        @EnumSource(value = RetourStatut.class, names = { "VALIDATED", "PROCESSING" })
        void retourTraitable(RetourStatut statut) {
            RetourBon bon = retourBon(statut);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonRepository.findById(31)).thenReturn(Optional.of(bon));
            saveAssignsId(5);

            AvoirFournisseurDTO dto = service.create(new AvoirFournisseurCommand(31, "RAS", List.of()));

            assertThat(dto.getId()).isEqualTo(5);
            assertThat(dto.getCommentaire()).isEqualTo("RAS");
        }
    }

    @Nested
    @DisplayName("createFromRetourBon")
    class CreateFromRetourBon {

        @Test
        @DisplayName("prend le fournisseur de la commande en priorite")
        void fournisseurDeLaCommande() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            Commande commande = new Commande();
            commande.setFournisseur(fournisseur(7, "COPHARMED"));
            bon.setCommande(commande);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            saveAssignsId(5);

            AvoirFournisseurDTO dto = service.createFromRetourBon(bon, null, null);

            assertThat(dto.getFournisseurId()).isEqualTo(7);
            assertThat(dto.getFournisseurLibelle()).isEqualTo("COPHARMED");
        }

        @Test
        @DisplayName("retombe sur le fournisseur du retour quand la commande n en porte pas")
        void fournisseurDuRetour() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setCommande(new Commande());
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            saveAssignsId(5);

            assertThat(service.createFromRetourBon(bon, null, null).getFournisseurId()).isEqualTo(2);
        }

        @Test
        @DisplayName("retombe sur le fournisseur du retour quand il n y a pas de commande")
        void fournisseurSansCommande() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            saveAssignsId(5);

            assertThat(service.createFromRetourBon(bon, null, null).getFournisseurId()).isEqualTo(2);
        }

        @Test
        @DisplayName("refuse un retour sans aucun fournisseur")
        void aucunFournisseur() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);

            assertThatThrownBy(() -> service.createFromRetourBon(bon, null, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Aucun fournisseur associé au retour");
        }

        @Test
        @DisplayName("initialise l avoir en attente, date et utilisateur courant")
        void initialiseLAvoir() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            AppUser user = new AppUser();
            when(userService.getUser()).thenReturn(user);
            saveAssignsId(5);

            LocalDateTime before = LocalDateTime.now();
            service.createFromRetourBon(bon, null, "commentaire");

            ArgumentCaptor<AvoirFournisseur> captor = ArgumentCaptor.forClass(AvoirFournisseur.class);
            verify(avoirFournisseurRepository, times(2)).save(captor.capture());
            AvoirFournisseur saved = captor.getValue();
            assertThat(saved.getStatut()).isEqualTo(AvoirFournisseurStatut.EN_ATTENTE);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getRetourBon()).isSameAs(bon);
            assertThat(saved.getDateMtv()).isAfterOrEqualTo(before);
            assertThat(saved.getCommentaire()).isEqualTo("commentaire");
        }

        @Test
        @DisplayName("attribue une reference annuelle sur quatre chiffres apres le premier save")
        void referenceAnnuelle() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            saveAssignsId(42);

            AvoirFournisseurDTO dto = service.createFromRetourBon(bon, null, null);

            assertThat(dto.getReference()).isEqualTo("AV-" + Year.now().getValue() + "-0042");
            verify(avoirFournisseurRepository, times(2)).save(any(AvoirFournisseur.class));
        }

        @Test
        @DisplayName("montant nul et retour cloture quand aucune ligne n est soumise")
        void sansLigne() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            saveAssignsId(5);

            AvoirFournisseurDTO dto = service.createFromRetourBon(bon, List.of(), null);

            assertThat(dto.getMontant()).isZero();
            assertThat(bon.getStatut()).isEqualTo(RetourStatut.CLOSED);
            verify(retourBonRepository).save(bon);
        }

        @Test
        @DisplayName("valorise l avoir au prix d achat multiplie par la quantite acceptee")
        void valorisation() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(item(1, 10, 500)));
            when(retourBonItemRepository.findById(2)).thenReturn(Optional.of(item(2, 4, 250)));
            saveAssignsId(5);

            AvoirFournisseurDTO dto = service.createFromRetourBon(
                bon,
                List.of(new AvoirLigneCommand(1, 10, null), new AvoirLigneCommand(2, 4, null)),
                null
            );

            assertThat(dto.getMontant()).isEqualTo(10 * 500 + 4 * 250);
        }

        @Test
        @DisplayName("le prix de la commande prime sur celui du retour")
        void prixSurcharge() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(item(1, 10, 500)));
            saveAssignsId(5);

            AvoirFournisseurDTO dto = service.createFromRetourBon(bon, List.of(new AvoirLigneCommand(1, 10, 300)), null);

            assertThat(dto.getMontant()).isEqualTo(3000);
        }

        @Test
        @DisplayName("reporte la quantite acceptee sur la ligne de retour")
        void reporteLaQuantiteAcceptee() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            RetourBonItem retourItem = item(1, 10, 500);
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(retourItem));
            saveAssignsId(5);

            service.createFromRetourBon(bon, List.of(new AvoirLigneCommand(1, 6, null)), null);

            assertThat(retourItem.getAcceptedQty()).isEqualTo(6);
            verify(retourBonItemRepository).save(retourItem);
        }

        @Test
        @DisplayName("cloture le retour quand toutes les quantites sont acceptees")
        void clotureQuandToutAccepte() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(item(1, 10, 500)));
            saveAssignsId(5);

            service.createFromRetourBon(bon, List.of(new AvoirLigneCommand(1, 10, null)), null);

            assertThat(bon.getStatut()).isEqualTo(RetourStatut.CLOSED);
        }

        @Test
        @DisplayName("bascule en acceptation partielle des qu une ligne est ecretee")
        void acceptationPartielle() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(item(1, 10, 500)));
            when(retourBonItemRepository.findById(2)).thenReturn(Optional.of(item(2, 4, 250)));
            saveAssignsId(5);

            service.createFromRetourBon(
                bon,
                List.of(new AvoirLigneCommand(1, 10, null), new AvoirLigneCommand(2, 3, null)),
                null
            );

            assertThat(bon.getStatut()).isEqualTo(RetourStatut.PARTIALLY_ACCEPTED);
        }

        @Test
        @DisplayName("leve une erreur quand une ligne de retour est introuvable")
        void ligneDeRetourIntrouvable() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            when(retourBonItemRepository.findById(404)).thenReturn(Optional.empty());
            List<AvoirLigneCommand> lignes = List.of(new AvoirLigneCommand(404, 1, null));

            assertThatThrownBy(() -> service.createFromRetourBon(bon, lignes, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("RetourBonItem non trouvé: 404");
        }

        @Test
        @DisplayName("rattache chaque ligne d avoir a son avoir et a sa ligne de retour")
        void rattacheLesLignes() {
            RetourBon bon = retourBon(RetourStatut.VALIDATED);
            bon.setFournisseur(fournisseur(2, "LABOREX"));
            RetourBonItem retourItem = item(1, 10, 500);
            when(retourBonItemRepository.findById(1)).thenReturn(Optional.of(retourItem));
            saveAssignsId(5);

            service.createFromRetourBon(bon, List.of(new AvoirLigneCommand(1, 10, null)), null);

            ArgumentCaptor<AvoirFournisseur> captor = ArgumentCaptor.forClass(AvoirFournisseur.class);
            verify(avoirFournisseurRepository, times(2)).save(captor.capture());
            List<AvoirFournisseurLine> lignes = captor.getValue().getLignes();
            assertThat(lignes).hasSize(1);
            assertThat(lignes.getFirst().getRetourBonItem()).isSameAs(retourItem);
            assertThat(lignes.getFirst().getAvoirFournisseur()).isSameAs(captor.getValue());
            assertThat(lignes.getFirst().getQtyMvt()).isEqualTo(10);
            assertThat(lignes.getFirst().getPrixAchat()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        private final Pageable pageable = PageRequest.of(1, 15, Sort.by("reference"));

        @Test
        @DisplayName("sans filtre : liste tout, trie par date de mouvement descendante")
        void sansFiltre() {
            when(avoirFournisseurRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(avoir(AvoirFournisseurStatut.EN_ATTENTE))));

            Page<AvoirFournisseurDTO> result = service.findAll(null, null, null, null, null, pageable);

            assertThat(result).hasSize(1);
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(avoirFournisseurRepository).findAll(captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(captor.getValue().getPageSize()).isEqualTo(15);
            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "dateMtv"));
            verify(avoirFournisseurRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @ParameterizedTest(name = "une reference [{0}] vide ne declenche pas la specification")
        @ValueSource(strings = { "", "   " })
        void referenceBlancheNestPasUnFiltre(String reference) {
            when(avoirFournisseurRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            service.findAll(reference, null, null, null, null, pageable);

            verify(avoirFournisseurRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("filtre sur la reference via la specification")
        void filtreSurReference() {
            stubSpecification();

            service.findAll("AV-2026", null, null, null, null, pageable);

            verify(avoirFournisseurRepository).buildSpecification(null, "AV-2026", null, null, null);
        }

        @Test
        @DisplayName("filtre sur le statut via la specification")
        void filtreSurStatut() {
            stubSpecification();

            service.findAll(null, AvoirFournisseurStatut.IMPUTE, null, null, null, pageable);

            verify(avoirFournisseurRepository).buildSpecification(AvoirFournisseurStatut.IMPUTE, null, null, null, null);
        }

        @Test
        @DisplayName("filtre sur le fournisseur via la specification")
        void filtreSurFournisseur() {
            stubSpecification();

            service.findAll(null, null, 3, null, null, pageable);

            verify(avoirFournisseurRepository).buildSpecification(null, null, 3, null, null);
        }

        @Test
        @DisplayName("borne la periode du debut de journee a la fin de journee")
        void bornesDePeriode() {
            stubSpecification();
            LocalDate from = LocalDate.of(2026, 2, 1);
            LocalDate to = LocalDate.of(2026, 2, 28);

            service.findAll(null, null, null, from, to, pageable);

            verify(avoirFournisseurRepository)
                .buildSpecification(null, null, null, from.atStartOfDay(), to.atTime(LocalTime.MAX));
        }

        @Test
        @DisplayName("une seule borne de periode suffit a activer la specification")
        void borneDeDebutSeule() {
            stubSpecification();
            LocalDate from = LocalDate.of(2026, 2, 1);

            service.findAll(null, null, null, from, null, pageable);

            verify(avoirFournisseurRepository).buildSpecification(null, null, null, from.atStartOfDay(), null);
        }

        @Test
        @DisplayName("une borne de fin seule suffit a activer la specification")
        void borneDeFinSeule() {
            stubSpecification();
            LocalDate to = LocalDate.of(2026, 2, 28);

            service.findAll(null, null, null, null, to, pageable);

            verify(avoirFournisseurRepository).buildSpecification(null, null, null, null, to.atTime(LocalTime.MAX));
        }

        @SuppressWarnings("unchecked")
        private void stubSpecification() {
            Specification<com.kobe.warehouse.domain.AvoirFournisseur> spec = (root, query, cb) -> null;
            when(avoirFournisseurRepository.buildSpecification(any(), any(), any(), any(), any())).thenReturn(spec);
            when(avoirFournisseurRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        }
    }

    @Nested
    @DisplayName("getEncoursParFournisseur")
    class GetEncoursParFournisseur {

        @Test
        @DisplayName("agrege uniquement les avoirs en attente")
        void agregeLesEnAttente() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[] { 2, "LABOREX", 125000L });
            rows.add(new Object[] { 7, "COPHARMED", 4200 });
            when(avoirFournisseurRepository.sumParFournisseur(AvoirFournisseurStatut.EN_ATTENTE)).thenReturn(rows);

            List<AvoirEncoursFournisseurDTO> result = service.getEncoursParFournisseur();

            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getFournisseurId()).isEqualTo(2);
            assertThat(result.getFirst().getFournisseurLibelle()).isEqualTo("LABOREX");
            assertThat(result.getFirst().getMontantEncours()).isEqualTo(125000L);
            assertThat(result.get(1).getMontantEncours()).isEqualTo(4200L);
        }

        @Test
        @DisplayName("renvoie une liste vide quand rien n est en attente")
        void aucunEncours() {
            when(avoirFournisseurRepository.sumParFournisseur(AvoirFournisseurStatut.EN_ATTENTE)).thenReturn(List.of());

            assertThat(service.getEncoursParFournisseur()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateStatut")
    class UpdateStatut {

        @Test
        @DisplayName("leve une erreur quand l avoir est introuvable")
        void avoirIntrouvable() {
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatut(1, AvoirFournisseurStatut.IMPUTE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Avoir fournisseur non trouvé");
        }

        @Test
        @DisplayName("refuse de modifier un avoir annule")
        void avoirAnnule() {
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(avoir(AvoirFournisseurStatut.ANNULE)));

            assertThatThrownBy(() -> service.updateStatut(1, AvoirFournisseurStatut.IMPUTE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Impossible de modifier un avoir annulé");

            verify(avoirFournisseurRepository, never()).save(any());
        }

        @ParameterizedTest(name = "bascule vers {0}")
        @EnumSource(value = AvoirFournisseurStatut.class, names = { "REMBOURSE", "IMPUTE", "ANNULE", "EN_ATTENTE" })
        void changeLeStatut(AvoirFournisseurStatut cible) {
            AvoirFournisseur existing = avoir(AvoirFournisseurStatut.EN_ATTENTE);
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(existing));
            when(avoirFournisseurRepository.save(existing)).thenReturn(existing);

            AvoirFournisseurDTO dto = service.updateStatut(1, cible);

            assertThat(existing.getStatut()).isEqualTo(cible);
            assertThat(dto.getStatut()).isEqualTo(cible);
        }
    }

    @Nested
    @DisplayName("annuler")
    class Annuler {

        @Test
        @DisplayName("leve une erreur quand l avoir est introuvable")
        void avoirIntrouvable() {
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.annuler(1, "erreur de saisie"))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Avoir fournisseur non trouvé");
        }

        @Test
        @DisplayName("refuse d annuler un avoir deja impute")
        void avoirImpute() {
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(avoir(AvoirFournisseurStatut.IMPUTE)));

            assertThatThrownBy(() -> service.annuler(1, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Impossible d'annuler un avoir déjà imputé");
        }

        @Test
        @DisplayName("refuse d annuler deux fois")
        void avoirDejaAnnule() {
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(avoir(AvoirFournisseurStatut.ANNULE)));

            assertThatThrownBy(() -> service.annuler(1, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("déjà annulé");
        }

        @Test
        @DisplayName("annule et enregistre le motif")
        void annuleAvecMotif() {
            AvoirFournisseur existing = avoir(AvoirFournisseurStatut.EN_ATTENTE);
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(existing));
            when(avoirFournisseurRepository.save(existing)).thenReturn(existing);

            AvoirFournisseurDTO dto = service.annuler(1, "erreur de saisie");

            assertThat(dto.getStatut()).isEqualTo(AvoirFournisseurStatut.ANNULE);
            assertThat(dto.getCommentaire()).isEqualTo("erreur de saisie");
        }

        @ParameterizedTest(name = "un motif [{0}] laisse le commentaire intact")
        @ValueSource(strings = { "", "   " })
        void motifBlancIgnore(String motif) {
            AvoirFournisseur existing = avoir(AvoirFournisseurStatut.EN_ATTENTE).setCommentaire("initial");
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(existing));
            when(avoirFournisseurRepository.save(existing)).thenReturn(existing);

            assertThat(service.annuler(1, motif).getCommentaire()).isEqualTo("initial");
        }

        @Test
        @DisplayName("un motif nul laisse le commentaire intact")
        void motifNulIgnore() {
            AvoirFournisseur existing = avoir(AvoirFournisseurStatut.EN_ATTENTE).setCommentaire("initial");
            when(avoirFournisseurRepository.findById(1)).thenReturn(Optional.of(existing));
            when(avoirFournisseurRepository.save(existing)).thenReturn(existing);

            assertThat(service.annuler(1, null).getCommentaire()).isEqualTo("initial");
        }
    }

    @Nested
    @DisplayName("countEnAttente")
    class CountEnAttente {

        @Test
        @DisplayName("compte les avoirs en attente")
        void compte() {
            when(avoirFournisseurRepository.countByStatut(AvoirFournisseurStatut.EN_ATTENTE)).thenReturn(4L);

            assertThat(service.countEnAttente()).isEqualTo(4L);
        }
    }
}
