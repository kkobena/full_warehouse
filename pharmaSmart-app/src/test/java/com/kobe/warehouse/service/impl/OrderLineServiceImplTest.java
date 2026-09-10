package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.FournisseurService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeRapideDTO;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderLineServiceImpl")
class OrderLineServiceImplTest {

    private static final int PRODUIT_ID = 500;
    private static final int FOURNISSEUR_ID = 3;
    private static final OrderLineId ORDER_LINE_ID = new OrderLineId(900, LocalDate.of(2026, 4, 18));

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private FournisseurProduitService fournisseurProduitService;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private ProduitService produitService;

    @Mock
    private OrderLineIdGeneratorService orderLineIdGeneratorService;

    @Mock
    private FournisseurService fournisseurService;

    private OrderLineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderLineServiceImpl(
            orderLineRepository,
            fournisseurProduitService,
            produitRepository,
            produitService,
            orderLineIdGeneratorService,
            fournisseurService
        );
        when(orderLineIdGeneratorService.getNextIdAsInt()).thenReturn(900);
        when(orderLineRepository.save(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderLineRepository.saveAndFlush(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Produit produit() {
        Produit produit = new Produit();
        produit.setId(PRODUIT_ID);
        produit.setLibelle("DOLIPRANE 1000MG");
        produit.setStockProduits(new HashSet<>());
        return produit;
    }

    private static FournisseurProduit fournisseurProduit(int id, int prixAchat, int prixUni) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(FOURNISSEUR_ID);
        fournisseur.setLibelle("LABOREX");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(id);
        fp.setCodeCip("1234567");
        fp.setPrixAchat(prixAchat);
        fp.setPrixUni(prixUni);
        fp.setFournisseur(fournisseur);
        return fp;
    }

    private static OrderLineDTO dto() {
        return new OrderLineDTO()
            .setProduitId(PRODUIT_ID)
            .setQuantityRequested(10)
            .setTotalQuantity(20)
            .setCommande(commandeDTO());
    }

    private static CommandeDTO commandeDTO() {
        CommandeDTO dto = new CommandeDTO();
        dto.setFournisseurId(FOURNISSEUR_ID);
        return dto;
    }

    private static OrderLine orderLine(int quantityRequested) {
        OrderLine line = new OrderLine();
        line.setId(900);
        line.setOrderDate(LocalDate.of(2026, 4, 18));
        line.setQuantityRequested(quantityRequested);
        line.setOrderCostAmount(400);
        line.setOrderUnitPrice(800);
        line.setFreeQty(0);
        line.setTaxAmount(0);
        return line;
    }

    @Nested
    @DisplayName("buildOrderLineFromOrderLineDTO")
    class BuildOrderLineFromOrderLineDTO {

        @Test
        @DisplayName("reprend le produit fournisseur deja reference chez ce fournisseur")
        void fournisseurProduitExistant() {
            Produit produit = produit();
            FournisseurProduit fp = fournisseurProduit(77, 400, 800);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.of(fp));

            OrderLine ligne = service.buildOrderLineFromOrderLineDTO(dto());

            assertThat(ligne.getFournisseurProduit()).isSameAs(fp);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(400);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(800);
            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            assertThat(ligne.getInitStock()).isEqualTo(20);
            assertThat(ligne.getId().getId()).isEqualTo(900);
            verify(produitRepository).save(produit);
            verify(fournisseurProduitService, never()).createNewFournisseurProduit(any());
        }

        @Test
        @DisplayName("cree un referencement provisoire quand le fournisseur ne connait pas le produit")
        void referencementProvisoire() {
            Produit produit = produit();
            FournisseurProduit principal = fournisseurProduit(77, 400, 800);
            produit.setFournisseurProduitPrincipal(principal);
            FournisseurProduit cree = fournisseurProduit(78, 400, 800);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());
            when(fournisseurService.getParentIdByChildId(FOURNISSEUR_ID)).thenReturn(Optional.empty());
            when(fournisseurProduitService.createNewFournisseurProduit(any())).thenReturn(cree);
            OrderLineDTO dto = dto();

            OrderLine ligne = service.buildOrderLineFromOrderLineDTO(dto);

            assertThat(ligne.getFournisseurProduit()).isSameAs(cree);
            assertThat(dto.getProvisionalCode()).isTrue();
            assertThat(ligne.getProvisionalCode()).isTrue();
            ArgumentCaptor<FournisseurProduitDTO> captor = ArgumentCaptor.forClass(FournisseurProduitDTO.class);
            verify(fournisseurProduitService).createNewFournisseurProduit(captor.capture());
            assertThat(captor.getValue().getCodeCip()).isEqualTo("1234567");
            assertThat(captor.getValue().getFournisseurId()).isEqualTo(FOURNISSEUR_ID);
            assertThat(captor.getValue().getProduitId()).isEqualTo(PRODUIT_ID);
            assertThat(captor.getValue().isPrincipal()).isFalse();
        }

        @Test
        @DisplayName("rattache le referencement au fournisseur principal du groupe quand il existe")
        void referencementSurLeFournisseurParent() {
            Produit produit = produit();
            produit.setFournisseurProduitPrincipal(fournisseurProduit(77, 400, 800));
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());
            when(fournisseurService.getParentIdByChildId(FOURNISSEUR_ID)).thenReturn(Optional.of(9));
            when(fournisseurProduitService.createNewFournisseurProduit(any())).thenReturn(fournisseurProduit(78, 400, 800));

            service.buildOrderLineFromOrderLineDTO(dto());

            ArgumentCaptor<FournisseurProduitDTO> captor = ArgumentCaptor.forClass(FournisseurProduitDTO.class);
            verify(fournisseurProduitService).createNewFournisseurProduit(captor.capture());
            assertThat(captor.getValue().getFournisseurId()).isEqualTo(9);
        }

        @Test
        @DisplayName("refuse un produit sans fournisseur principal")
        void sansFournisseurPrincipal() {
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(produit());
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());
            OrderLineDTO dto = dto();

            assertThatThrownBy(() -> service.buildOrderLineFromOrderLineDTO(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ce produit n'a pas de fournisseur principal");
        }
    }

    @Nested
    @DisplayName("buildOrderLine")
    class BuildOrderLine {

        @Test
        @DisplayName("depuis un DTO : reprend les prix du produit fournisseur")
        void depuisUnDto() {
            FournisseurProduit fp = fournisseurProduit(77, 400, 800);

            OrderLine ligne = service.buildOrderLine(dto(), fp);

            assertThat(ligne.getId().getId()).isEqualTo(900);
            assertThat(ligne.getCreatedAt()).isNotNull();
            assertThat(ligne.getUpdatedAt()).isEqualTo(ligne.getCreatedAt());
            assertThat(ligne.getInitStock()).isEqualTo(20);
            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(800);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(400);
            assertThat(ligne.getFournisseurProduit()).isSameAs(fp);
        }

        @Test
        @DisplayName("depuis un DTO : ne marque pas la ligne comme provisoire sans indication")
        void sansCodeProvisoire() {
            assertThat(service.buildOrderLine(dto(), fournisseurProduit(77, 400, 800)).getProvisionalCode()).isFalse();
        }

        @Test
        @DisplayName("depuis un DTO : reporte le code provisoire quand il est transmis")
        void avecCodeProvisoire() {
            OrderLineDTO dto = dto().setProvisionalCode(Boolean.TRUE);

            assertThat(service.buildOrderLine(dto, fournisseurProduit(77, 400, 800)).getProvisionalCode()).isTrue();
        }

        @Test
        @DisplayName("depuis une commande rapide : reprend les prix du produit fournisseur")
        void depuisUneCommandeRapide() {
            FournisseurProduit fp = fournisseurProduit(77, 400, 800);
            when(fournisseurProduitService.getOne(77)).thenReturn(Optional.of(fp));

            OrderLine ligne = service.buildOrderLine(new CommandeRapideDTO(77, 10, 20));

            assertThat(ligne.getFournisseurProduit()).isSameAs(fp);
            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            assertThat(ligne.getInitStock()).isEqualTo(20);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(800);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(400);
        }

        @Test
        @DisplayName("depuis une commande rapide : refuse un produit fournisseur inconnu")
        void commandeRapideProduitInconnu() {
            when(fournisseurProduitService.getOne(77)).thenReturn(Optional.empty());
            CommandeRapideDTO commande = new CommandeRapideDTO(77, 10, 20);

            assertThatThrownBy(() -> service.buildOrderLine(commande))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("FournisseurProduit introuvable : 77");
        }

        @Test
        @DisplayName("depuis une commande : reprend l horodatage de la commande")
        void depuisUneCommande() {
            Commande commande = new Commande();
            commande.setId(12);
            commande.setOrderDate(LocalDate.of(2026, 4, 18));
            commande.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 0));
            OrderLineDTO dto = dto().setQuantityReceived(8).setInitStock(20).setOrderUnitPrice(900).setOrderCostAmount(450).setFreeQty(2);
            dto.setProvisionalCode(Boolean.TRUE);

            OrderLine ligne = service.createOrderLine(commande, dto);

            assertThat(ligne.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 18, 10, 0));
            assertThat(ligne.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 18, 10, 0));
            assertThat(ligne.getQuantityReceived()).isEqualTo(8);
            assertThat(ligne.getInitStock()).isEqualTo(20);
            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
            assertThat(ligne.getFreeQty()).isEqualTo(2);
            assertThat(ligne.getProvisionalCode()).isTrue();
        }
    }

    @Nested
    @DisplayName("buildOrderLine depuis une suggestion")
    class BuildOrderLineDepuisSuggestion {

        private SuggestionLine suggestionLine(Produit produit, int quantity) {
            FournisseurProduit source = fournisseurProduit(77, 400, 800);
            source.setProduit(produit);
            SuggestionLine ligne = new SuggestionLine();
            ligne.setId(50);
            ligne.setQuantity(quantity);
            ligne.setFournisseurProduit(source);
            return ligne;
        }

        private static StockProduit stock(int qtyStock, int qtyUg) {
            StockProduit sp = new StockProduit();
            sp.setQtyStock(qtyStock);
            sp.setQtyUG(qtyUg);
            return sp;
        }

        @Test
        @DisplayName("somme le stock du produit et reprend les prix du fournisseur cible")
        void fournisseurCibleReference() {
            Produit produit = produit();
            produit.getStockProduits().add(stock(20, 3));
            FournisseurProduit cible = fournisseurProduit(78, 450, 900);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.of(cible));

            OrderLine ligne = service.buildOrderLine(suggestionLine(produit, 12), FOURNISSEUR_ID);

            assertThat(ligne.getInitStock()).isEqualTo(23);
            assertThat(ligne.getQuantityRequested()).isEqualTo(12);
            assertThat(ligne.getQuantityReceived()).isZero();
            assertThat(ligne.getFreeQty()).isZero();
            assertThat(ligne.getTaxAmount()).isZero();
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
            assertThat(ligne.getFournisseurProduit()).isSameAs(cible);
            assertThat(ligne.getProvisionalCode()).isFalse();
        }

        @Test
        @DisplayName("cree un referencement provisoire quand le fournisseur cible ne connait pas le produit")
        void fournisseurCibleNonReference() {
            Produit produit = produit();
            FournisseurProduit cree = fournisseurProduit(78, 400, 800);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());
            when(fournisseurProduitService.createNewFournisseurProduit(any())).thenReturn(cree);

            OrderLine ligne = service.buildOrderLine(suggestionLine(produit, 12), FOURNISSEUR_ID);

            assertThat(ligne.getProvisionalCode()).isTrue();
            assertThat(ligne.getFournisseurProduit()).isSameAs(cree);
            assertThat(ligne.getInitStock()).isZero();
            ArgumentCaptor<FournisseurProduitDTO> captor = ArgumentCaptor.forClass(FournisseurProduitDTO.class);
            verify(fournisseurProduitService).createNewFournisseurProduit(captor.capture());
            assertThat(captor.getValue().getCodeCip()).isEqualTo("1234567");
            assertThat(captor.getValue().getFournisseurId()).isEqualTo(FOURNISSEUR_ID);
        }
    }

    @Nested
    @DisplayName("mises a jour de ligne")
    class MisesAJour {

        @Test
        @DisplayName("updateOrderLineQuantityRequested renvoie l avant et l apres")
        void quantiteDemandee() {
            OrderLine ligne = orderLine(10);
            when(orderLineRepository.getReferenceById(ORDER_LINE_ID)).thenReturn(ligne);
            OrderLineDTO dto = new OrderLineDTO().setQuantityRequested(15);
            dto.setOrderLineId(ORDER_LINE_ID);

            Pair<OrderLine, OrderLine> paire = service.updateOrderLineQuantityRequested(dto);

            assertThat(paire.getFirst().getQuantityRequested()).isEqualTo(10);
            assertThat(paire.getSecond()).isSameAs(ligne);
            assertThat(ligne.getQuantityRequested()).isEqualTo(15);
            assertThat(ligne.getUpdatedAt()).isNotNull();
            verify(orderLineRepository).saveAndFlush(ligne);
        }

        @Test
        @DisplayName("updateOrderLineUnitPrice renvoie l avant et l apres")
        void prixDeVente() {
            OrderLine ligne = orderLine(10);
            when(orderLineRepository.getReferenceById(ORDER_LINE_ID)).thenReturn(ligne);
            OrderLineDTO dto = new OrderLineDTO().setOrderUnitPrice(950);
            dto.setOrderLineId(ORDER_LINE_ID);

            Pair<OrderLine, OrderLine> paire = service.updateOrderLineUnitPrice(dto);

            assertThat(paire.getFirst().getOrderUnitPrice()).isEqualTo(800);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(950);
        }

        @Test
        @DisplayName("updateOrderLineCostAmount renvoie l avant et l apres")
        void prixDAchat() {
            OrderLine ligne = orderLine(10);
            when(orderLineRepository.getReferenceById(ORDER_LINE_ID)).thenReturn(ligne);
            OrderLineDTO dto = new OrderLineDTO().setOrderCostAmount(450);
            dto.setOrderLineId(ORDER_LINE_ID);

            Pair<OrderLine, OrderLine> paire = service.updateOrderLineCostAmount(dto);

            assertThat(paire.getFirst().getOrderCostAmount()).isEqualTo(400);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
        }

        @Test
        @DisplayName("updateOrderLine cumule la quantite demandee sans persister")
        void cumuleLaQuantite() {
            OrderLine ligne = orderLine(10);

            service.updateOrderLine(ligne, 5);

            assertThat(ligne.getQuantityRequested()).isEqualTo(15);
            assertThat(ligne.getUpdatedAt()).isNotNull();
            verify(orderLineRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateOrderLine sans quantite ne fait rien")
        void updateOrderLineSansEffet() {
            OrderLine ligne = orderLine(10);

            service.updateOrderLine(ligne);

            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            verifyNoInteractions(orderLineRepository);
        }

        @Test
        @DisplayName("updateOrderLineQuantityReceived enregistre la quantite recue")
        void quantiteRecue() {
            OrderLine ligne = orderLine(10);

            service.updateOrderLineQuantityReceived(ligne, 8);

            assertThat(ligne.getQuantityReceived()).isEqualTo(8);
            assertThat(ligne.getUpdatedAt()).isNotNull();
            verify(orderLineRepository).save(ligne);
        }

        @Test
        @DisplayName("updateOrderLineQuantityUG enregistre les unites gratuites")
        void unitesGratuites() {
            OrderLine ligne = orderLine(10);
            when(orderLineRepository.getReferenceById(ORDER_LINE_ID)).thenReturn(ligne);

            service.updateOrderLineQuantityUG(ORDER_LINE_ID, 3);

            assertThat(ligne.getFreeQty()).isEqualTo(3);
            verify(orderLineRepository).save(ligne);
        }

        @Test
        @DisplayName("updateCodeCip fige le code definitif du produit fournisseur")
        void updateCodeCip() {
            OrderLine ligne = orderLine(10);
            FournisseurProduit fp = fournisseurProduit(77, 400, 800);
            ligne.setFournisseurProduit(fp);
            ligne.setProvisionalCode(Boolean.TRUE);
            when(orderLineRepository.findById(ORDER_LINE_ID)).thenReturn(Optional.of(ligne));
            OrderLineDTO dto = new OrderLineDTO().setProduitCip("7654321");
            dto.setOrderLineId(ORDER_LINE_ID);

            service.updateCodeCip(dto);

            verify(fournisseurProduitService).updateCip("7654321", fp);
            assertThat(ligne.getProvisionalCode()).isFalse();
            verify(orderLineRepository).save(ligne);
        }
    }

    @Nested
    @DisplayName("changeFournisseurProduit")
    class ChangeFournisseurProduit {

        private OrderLine ligneAvecProduit(Produit produit) {
            FournisseurProduit source = fournisseurProduit(77, 400, 800);
            source.setProduit(produit);
            OrderLine ligne = orderLine(10);
            ligne.setFournisseurProduit(source);
            return ligne;
        }

        @Test
        @DisplayName("bascule sur le referencement existant et reprend ses prix")
        void referencementExistant() {
            OrderLine ligne = ligneAvecProduit(produit());
            FournisseurProduit cible = fournisseurProduit(78, 450, 900);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, 9)).thenReturn(Optional.of(cible));

            service.changeFournisseurProduit(ligne, 9);

            assertThat(ligne.getFournisseurProduit()).isSameAs(cible);
            assertThat(ligne.getProvisionalCode()).isFalse();
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
            verify(orderLineRepository).save(ligne);
        }

        @Test
        @DisplayName("cree un referencement provisoire quand le fournisseur cible ne connait pas le produit")
        void referencementProvisoire() {
            Produit produit = produit();
            produit.setFournisseurProduitPrincipal(fournisseurProduit(77, 400, 800));
            OrderLine ligne = ligneAvecProduit(produit);
            FournisseurProduit cree = fournisseurProduit(78, 450, 900);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, 9)).thenReturn(Optional.empty());
            when(fournisseurProduitService.createNewFournisseurProduit(any())).thenReturn(cree);

            service.changeFournisseurProduit(ligne, 9);

            assertThat(ligne.getFournisseurProduit()).isSameAs(cree);
            assertThat(ligne.getProvisionalCode()).isTrue();
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            ArgumentCaptor<FournisseurProduitDTO> captor = ArgumentCaptor.forClass(FournisseurProduitDTO.class);
            verify(fournisseurProduitService).createNewFournisseurProduit(captor.capture());
            assertThat(captor.getValue().getFournisseurId()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("buildDeliveryReceiptItemFromRecord")
    class BuildDeliveryReceiptItemFromRecord {

        private Commande commande() {
            Commande commande = new Commande();
            commande.setId(12);
            commande.setOrderDate(LocalDate.of(2026, 4, 18));
            commande.setCreatedAt(LocalDateTime.of(2026, 4, 18, 10, 0));
            return commande;
        }

        @Test
        @DisplayName("reprend les prix du fichier quand ils sont renseignes")
        void prixDuFichier() {
            OrderLine ligne = service.buildDeliveryReceiptItemFromRecord(
                fournisseurProduit(77, 400, 800),
                10,
                8,
                450,
                900,
                2,
                20,
                18,
                commande()
            );

            assertThat(ligne.getId().getId()).isEqualTo(900);
            assertThat(ligne.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 18, 10, 0));
            assertThat(ligne.getQuantityRequested()).isEqualTo(10);
            assertThat(ligne.getQuantityReceived()).isEqualTo(8);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            assertThat(ligne.getFreeQty()).isEqualTo(2);
            assertThat(ligne.getInitStock()).isEqualTo(20);
            assertThat(ligne.getTaxAmount()).isEqualTo(18);
        }

        @Test
        @DisplayName("retombe sur les prix du fournisseur quand le fichier ne les porte pas")
        void prixDuFournisseur() {
            OrderLine ligne = service.buildDeliveryReceiptItemFromRecord(
                fournisseurProduit(77, 400, 800),
                10,
                8,
                0,
                0,
                0,
                20,
                0,
                commande()
            );

            assertThat(ligne.getOrderCostAmount()).isEqualTo(400);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(800);
        }
    }

    @Nested
    @DisplayName("acces au depot")
    class AccesAuDepot {

        @Test
        @DisplayName("createOrderLine enregistre et vide le contexte")
        void createOrderLine() {
            OrderLine ligne = orderLine(10);

            assertThat(service.createOrderLine(ligne)).isSameAs(ligne);

            verify(orderLineRepository).saveAndFlush(ligne);
        }

        @Test
        @DisplayName("save enregistre la ligne")
        void save() {
            OrderLine ligne = orderLine(10);

            assertThat(service.save(ligne)).isSameAs(ligne);

            verify(orderLineRepository).save(ligne);
        }

        @Test
        @DisplayName("saveAll accepte une liste comme un ensemble")
        void saveAll() {
            List<OrderLine> liste = List.of(orderLine(10));
            Set<OrderLine> ensemble = Set.of(orderLine(10));

            service.saveAll(liste);
            service.saveAll(ensemble);

            verify(orderLineRepository).saveAll(liste);
            verify(orderLineRepository).saveAll(ensemble);
        }

        @Test
        @DisplayName("deleteAll, deleteOrderLine et delete suppriment les lignes")
        void suppressions() {
            Set<OrderLine> ensemble = Set.of(orderLine(10));
            OrderLine ligne = orderLine(10);

            service.deleteAll(ensemble);
            service.deleteOrderLine(ligne);
            service.delete(ligne);

            verify(orderLineRepository).deleteAll(ensemble);
            verify(orderLineRepository, org.mockito.Mockito.times(2)).delete(ligne);
        }

        @Test
        @DisplayName("findAllByOrderLineIdIn delegue au depot")
        void findAllByOrderLineIdIn() {
            List<OrderLine> attendu = List.of(orderLine(10));
            when(orderLineRepository.findAllByIdInAndOrderDate(Set.of(900), LocalDate.of(2026, 4, 18))).thenReturn(attendu);

            assertThat(service.findAllByOrderLineIdIn(Set.of(900), LocalDate.of(2026, 4, 18))).isSameAs(attendu);
        }

        @Test
        @DisplayName("findOneById renvoie vide sans identifiant")
        void findOneByIdSansId() {
            assertThat(service.findOneById(null)).isEmpty();

            verifyNoInteractions(orderLineRepository);
        }

        @Test
        @DisplayName("findOneById delegue au depot")
        void findOneById() {
            OrderLine ligne = orderLine(10);
            when(orderLineRepository.findById(ORDER_LINE_ID)).thenReturn(Optional.of(ligne));

            assertThat(service.findOneById(ORDER_LINE_ID)).contains(ligne);
        }

        @Test
        @DisplayName("findOneFromCommande renvoie vide quand le produit n est pas reference")
        void findOneFromCommandeSansReferencement() {
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());

            assertThat(service.findOneFromCommande(PRODUIT_ID, new CommandeId(12, LocalDate.of(2026, 4, 18)), FOURNISSEUR_ID)).isEmpty();

            verifyNoInteractions(orderLineRepository);
        }

        @Test
        @DisplayName("findOneFromCommande cherche la ligne du produit fournisseur dans la commande")
        void findOneFromCommande() {
            OrderLine ligne = orderLine(10);
            when(fournisseurProduitService.findFirstByProduitIdAndFournisseurId(PRODUIT_ID, FOURNISSEUR_ID))
                .thenReturn(Optional.of(fournisseurProduit(77, 400, 800)));
            when(
                orderLineRepository.findFirstByFournisseurProduitIdAndCommandeIdAndCommandeOrderDate(77, 12, LocalDate.of(2026, 4, 18))
            ).thenReturn(Optional.of(ligne));

            assertThat(service.findOneFromCommande(PRODUIT_ID, new CommandeId(12, LocalDate.of(2026, 4, 18)), FOURNISSEUR_ID))
                .contains(ligne);
        }

        @Test
        @DisplayName("getFournisseurProduitByCriteria delegue au service produit")
        void getFournisseurProduitByCriteria() {
            when(produitService.getFournisseurProduitByCriteria("1234567", FOURNISSEUR_ID)).thenReturn(Optional.empty());

            assertThat(service.getFournisseurProduitByCriteria("1234567", FOURNISSEUR_ID)).isEmpty();
        }

        @Test
        @DisplayName("produitTotalStockWithQantitUg delegue au service produit")
        void produitTotalStock() {
            Produit produit = produit();
            when(produitService.produitTotalStock(produit)).thenReturn(23);

            assertThat(service.produitTotalStockWithQantitUg(produit)).isEqualTo(23);
        }
    }
}
