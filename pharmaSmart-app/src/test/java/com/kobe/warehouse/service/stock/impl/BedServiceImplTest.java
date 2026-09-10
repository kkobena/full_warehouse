package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.BedDTO;
import com.kobe.warehouse.service.dto.BedImportLigneDTO;
import com.kobe.warehouse.service.dto.BedLigneDTO;
import com.kobe.warehouse.service.dto.BedSummaryDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BedServiceImpl")
class BedServiceImplTest {

    private static final int BED_ID = 12;
    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 18);

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private LogsService logsService;

    @Mock
    private ProduitService produitService;

    @Mock
    private CommandeIdGeneratorService commandeIdGeneratorService;

    @Mock
    private OrderLineIdGeneratorService orderLineIdGeneratorService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    private BedServiceImpl service;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        service = new BedServiceImpl(
            commandeRepository,
            orderLineRepository,
            fournisseurProduitRepository,
            fournisseurRepository,
            storageService,
            logsService,
            produitService,
            commandeIdGeneratorService,
            orderLineIdGeneratorService,
            referenceService,
            inventoryTransactionService
        );
        currentUser = new AppUser();
        when(storageService.getUser()).thenReturn(currentUser);
        when(commandeIdGeneratorService.getNextIdAsInt()).thenReturn(BED_ID);
        when(orderLineIdGeneratorService.getNextIdAsInt()).thenReturn(900);
        when(referenceService.buildNumCommande()).thenReturn("CMD-0001");
        when(commandeRepository.countByTypeAndOrderDate(any(), any())).thenReturn(0L);
    }

    private static Fournisseur fournisseur(int id) {
        Fournisseur f = new Fournisseur();
        f.setId(id);
        f.setLibelle("LABOREX");
        return f;
    }

    private static FournisseurProduit fournisseurProduit(int prixUni) {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip("1234567");
        fp.setPrixUni(prixUni);
        fp.setProduit(produit);
        return fp;
    }

    private static Commande bed(OrderStatut statut) {
        Commande commande = new Commande();
        commande.setId(BED_ID);
        commande.setOrderDate(ORDER_DATE);
        commande.setType(TypeDeliveryReceipt.DIRECT);
        commande.setOrderStatus(statut);
        commande.setGrossAmount(0);
        return commande;
    }

    private static OrderLine orderLine(int id, int quantite, int prixAchat) {
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setOrderDate(ORDER_DATE);
        line.setQuantityRequested(quantite);
        line.setQuantityReceived(quantite);
        line.setOrderCostAmount(prixAchat);
        line.setOrderUnitPrice(prixAchat * 2);
        line.setFournisseurProduit(fournisseurProduit(prixAchat * 2));
        return line;
    }

    private void stubBed(Commande commande) {
        when(commandeRepository.findById(new CommandeId(BED_ID, ORDER_DATE))).thenReturn(Optional.of(commande));
    }

    private static BedLigneDTO ligne(int quantite, int prixAchat, int prixVente) {
        return new BedLigneDTO().setFournisseurProduitId(77).setQuantite(quantite).setPrixAchat(prixAchat).setPrixVente(prixVente);
    }

    private Commande capturerCommande() {
        ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
        verify(commandeRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("createBed")
    class CreateBed {

        @Test
        @DisplayName("cree un BED en brouillon de type DIRECT, a montants nuls")
        void creeUnBrouillon() {
            LocalDateTime before = LocalDateTime.now();

            BedDTO dto = service.createBed(new BedDTO().setMotifBed(MotifBed.RETOUR_CLIENT).setCommentaireBed("retour Mme X"));

            Commande saved = capturerCommande();
            assertThat(saved.getId().getId()).isEqualTo(BED_ID);
            assertThat(saved.getOrderDate()).isEqualTo(LocalDate.now());
            assertThat(saved.getType()).isEqualTo(TypeDeliveryReceipt.DIRECT);
            assertThat(saved.getOrderStatus()).isEqualTo(OrderStatut.REQUESTED);
            assertThat(saved.getMotifBed()).isEqualTo(MotifBed.RETOUR_CLIENT);
            assertThat(saved.getCommentaireBed()).isEqualTo("retour Mme X");
            assertThat(saved.getOrderReference()).isEqualTo("CMD-0001");
            assertThat(saved.getUser()).isSameAs(currentUser);
            assertThat(saved.getGrossAmount()).isZero();
            assertThat(saved.getDiscountAmount()).isZero();
            assertThat(saved.getTaxAmount()).isZero();
            assertThat(saved.getHtAmount()).isZero();
            assertThat(saved.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
            assertThat(dto.getId()).isEqualTo(BED_ID);
        }

        @Test
        @DisplayName("numerote la reference du jour a partir de 001")
        void referenceDuPremierBedDuJour() {
            when(commandeRepository.countByTypeAndOrderDate(TypeDeliveryReceipt.DIRECT, LocalDate.now())).thenReturn(0L);

            service.createBed(new BedDTO());

            String attendu = "BED-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
            assertThat(capturerCommande().getReceiptReference()).isEqualTo(attendu);
        }

        @Test
        @DisplayName("incremente la reference sur les BED suivants du jour")
        void referenceIncrementee() {
            when(commandeRepository.countByTypeAndOrderDate(TypeDeliveryReceipt.DIRECT, LocalDate.now())).thenReturn(41L);

            service.createBed(new BedDTO());

            assertThat(capturerCommande().getReceiptReference()).endsWith("-042");
        }

        @Test
        @DisplayName("rattache le fournisseur quand il est fourni")
        void avecFournisseur() {
            when(fournisseurRepository.getReferenceById(3)).thenReturn(fournisseur(3));

            BedDTO dto = service.createBed(new BedDTO().setFournisseurId(3));

            assertThat(capturerCommande().getFournisseur()).isNotNull();
            assertThat(dto.getFournisseurId()).isEqualTo(3);
            assertThat(dto.getFournisseurLibelle()).isEqualTo("LABOREX");
        }

        @Test
        @DisplayName("laisse le fournisseur vide quand il n est pas fourni")
        void sansFournisseur() {
            service.createBed(new BedDTO());

            assertThat(capturerCommande().getFournisseur()).isNull();
            verifyNoInteractions(fournisseurRepository);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("refuse un identifiant inconnu")
        void inconnu() {
            when(commandeRepository.findById(any(CommandeId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(BED_ID, ORDER_DATE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("BED introuvable");
        }

        @Test
        @DisplayName("refuse une commande qui n est pas un BED")
        void pasUnBed() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setType(TypeDeliveryReceipt.ORDER);
            stubBed(commande);

            assertThatThrownBy(() -> service.findById(BED_ID, ORDER_DATE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("BED introuvable");
        }

        @Test
        @DisplayName("mappe le BED et ses lignes")
        void mappeLeBedEtSesLignes() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.getOrderLines().add(orderLine(900, 5, 400));
            stubBed(commande);

            BedDTO dto = service.findById(BED_ID, ORDER_DATE);

            assertThat(dto.getId()).isEqualTo(BED_ID);
            assertThat(dto.getLignes()).hasSize(1);
            BedLigneDTO ligne = dto.getLignes().getFirst();
            assertThat(ligne.getId()).isEqualTo(900);
            assertThat(ligne.getQuantite()).isEqualTo(5);
            assertThat(ligne.getPrixAchat()).isEqualTo(400);
            assertThat(ligne.getCodeCip()).isEqualTo("1234567");
            assertThat(ligne.getProduitLibelle()).isEqualTo("DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("renvoie une liste de lignes vide quand le BED est vierge")
        void bedVierge() {
            stubBed(bed(OrderStatut.REQUESTED));

            assertThat(service.findById(BED_ID, ORDER_DATE).getLignes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @SuppressWarnings("unchecked")
        private final Specification<Commande> spec = (root, query, cb) -> null;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void stubSpecifications() {
            when(commandeRepository.byReceiptType(any())).thenReturn(spec);
            when(commandeRepository.byMotifBed(any())).thenReturn(spec);
            when(commandeRepository.hasOrderStatut(any())).thenReturn(spec);
            when(commandeRepository.between(any(LocalDate.class), any(LocalDate.class))).thenReturn(spec);
            when(commandeRepository.byBedSearchRef(any())).thenReturn(spec);
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        }

        @Test
        @DisplayName("ne liste que les receptions directes quand aucun filtre n est pose")
        void sansFiltre() {
            service.findAll(null, null, null, null, null, PageRequest.of(0, 20));

            verify(commandeRepository).byReceiptType(TypeDeliveryReceipt.DIRECT);
            verify(commandeRepository, never()).byMotifBed(any());
            verify(commandeRepository, never()).hasOrderStatut(any());
            verify(commandeRepository, never()).between(any(LocalDate.class), any(LocalDate.class));
            verify(commandeRepository, never()).byBedSearchRef(any());
        }

        @Test
        @DisplayName("filtre sur le motif")
        void filtreSurLeMotif() {
            service.findAll(null, MotifBed.ECHANTILLON, null, null, null, PageRequest.of(0, 20));

            verify(commandeRepository).byMotifBed(MotifBed.ECHANTILLON);
        }

        @Test
        @DisplayName("filtre sur le statut")
        void filtreSurLeStatut() {
            service.findAll(null, null, OrderStatut.CLOSED, null, null, PageRequest.of(0, 20));

            verify(commandeRepository).hasOrderStatut(OrderStatut.CLOSED);
        }

        @Test
        @DisplayName("filtre sur une periode complete")
        void filtreSurPeriodeComplete() {
            LocalDate from = LocalDate.of(2026, 4, 1);
            LocalDate to = LocalDate.of(2026, 4, 30);

            service.findAll(null, null, null, from, to, PageRequest.of(0, 20));

            verify(commandeRepository).between(from, to);
        }

        @Test
        @DisplayName("une date de debut seule borne la periode a aujourd hui")
        void filtreSurDateDeDebutSeule() {
            LocalDate from = LocalDate.of(2026, 4, 1);

            service.findAll(null, null, null, from, null, PageRequest.of(0, 20));

            verify(commandeRepository).between(from, LocalDate.now());
        }

        @Test
        @DisplayName("une date de fin seule n est pas un filtre de periode")
        void dateDeFinSeuleIgnoree() {
            service.findAll(null, null, null, null, LocalDate.of(2026, 4, 30), PageRequest.of(0, 20));

            verify(commandeRepository, never()).between(any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("filtre sur la recherche libre")
        void filtreSurRecherche() {
            service.findAll("BED-2026", null, null, null, null, PageRequest.of(0, 20));

            verify(commandeRepository).byBedSearchRef("BED-2026");
        }

        @ParameterizedTest(name = "une recherche [{0}] n est pas un filtre")
        @ValueSource(strings = { "", "   " })
        void rechercheBlancheIgnoree(String search) {
            service.findAll(search, null, null, null, null, PageRequest.of(0, 20));

            verify(commandeRepository, never()).byBedSearchRef(any());
        }

        @Test
        @DisplayName("mappe chaque BED de la page en resume")
        @SuppressWarnings("unchecked")
        void mappeLaPage() {
            Commande commande = bed(OrderStatut.CLOSED);
            commande.setReceiptReference("BED-20260418-001");
            commande.setMotifBed(MotifBed.ECHANTILLON);
            commande.getOrderLines().add(orderLine(900, 5, 400));
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(commande)));

            Page<BedSummaryDTO> page = service.findAll(null, null, null, null, null, PageRequest.of(0, 20));

            assertThat(page).hasSize(1);
            BedSummaryDTO summary = page.getContent().getFirst();
            assertThat(summary.getReceiptReference()).isEqualTo("BED-20260418-001");
            assertThat(summary.getMotifLabel()).isEqualTo(MotifBed.ECHANTILLON.getLabel());
            assertThat(summary.getLignesCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("addLigne")
    class AddLigne {

        @ParameterizedTest(name = "un BED {0} refuse l ajout")
        @EnumSource(value = OrderStatut.class, names = { "REQUESTED" }, mode = EnumSource.Mode.EXCLUDE)
        void bedNonModifiable(OrderStatut statut) {
            stubBed(bed(statut));
            BedLigneDTO ligne = ligne(5, 400, 800);

            assertThatThrownBy(() -> service.addLigne(BED_ID, ORDER_DATE, ligne))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce BED ne peut plus être modifié");

            verify(orderLineRepository, never()).save(any());
        }

        @Test
        @DisplayName("cree la ligne avec quantite demandee et recue alignees")
        void creeLaLigne() {
            Commande commande = bed(OrderStatut.REQUESTED);
            stubBed(commande);
            when(fournisseurProduitRepository.getReferenceById(77)).thenReturn(fournisseurProduit(900));

            service.addLigne(BED_ID, ORDER_DATE, ligne(5, 400, 800));

            ArgumentCaptor<OrderLine> captor = ArgumentCaptor.forClass(OrderLine.class);
            verify(orderLineRepository).save(captor.capture());
            OrderLine line = captor.getValue();
            assertThat(line.getId().getId()).isEqualTo(900);
            assertThat(line.getOrderDate()).isEqualTo(ORDER_DATE);
            assertThat(line.getCommande()).isSameAs(commande);
            assertThat(line.getQuantityRequested()).isEqualTo(5);
            assertThat(line.getQuantityReceived()).isEqualTo(5);
            assertThat(line.getOrderCostAmount()).isEqualTo(400);
            assertThat(line.getInitStock()).isZero();
            assertThat(line.getFinalStock()).isZero();
            assertThat(line.getDiscountAmount()).isZero();
            assertThat(line.getUpdated()).isFalse();
        }

        @Test
        @DisplayName("privilegie le prix de vente du fournisseur quand il est renseigne")
        void prixDeVenteDuFournisseur() {
            stubBed(bed(OrderStatut.REQUESTED));
            when(fournisseurProduitRepository.getReferenceById(77)).thenReturn(fournisseurProduit(900));

            service.addLigne(BED_ID, ORDER_DATE, ligne(5, 400, 800));

            ArgumentCaptor<OrderLine> captor = ArgumentCaptor.forClass(OrderLine.class);
            verify(orderLineRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderUnitPrice()).isEqualTo(900);
        }

        @Test
        @DisplayName("retombe sur le prix de vente transmis quand le fournisseur n en porte pas")
        void prixDeVenteTransmis() {
            stubBed(bed(OrderStatut.REQUESTED));
            when(fournisseurProduitRepository.getReferenceById(77)).thenReturn(fournisseurProduit(0));

            service.addLigne(BED_ID, ORDER_DATE, ligne(5, 400, 800));

            ArgumentCaptor<OrderLine> captor = ArgumentCaptor.forClass(OrderLine.class);
            verify(orderLineRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderUnitPrice()).isEqualTo(800);
        }

        @Test
        @DisplayName("cumule la ligne dans le montant brut du BED")
        void cumuleLeMontantBrut() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(1000);
            stubBed(commande);
            when(fournisseurProduitRepository.getReferenceById(77)).thenReturn(fournisseurProduit(900));

            service.addLigne(BED_ID, ORDER_DATE, ligne(5, 400, 800));

            assertThat(commande.getGrossAmount()).isEqualTo(1000 + 5 * 400);
            assertThat(commande.getOrderLines()).hasSize(1);
        }

        @Test
        @DisplayName("un montant brut absent est traite comme zero")
        void montantBrutAbsent() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(null);
            stubBed(commande);
            when(fournisseurProduitRepository.getReferenceById(77)).thenReturn(fournisseurProduit(900));

            service.addLigne(BED_ID, ORDER_DATE, ligne(5, 400, 800));

            assertThat(commande.getGrossAmount()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("updateLigne")
    class UpdateLigne {

        @Test
        @DisplayName("refuse un BED qui n est plus en brouillon")
        void bedNonModifiable() {
            stubBed(bed(OrderStatut.CLOSED));
            BedLigneDTO dto = ligne(3, 500, 900);

            assertThatThrownBy(() -> service.updateLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE, dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce BED ne peut plus être modifié");
        }

        @Test
        @DisplayName("remplace la quantite et le prix de la ligne")
        void remplaceLaLigne() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(2000);
            stubBed(commande);
            OrderLine line = orderLine(900, 5, 400);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(line);

            service.updateLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE, ligne(3, 500, 900));

            assertThat(line.getQuantityRequested()).isEqualTo(3);
            assertThat(line.getQuantityReceived()).isEqualTo(3);
            assertThat(line.getOrderCostAmount()).isEqualTo(500);
            verify(orderLineRepository).save(line);
        }

        @Test
        @DisplayName("reprend le montant brut en retirant l ancienne ligne")
        void recalculeLeMontantBrut() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(2000);
            stubBed(commande);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(orderLine(900, 5, 400));

            service.updateLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE, ligne(3, 500, 900));

            assertThat(commande.getGrossAmount()).isEqualTo(2000 - 2000 + 1500);
        }

        @Test
        @DisplayName("le montant brut ne descend jamais sous zero")
        void montantBrutPlancherAZero() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(100);
            stubBed(commande);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(orderLine(900, 5, 400));

            service.updateLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE, ligne(0, 0, 0));

            assertThat(commande.getGrossAmount()).isZero();
        }

        @Test
        @DisplayName("un montant brut absent est traite comme zero")
        void montantBrutAbsent() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(null);
            stubBed(commande);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(orderLine(900, 1, 100));

            service.updateLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE, ligne(2, 300, 600));

            assertThat(commande.getGrossAmount()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("removeLigne")
    class RemoveLigne {

        @Test
        @DisplayName("refuse un BED qui n est plus en brouillon")
        void bedNonModifiable() {
            stubBed(bed(OrderStatut.CLOSED));

            assertThatThrownBy(() -> service.removeLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce BED ne peut plus être modifié");

            verify(orderLineRepository, never()).delete(any());
        }

        @Test
        @DisplayName("supprime la ligne et deduit son montant")
        void supprimeEtDeduit() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(3000);
            stubBed(commande);
            OrderLine line = orderLine(900, 5, 400);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(line);

            service.removeLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE);

            assertThat(commande.getGrossAmount()).isEqualTo(1000);
            verify(orderLineRepository).delete(line);
            verify(commandeRepository).save(commande);
        }

        @Test
        @DisplayName("le montant brut ne descend jamais sous zero")
        void montantBrutPlancherAZero() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(100);
            stubBed(commande);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(orderLine(900, 5, 400));

            service.removeLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE);

            assertThat(commande.getGrossAmount()).isZero();
        }

        @Test
        @DisplayName("un montant brut absent est traite comme zero")
        void montantBrutAbsent() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setGrossAmount(null);
            stubBed(commande);
            when(orderLineRepository.getReferenceById(new OrderLineId(900, ORDER_DATE))).thenReturn(orderLine(900, 1, 100));

            service.removeLigne(BED_ID, ORDER_DATE, 900, ORDER_DATE);

            assertThat(commande.getGrossAmount()).isZero();
        }
    }

    @Nested
    @DisplayName("validateBed")
    class ValidateBed {

        private Commande bedPret() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.setMotifBed(MotifBed.RETOUR_CLIENT);
            commande.setReceiptReference("BED-20260418-001");
            commande.getOrderLines().add(orderLine(900, 5, 400));
            stubBed(commande);
            return commande;
        }

        @Test
        @DisplayName("refuse un BED deja valide")
        void bedDejaValide() {
            stubBed(bed(OrderStatut.CLOSED));

            assertThatThrownBy(() -> service.validateBed(BED_ID, ORDER_DATE, null, null, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce BED est déjà validé ou annulé");
        }

        @Test
        @DisplayName("refuse un BED sans ligne")
        void bedSansLigne() {
            stubBed(bed(OrderStatut.REQUESTED));

            assertThatThrownBy(() -> service.validateBed(BED_ID, ORDER_DATE, MotifBed.AUTRE, null, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Le BED doit avoir au moins une ligne");
        }

        @Test
        @DisplayName("refuse un BED sans motif")
        void bedSansMotif() {
            Commande commande = bed(OrderStatut.REQUESTED);
            commande.getOrderLines().add(orderLine(900, 5, 400));
            stubBed(commande);

            assertThatThrownBy(() -> service.validateBed(BED_ID, ORDER_DATE, null, null, null))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Le motif est obligatoire");
        }

        @Test
        @DisplayName("conserve le motif du BED quand aucun n est transmis")
        void motifConserve() {
            Commande commande = bedPret();

            service.validateBed(BED_ID, ORDER_DATE, null, null, null);

            assertThat(commande.getMotifBed()).isEqualTo(MotifBed.RETOUR_CLIENT);
        }

        @Test
        @DisplayName("le motif transmis prime sur celui du BED")
        void motifTransmisPrime() {
            Commande commande = bedPret();

            service.validateBed(BED_ID, ORDER_DATE, MotifBed.ECHANTILLON, null, null);

            assertThat(commande.getMotifBed()).isEqualTo(MotifBed.ECHANTILLON);
        }

        @Test
        @DisplayName("rattache le fournisseur transmis quand le BED n en a pas")
        void rattacheLeFournisseur() {
            Commande commande = bedPret();
            when(fournisseurRepository.getReferenceById(3)).thenReturn(fournisseur(3));

            service.validateBed(BED_ID, ORDER_DATE, null, 3, null);

            assertThat(commande.getFournisseur()).isNotNull();
        }

        @Test
        @DisplayName("ne remplace pas un fournisseur deja rattache")
        void neRemplacePasLeFournisseur() {
            Commande commande = bedPret();
            Fournisseur existant = fournisseur(9);
            commande.setFournisseur(existant);

            service.validateBed(BED_ID, ORDER_DATE, null, 3, null);

            assertThat(commande.getFournisseur()).isSameAs(existant);
            verify(fournisseurRepository, never()).getReferenceById(anyInt());
        }

        @Test
        @DisplayName("enregistre le commentaire transmis")
        void enregistreLeCommentaire() {
            Commande commande = bedPret();

            service.validateBed(BED_ID, ORDER_DATE, null, null, "controle du 18/04");

            assertThat(commande.getCommentaireBed()).isEqualTo("controle du 18/04");
        }

        @ParameterizedTest(name = "un commentaire [{0}] laisse celui du BED intact")
        @ValueSource(strings = { "", "   " })
        void commentaireBlancIgnore(String commentaire) {
            Commande commande = bedPret();
            commande.setCommentaireBed("initial");

            service.validateBed(BED_ID, ORDER_DATE, null, null, commentaire);

            assertThat(commande.getCommentaireBed()).isEqualTo("initial");
        }

        @Test
        @DisplayName("un commentaire nul laisse celui du BED intact")
        void commentaireNulIgnore() {
            Commande commande = bedPret();
            commande.setCommentaireBed("initial");

            service.validateBed(BED_ID, ORDER_DATE, null, null, null);

            assertThat(commande.getCommentaireBed()).isEqualTo("initial");
        }

        @Test
        @DisplayName("credite le stock et trace les niveaux avant et apres")
        void crediteLeStock() {
            Commande commande = bedPret();
            when(produitService.getProductTotalStock(500)).thenReturn(20);

            service.validateBed(BED_ID, ORDER_DATE, null, null, null);

            OrderLine line = commande.getOrderLines().iterator().next();
            assertThat(line.getInitStock()).isEqualTo(20);
            assertThat(line.getFinalStock()).isEqualTo(25);
            verify(orderLineRepository).save(line);
            verify(produitService).updateTotalStock(any(Produit.class), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(0));
        }

        @Test
        @DisplayName("cloture le BED, le date du jour et journalise l entree en stock")
        void clotureEtJournalise() {
            Commande commande = bedPret();

            service.validateBed(BED_ID, ORDER_DATE, null, null, null);

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
            assertThat(commande.getReceiptDate()).isEqualTo(LocalDate.now());
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.ENTREE_STOCK),
                org.mockito.ArgumentMatchers.eq("bed.entry"),
                any(Object[].class),
                org.mockito.ArgumentMatchers.eq(String.valueOf(BED_ID))
            );
        }
    }

    @Nested
    @DisplayName("deleteBed")
    class DeleteBed {

        @Test
        @DisplayName("supprime un BED en brouillon")
        void supprimeUnBrouillon() {
            Commande commande = bed(OrderStatut.REQUESTED);
            stubBed(commande);

            service.deleteBed(BED_ID, ORDER_DATE);

            verify(commandeRepository).delete(commande);
        }

        @ParameterizedTest(name = "refuse la suppression d un BED {0}")
        @EnumSource(value = OrderStatut.class, names = { "REQUESTED" }, mode = EnumSource.Mode.EXCLUDE)
        void refuseHorsBrouillon(OrderStatut statut) {
            stubBed(bed(statut));

            assertThatThrownBy(() -> service.deleteBed(BED_ID, ORDER_DATE))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Seul un BED en brouillon peut être supprimé");

            verify(commandeRepository, never()).delete(any(Commande.class));
        }
    }

    @Nested
    @DisplayName("createBedFromImport")
    class CreateBedFromImport {

        @Test
        @DisplayName("ne cree rien quand l importation est vide")
        void importationVide() {
            assertThat(service.createBedFromImport(MotifBed.BASCULEMENT, 3, List.of())).isNull();

            verify(commandeRepository, never()).save(any());
        }

        @Test
        @DisplayName("cree un BED deja cloture et date du jour")
        void bedDejaCloture() {
            String reference = service.createBedFromImport(
                MotifBed.BASCULEMENT,
                null,
                List.of(new BedImportLigneDTO(77, 5, 400, 800))
            );

            Commande saved = capturerCommande();
            assertThat(saved.getOrderStatus()).isEqualTo(OrderStatut.CLOSED);
            assertThat(saved.getType()).isEqualTo(TypeDeliveryReceipt.DIRECT);
            assertThat(saved.getOrderDate()).isEqualTo(LocalDate.now());
            assertThat(saved.getReceiptDate()).isEqualTo(LocalDate.now());
            assertThat(saved.getMotifBed()).isEqualTo(MotifBed.BASCULEMENT);
            assertThat(reference).isEqualTo(saved.getReceiptReference());
        }

        @Test
        @DisplayName("valorise le montant brut a partir des lignes importees")
        void valoriseLeMontantBrut() {
            service.createBedFromImport(
                MotifBed.BASCULEMENT,
                null,
                List.of(new BedImportLigneDTO(77, 5, 400, 800), new BedImportLigneDTO(77, 2, 150, 300))
            );

            Commande saved = capturerCommande();
            assertThat(saved.getGrossAmount()).isEqualTo(5 * 400 + 2 * 150);
            assertThat(saved.getDiscountAmount()).isZero();
            assertThat(saved.getTaxAmount()).isZero();
            assertThat(saved.getHtAmount()).isZero();
            assertThat(saved.getOrderLines()).hasSize(2);
        }

        @Test
        @DisplayName("cree des lignes deja receptionnees sans passer par le service de stock")
        void lignesDejaReceptionnees() {
            service.createBedFromImport(MotifBed.BASCULEMENT, null, List.of(new BedImportLigneDTO(77, 5, 400, 800)));

            OrderLine line = capturerCommande().getOrderLines().iterator().next();
            assertThat(line.getQuantityRequested()).isEqualTo(5);
            assertThat(line.getQuantityReceived()).isEqualTo(5);
            assertThat(line.getOrderCostAmount()).isEqualTo(400);
            assertThat(line.getOrderUnitPrice()).isEqualTo(800);
            assertThat(line.getInitStock()).isZero();
            assertThat(line.getFinalStock()).isEqualTo(5);
            assertThat(line.getUpdated()).isFalse();
            verifyNoInteractions(produitService);
        }

        @Test
        @DisplayName("rattache le fournisseur quand il est fourni")
        void avecFournisseur() {
            when(fournisseurRepository.getReferenceById(3)).thenReturn(fournisseur(3));

            service.createBedFromImport(MotifBed.BASCULEMENT, 3, List.of(new BedImportLigneDTO(77, 5, 400, 800)));

            assertThat(capturerCommande().getFournisseur()).isNotNull();
        }

        @Test
        @DisplayName("enregistre les lignes et trace les mouvements d inventaire")
        void enregistreEtTrace() {
            service.createBedFromImport(MotifBed.BASCULEMENT, null, List.of(new BedImportLigneDTO(77, 5, 400, 800)));

            verify(orderLineRepository).saveAll(any());
            verify(inventoryTransactionService).saveAll(any());
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.ENTREE_STOCK),
                org.mockito.ArgumentMatchers.eq("bed.import"),
                any(Object[].class),
                org.mockito.ArgumentMatchers.eq(String.valueOf(BED_ID))
            );
        }

        @Test
        @DisplayName("numerote la reference sur la sequence du jour")
        void referenceDuJour() {
            when(commandeRepository.countByTypeAndOrderDate(TypeDeliveryReceipt.DIRECT, LocalDate.now())).thenReturn(2L);

            String reference = service.createBedFromImport(
                MotifBed.BASCULEMENT,
                null,
                List.of(new BedImportLigneDTO(77, 5, 400, 800))
            );

            assertThat(reference).endsWith("-003");
        }
    }
}
