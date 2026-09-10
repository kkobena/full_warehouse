package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.csv.ExportationCsvService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.CommandeRapideDTO;
import com.kobe.warehouse.service.dto.CommandeResponseDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurStatsServiceDTO;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.SemoisCommanderDTO;
import com.kobe.warehouse.service.dto.VerificationResponseCommandeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.util.Pair;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommandServiceImpl")
class CommandServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.now();
    private static final int FOURNISSEUR_ID = 3;
    private static final CommandeId COMMANDE_ID = new CommandeId(12, ORDER_DATE);

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private OrderLineService orderLineService;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private ExportationCsvService exportationCsvService;

    @Mock
    private ImportationEchoueService importationEchoueService;

    @Mock
    private CommandeIdGeneratorService commandeIdGeneratorService;

    @Mock
    private SuggestionReassortService suggestionReassortService;

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private SuggestionLineRepository suggestionLineRepository;

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    private CommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommandServiceImpl(
            commandeRepository,
            storageService,
            orderLineService,
            referenceService,
            exportationCsvService,
            importationEchoueService,
            commandeIdGeneratorService,
            suggestionReassortService,
            suggestionRepository,
            suggestionLineRepository,
            fournisseurProduitRepository
        );
        AppUser user = new AppUser();
        user.setId(1);
        user.setFirstName("Awa");
        user.setLastName("KONE");
        when(storageService.getUser()).thenReturn(user);
        when(referenceService.buildNumCommande()).thenReturn("CMD-2026-001");
        when(commandeIdGeneratorService.getNextIdAsInt()).thenReturn(77);
        when(commandeRepository.save(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandeRepository.saveAndFlush(any(Commande.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderLineService.save(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ fixtures

    private static Produit produit(int id, String libelle) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle(libelle);
        produit.setCodeEanLaboratoire("EAN" + id);
        produit.setStockProduits(new LinkedHashSet<>());
        return produit;
    }

    private static FournisseurProduit fournisseurProduit(int id, String codeCip) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(FOURNISSEUR_ID);
        fournisseur.setLibelle("LABOREX");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(id);
        fp.setCodeCip(codeCip);
        fp.setCodeEan("EAN-FP-" + id);
        fp.setPrixAchat(400);
        fp.setPrixUni(800);
        fp.setFournisseur(fournisseur);
        fp.setProduit(produit(id * 10, "PRODUIT " + id));
        return fp;
    }

    private static OrderLine orderLine(int id, FournisseurProduit fp, int quantityRequested, int quantityReceived) {
        OrderLine line = new OrderLine();
        line.setId(id);
        line.setOrderDate(ORDER_DATE);
        line.setFournisseurProduit(fp);
        line.setQuantityRequested(quantityRequested);
        line.setQuantityReceived(quantityReceived);
        line.setOrderCostAmount(400);
        line.setOrderUnitPrice(800);
        line.setGrossAmount(400 * quantityRequested);
        line.setOrderAmount(800 * quantityRequested);
        line.setFreeQty(0);
        line.setTaxAmount(0);
        line.setInitStock(5);
        return line;
    }

    private static Commande commande(int id, OrderLine... lines) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(FOURNISSEUR_ID);
        Commande commande = new Commande();
        commande.setId(id);
        commande.setOrderDate(ORDER_DATE);
        commande.setOrderReference("CMD-" + id);
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setFournisseur(fournisseur);
        commande.setGrossAmount(0);
        commande.setOrderAmount(0);
        commande.setFinalAmount(0);
        commande.setTaxAmount(0);
        commande.setOrderLines(new ArrayList<>());
        for (OrderLine line : lines) {
            commande.addOrderLine(line);
        }
        return commande;
    }

    private void referenceCommande(Commande commande) {
        when(commandeRepository.getReferenceById(any(CommandeId.class))).thenReturn(commande);
    }

    // ------------------------------------------------------------------ tests

    @Nested
    @DisplayName("creation de commande")
    class Creation {

        @Test
        @DisplayName("createNewCommandeFromCommandeDTO valorise la commande a partir de la premiere ligne")
        void depuisUnDto() {
            OrderLineDTO ligneDTO = new OrderLineDTO().setQuantityRequested(10);
            CommandeDTO dto = new CommandeDTO().setOrderLines(List.of(ligneDTO));
            dto.setFournisseurId(FOURNISSEUR_ID);
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            when(orderLineService.buildOrderLineFromOrderLineDTO(ligneDTO)).thenReturn(ligne);

            CommandeLiteDTO cree = service.createNewCommandeFromCommandeDTO(dto);

            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).saveAndFlush(captor.capture());
            Commande commande = captor.getValue();
            assertThat(commande.getId().getId()).isEqualTo(77);
            assertThat(commande.getOrderReference()).isEqualTo("CMD-2026-001");
            assertThat(commande.getOrderLines()).containsExactly(ligne);
            assertThat(commande.getOrderAmount()).isEqualTo(8000);
            assertThat(commande.getFinalAmount()).isEqualTo(8000);
            assertThat(commande.getGrossAmount()).isEqualTo(4000);
            assertThat(commande.getFournisseur().getId()).isEqualTo(FOURNISSEUR_ID);
            assertThat(commande.getCreatedAt()).isEqualTo(commande.getUpdatedAt());
            assertThat(cree.getId()).isEqualTo(77);
        }

        @Test
        @DisplayName("createCommandeRapide deduit le fournisseur du produit fournisseur")
        void commandeRapide() {
            CommandeRapideDTO dto = new CommandeRapideDTO(55, 10, 20);
            OrderLine ligne = orderLine(900, fournisseurProduit(55, "5555555"), 10, 0);
            when(orderLineService.buildOrderLine(dto)).thenReturn(ligne);

            CommandeLiteDTO cree = service.createCommandeRapide(dto);

            assertThat(cree.getId()).isEqualTo(77);
            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(FOURNISSEUR_ID);
        }
    }

    @Nested
    @DisplayName("createOrUpdateOrderLine")
    class CreateOrUpdate {

        private OrderLineDTO dto() {
            CommandeDTO commandeDTO = new CommandeDTO();
            commandeDTO.setCommandeId(COMMANDE_ID);
            commandeDTO.setFournisseurId(FOURNISSEUR_ID);
            return new OrderLineDTO().setProduitId(10).setQuantityRequested(4).setCommande(commandeDTO);
        }

        @Test
        @DisplayName("ajoute une nouvelle ligne quand le produit n'est pas encore dans la commande")
        void nouvelleLigne() {
            Commande commande = commande(12);
            referenceCommande(commande);
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 4, 0);
            OrderLineDTO dto = dto();
            when(orderLineService.findOneFromCommande(10, COMMANDE_ID, FOURNISSEUR_ID)).thenReturn(Optional.empty());
            when(orderLineService.buildOrderLineFromOrderLineDTO(dto)).thenReturn(ligne);

            CommandeLiteDTO resultat = service.createOrUpdateOrderLine(dto);

            assertThat(commande.getOrderLines()).contains(ligne);
            assertThat(commande.getGrossAmount()).isEqualTo(1600);
            assertThat(commande.getFinalAmount()).isEqualTo(3200);
            assertThat(commande.getOrderAmount()).isEqualTo(3200);
            assertThat(resultat.getId()).isEqualTo(12);
            verify(orderLineService).save(ligne);
        }

        @Test
        @DisplayName("cumule la quantite sur la ligne existante et corrige les montants")
        void ligneExistante() {
            OrderLine existante = orderLine(900, fournisseurProduit(1, "1111111"), 6, 0);
            Commande commande = commande(12, existante);
            commande.setGrossAmount(2400);
            commande.setFinalAmount(4800);
            referenceCommande(commande);
            when(orderLineService.findOneFromCommande(10, COMMANDE_ID, FOURNISSEUR_ID)).thenReturn(Optional.of(existante));

            service.createOrUpdateOrderLine(dto());

            assertThat(existante.getQuantityRequested()).isEqualTo(10);
            // 10*400 + 2400 - (6*400) = 4000
            assertThat(commande.getGrossAmount()).isEqualTo(4000);
            // 10*800 + 4800 - (6*800) = 8000
            assertThat(commande.getFinalAmount()).isEqualTo(8000);
            verify(orderLineService, never()).buildOrderLineFromOrderLineDTO(any(OrderLineDTO.class));
        }
    }

    @Nested
    @DisplayName("mises a jour de lignes")
    class MisesAJour {

        private Pair<OrderLine, OrderLine> paire() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ancienne = orderLine(900, fp, 5, 0);
            OrderLine nouvelle = orderLine(900, fp, 8, 0);
            Commande commande = commande(12);
            commande.setGrossAmount(2000);
            commande.setFinalAmount(4000);
            nouvelle.setCommande(commande);
            return Pair.of(ancienne, nouvelle);
        }

        @Test
        @DisplayName("updateQuantityRequested reporte l'ecart de quantite sur la commande")
        void quantiteDemandee() {
            OrderLineDTO dto = new OrderLineDTO();
            when(orderLineService.updateOrderLineQuantityRequested(dto)).thenReturn(paire());

            CommandeLiteDTO resultat = service.updateQuantityRequested(dto);

            assertThat(resultat.getId()).isEqualTo(12);
            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).saveAndFlush(captor.capture());
            // 8*400 + 2000 - 5*400 = 3200
            assertThat(captor.getValue().getGrossAmount()).isEqualTo(3200);
            // 8*800 + 4000 - 5*800 = 6400
            assertThat(captor.getValue().getFinalAmount()).isEqualTo(6400);
            assertThat(captor.getValue().getOrderAmount()).isEqualTo(6400);
        }

        @Test
        @DisplayName("updateOrderCostAmount recalcule la commande")
        void prixDAchat() {
            OrderLineDTO dto = new OrderLineDTO();
            when(orderLineService.updateOrderLineCostAmount(dto)).thenReturn(paire());

            assertThat(service.updateOrderCostAmount(dto).getGrossAmount()).isEqualTo(3200);
        }

        @Test
        @DisplayName("updateOrderUnitPrice recalcule la commande")
        void prixDeVente() {
            OrderLineDTO dto = new OrderLineDTO();
            when(orderLineService.updateOrderLineUnitPrice(dto)).thenReturn(paire());

            assertThat(service.updateOrderUnitPrice(dto).getFinalAmount()).isEqualTo(6400);
        }

        @Test
        @DisplayName("updateOrderLineQuantityReceived delegue au service de ligne")
        void quantiteRecue() {
            OrderLineId id = new OrderLineId(900, ORDER_DATE);
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 5, 0);
            OrderLineDTO dto = new OrderLineDTO().setQuantityReceived(3);
            dto.setOrderLineId(id);
            when(orderLineService.findOneById(id)).thenReturn(Optional.of(ligne));

            service.updateOrderLineQuantityReceived(dto);

            verify(orderLineService).updateOrderLineQuantityReceived(ligne, 3);
        }

        @Test
        @DisplayName("updateOrderLineQuantityReceived echoue quand la ligne est introuvable")
        void quantiteRecueLigneIntrouvable() {
            OrderLineDTO dto = new OrderLineDTO().setQuantityReceived(3);
            dto.setOrderLineId(new OrderLineId(900, ORDER_DATE));
            when(orderLineService.findOneById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrderLineQuantityReceived(dto))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Ligne de commande introuvable");
        }

        @Test
        @DisplayName("updateOrderLineQuantityUg delegue au service de ligne")
        void quantiteUg() {
            OrderLineId id = new OrderLineId(900, ORDER_DATE);
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 5, 0);
            OrderLineDTO dto = new OrderLineDTO().setFreeQty(2);
            dto.setOrderLineId(id);
            when(orderLineService.findOneById(id)).thenReturn(Optional.of(ligne));

            service.updateOrderLineQuantityUg(dto);

            verify(orderLineService).updateOrderLineQuantityUG(ligne.getId(), 2);
        }

        @Test
        @DisplayName("updateOrderLineQuantityUg echoue quand la ligne est introuvable")
        void quantiteUgLigneIntrouvable() {
            OrderLineDTO dto = new OrderLineDTO().setFreeQty(2);
            dto.setOrderLineId(new OrderLineId(900, ORDER_DATE));
            when(orderLineService.findOneById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrderLineQuantityUg(dto)).isInstanceOf(GenericError.class);
        }

        @Test
        @DisplayName("updateCodeCip delegue au service de ligne")
        void codeCip() {
            OrderLineDTO dto = new OrderLineDTO();

            service.updateCodeCip(dto);

            verify(orderLineService).updateCodeCip(dto);
        }
    }

    @Nested
    @DisplayName("suppressions")
    class Suppressions {

        @Test
        @DisplayName("deleteOrderLineById retire la ligne et recalcule les montants")
        void suppressionDUneLigne() {
            OrderLine ligne1 = orderLine(900, fournisseurProduit(1, "1111111"), 5, 0);
            OrderLine ligne2 = orderLine(901, fournisseurProduit(2, "2222222"), 3, 0);
            Commande commande = commande(12, ligne1, ligne2);
            when(orderLineService.findOneById(ligne1.getId())).thenReturn(Optional.of(ligne1));

            service.deleteOrderLineById(ligne1.getId());

            assertThat(commande.getOrderLines()).containsExactly(ligne2);
            assertThat(commande.getGrossAmount()).isEqualTo(1200);
            assertThat(commande.getOrderAmount()).isEqualTo(2400);
            assertThat(commande.getFinalAmount()).isEqualTo(2400);
            verify(orderLineService).deleteOrderLine(ligne1);
            verify(commandeRepository).save(commande);
        }

        @Test
        @DisplayName("deleteOrderLineById ne fait rien si la ligne est introuvable")
        void suppressionLigneIntrouvable() {
            when(orderLineService.findOneById(any())).thenReturn(Optional.empty());

            service.deleteOrderLineById(new OrderLineId(900, ORDER_DATE));

            verify(commandeRepository, never()).save(any(Commande.class));
        }

        @Test
        @DisplayName("deleteOrderLinesByIds retire plusieurs lignes en une passe")
        void suppressionEnLot() {
            OrderLine ligne1 = orderLine(900, fournisseurProduit(1, "1111111"), 5, 0);
            OrderLine ligne2 = orderLine(901, fournisseurProduit(2, "2222222"), 3, 0);
            Commande commande = commande(12, ligne1, ligne2);
            referenceCommande(commande);
            when(orderLineService.findOneById(ligne1.getId())).thenReturn(Optional.of(ligne1));
            when(orderLineService.findOneById(ligne2.getId())).thenReturn(Optional.empty());

            service.deleteOrderLinesByIds(COMMANDE_ID, new ArrayList<>(List.of(ligne1.getId(), ligne2.getId())));

            assertThat(commande.getOrderLines()).containsExactly(ligne2);
            assertThat(commande.getGrossAmount()).isEqualTo(1200);
            verify(orderLineService).deleteOrderLine(ligne1);
            verify(orderLineService, never()).deleteOrderLine(ligne2);
        }

        @Test
        @DisplayName("deleteById delegue au depot")
        void suppressionParId() {
            service.deleteById(COMMANDE_ID);

            verify(commandeRepository).deleteById(COMMANDE_ID);
        }

        @Test
        @DisplayName("deleteAll supprime chaque commande de la liste")
        void suppressionDeToutes() {
            CommandeId autre = new CommandeId(13, ORDER_DATE);

            service.deleteAll(List.of(COMMANDE_ID, autre));

            verify(commandeRepository).deleteById(COMMANDE_ID);
            verify(commandeRepository).deleteById(autre);
        }

        @Test
        @DisplayName("rollback remet la commande au statut demande")
        void retourEnArriere() {
            Commande commande = commande(12);
            commande.setOrderStatus(OrderStatut.RECEIVED);
            referenceCommande(commande);

            service.rollback(COMMANDE_ID);

            assertThat(commande.getOrderStatus()).isEqualTo(OrderStatut.REQUESTED);
            assertThat(commande.getUpdatedAt()).isNotNull();
            verify(commandeRepository).save(commande);
        }
    }

    @Nested
    @DisplayName("fusion de commandes")
    class Fusion {

        @Test
        @DisplayName("cumule les quantites des lignes communes et rattache les lignes inedites")
        void fusionner() {
            FournisseurProduit commun = fournisseurProduit(1, "1111111");
            OrderLine cible = orderLine(900, commun, 5, 0);
            Commande premiere = commande(12, cible);
            OrderLine doublon = orderLine(910, commun, 4, 0);
            OrderLine inedite = orderLine(911, fournisseurProduit(2, "2222222"), 3, 0);
            Commande seconde = commande(13, doublon, inedite);
            CommandeId secondeId = new CommandeId(13, ORDER_DATE);
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(premiere);
            when(commandeRepository.getReferenceById(secondeId)).thenReturn(seconde);

            service.fusionner(new ArrayList<>(List.of(secondeId, COMMANDE_ID)));

            verify(orderLineService).updateOrderLine(cible, 4);
            verify(orderLineService).save(cible);
            verify(orderLineService).save(inedite);
            assertThat(premiere.getOrderLines()).contains(inedite);
            // updateCommande(commande, orderLine) : grossAmount de la ligne inedite
            assertThat(premiere.getGrossAmount()).isEqualTo(1200);
            assertThat(premiere.getUpdatedAt()).isNotNull();
            verify(commandeRepository).save(premiere);
            verify(commandeRepository).deleteAll(List.of(seconde));
        }
    }

    @Nested
    @DisplayName("import d'un fichier de commande")
    class UploadNouvelleCommande {

        private MultipartFile fichier(String nom, String contenu) {
            return new MockMultipartFile("file", nom, "text/plain", contenu.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("CSV : cree une ligne par produit reconnu")
        void csvProduitReconnu() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ligne = orderLine(900, fp, 5, 5);
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.produitTotalStockWithQantitUg(fp.getProduit())).thenReturn(12);
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(ligne);

            CommandeResponseDTO reponse = service.uploadNewCommande(FOURNISSEUR_ID, CommandeModel.CIP_QTE, fichier("cmd.csv", "1111111;5\n"));

            assertThat(reponse.getSuccesCount()).isEqualTo(1);
            assertThat(reponse.getFailureCount()).isZero();
            assertThat(reponse.getItems()).isEmpty();
            assertThat(reponse.getReference()).isEqualTo("CMD-2026-001");
            ArgumentCaptor<OrderLineDTO> captor = ArgumentCaptor.forClass(OrderLineDTO.class);
            verify(orderLineService).createOrderLine(any(Commande.class), captor.capture());
            assertThat(captor.getValue().getInitStock()).isEqualTo(12);
            assertThat(captor.getValue().getOrderCostAmount()).isEqualTo(400);
            assertThat(captor.getValue().getOrderUnitPrice()).isEqualTo(800);
            assertThat(captor.getValue().getProvisionalCode()).isFalse();
            verifyNoInteractions(importationEchoueService);
            verify(exportationCsvService, never()).createRuptureFile(anyString(), any(), any());
        }

        @Test
        @DisplayName("CSV : la seconde occurrence d'un produit cumule les quantites")
        void csvProduitEnDoublon() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ligne = orderLine(900, fp, 5, 5);
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(ligne);

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.CIP_QTE,
                fichier("cmd.csv", "1111111;5\n1111111;3\n")
            );

            assertThat(reponse.getSuccesCount()).isEqualTo(2);
            assertThat(ligne.getQuantityReceived()).isEqualTo(8);
            assertThat(ligne.getQuantityRequested()).isEqualTo(8);
            verify(orderLineService, org.mockito.Mockito.times(1)).createOrderLine(any(Commande.class), any(OrderLineDTO.class));
        }

        @Test
        @DisplayName("CSV : un produit inconnu est reporte en echec et declenche le fichier de rupture")
        void csvProduitInconnu() {
            when(orderLineService.getFournisseurProduitByCriteria(anyString(), anyInt())).thenReturn(Optional.empty());

            CommandeResponseDTO reponse = service.uploadNewCommande(FOURNISSEUR_ID, CommandeModel.CIP_QTE, fichier("cmd.csv", "9999999;5\n"));

            assertThat(reponse.getFailureCount()).isEqualTo(1);
            assertThat(reponse.getItems()).singleElement().extracting(OrderItem::getProduitCip).isEqualTo("9999999");
            verify(exportationCsvService).createRuptureFile(eq("CMD-2026-001"), any(), eq(CommandeModel.CIP_QTE));
            verify(importationEchoueService).save(eq(77), eq(true), any());
        }

        @Test
        @DisplayName("TXT : cree une ligne par produit reconnu")
        void txtProduitReconnu() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ligne = orderLine(900, fp, 5, 5);
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(ligne);

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.CIP_QTE,
                fichier("cmd.txt", "1111111\tX\t450\t5\tY\t900\n")
            );

            assertThat(reponse.getSuccesCount()).isEqualTo(1);
            assertThat(reponse.getTotalItemCount()).isEqualTo(1);
            ArgumentCaptor<OrderLineDTO> captor = ArgumentCaptor.forClass(OrderLineDTO.class);
            verify(orderLineService).createOrderLine(any(Commande.class), captor.capture());
            assertThat(captor.getValue().getOrderCostAmount()).isEqualTo(450);
            assertThat(captor.getValue().getOrderUnitPrice()).isEqualTo(900);
        }

        @Test
        @DisplayName("TXT : la seconde occurrence d'un produit cumule les quantites")
        void txtProduitEnDoublon() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ligne = orderLine(900, fp, 5, 5);
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(ligne);

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.CIP_QTE,
                fichier("cmd.txt", "1111111\tX\t450\t5\tY\t900\n1111111\tX\t450\t2\tY\t900\n")
            );

            assertThat(reponse.getSuccesCount()).isEqualTo(2);
            assertThat(ligne.getQuantityReceived()).isEqualTo(7);
        }

        @Test
        @DisplayName("TXT : un produit inconnu est reporte en echec")
        void txtProduitInconnu() {
            when(orderLineService.getFournisseurProduitByCriteria(anyString(), anyInt())).thenReturn(Optional.empty());

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.CIP_QTE,
                fichier("cmd.txt", "9999999\tX\t450\t5\tY\t900\n")
            );

            assertThat(reponse.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getProduitCip()).isEqualTo("9999999");
                assertThat(item.getProduitEan()).isEqualTo("9999999");
                assertThat(item.getQuantityReceived()).isEqualTo(5);
                assertThat(item.getMontant()).isEqualTo(900.0);
            });
            verify(importationEchoueService).save(eq(77), eq(true), any());
        }

        @Test
        @DisplayName("une extension inconnue est refusee")
        void extensionInconnue() {
            MultipartFile fichier = fichier("cmd.pdf", "peu importe");

            assertThatThrownBy(() -> service.uploadNewCommande(FOURNISSEUR_ID, CommandeModel.LABOREX, fichier))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("LABOREX");
        }

        @Test
        @DisplayName("CSV : la ligne d'en-tete non numerique est ignoree")
        void csvAvecEntete() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(orderLine(900, fp, 5, 5));

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.CIP_QTE,
                fichier("cmd.csv", "CIP;QUANTITE\n1111111;5\n")
            );

            assertThat(reponse.getSuccesCount()).isEqualTo(1);
            assertThat(reponse.getTotalItemCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("CSV : le format TEDIS renseigne le lot et compte toutes les lignes du fichier")
        void csvFormatTedis() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine ligne = orderLine(900, fp, 5, 5);
            when(orderLineService.getFournisseurProduitByCriteria("1111111", FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.createOrderLine(any(Commande.class), any(OrderLineDTO.class))).thenReturn(ligne);

            CommandeResponseDTO reponse = service.uploadNewCommande(
                FOURNISSEUR_ID,
                CommandeModel.TEDIS,
                fichier("cmd.csv", "1;1111111;450;5;x;900;LOT-A;20271231\n")
            );

            // TEDIS n'a pas d'en-tete : la ligne unique est comptee telle quelle
            assertThat(reponse.getTotalItemCount()).isEqualTo(1);
            assertThat(ligne.getLots()).singleElement().satisfies(lot -> {
                assertThat(lot.getNumLot()).isEqualTo("LOT-A");
                assertThat(lot.getQuantity()).isEqualTo(5);
                assertThat(lot.getExpiryDate()).isEqualTo(LocalDate.of(2027, 12, 31));
                assertThat(lot.getProduit()).isSameAs(fp.getProduit());
                assertThat(lot.getOrderLine()).isSameAs(ligne);
            });
        }

        @Test
        @DisplayName("CSV : une erreur de lecture laisse la commande sans ligne")
        void csvErreurDeLecture() throws IOException {
            MultipartFile enErreur = org.mockito.Mockito.mock(MultipartFile.class);
            when(enErreur.getOriginalFilename()).thenReturn("cmd.csv");
            when(enErreur.getInputStream()).thenThrow(new IOException("flux coupe"));

            CommandeResponseDTO reponse = service.uploadNewCommande(FOURNISSEUR_ID, CommandeModel.CIP_QTE, enErreur);

            assertThat(reponse.getSuccesCount()).isZero();
            assertThat(reponse.getItems()).isEmpty();
            verify(commandeRepository).save(any(Commande.class));
        }

        @Test
        @DisplayName("TXT : une erreur de lecture laisse la commande sans ligne")
        void txtErreurDeLecture() throws IOException {
            MultipartFile enErreur = org.mockito.Mockito.mock(MultipartFile.class);
            when(enErreur.getOriginalFilename()).thenReturn("cmd.txt");
            when(enErreur.getInputStream()).thenThrow(new IOException("flux coupe"));

            CommandeResponseDTO reponse = service.uploadNewCommande(FOURNISSEUR_ID, CommandeModel.CIP_QTE, enErreur);

            assertThat(reponse.getTotalItemCount()).isZero();
            verify(commandeRepository).save(any(Commande.class));
        }

        @Test
        @DisplayName("createRuptureFile ne fait rien sans article en rupture")
        void ruptureSansArticle() {
            service.createRuptureFile("CMD-001", CommandeModel.LABOREX, List.of());

            verifyNoInteractions(exportationCsvService);
        }
    }

    @Nested
    @DisplayName("import de la reponse du grossiste")
    class ReponseGrossiste {

        private MultipartFile csv(String contenu) {
            return new MockMultipartFile("file", "reponse.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
        }

        private MultipartFile excel(Object[][] rows) throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("reponse");
                for (int r = 0; r < rows.length; r++) {
                    Row row = sheet.createRow(r);
                    for (int c = 0; c < rows[r].length; c++) {
                        Object value = rows[r][c];
                        if (value == null) {
                            continue;
                        }
                        if (value instanceof Number number) {
                            row.createCell(c).setCellValue(number.doubleValue());
                        } else {
                            row.createCell(c).setCellValue(value.toString());
                        }
                    }
                }
                workbook.write(out);
                return new MockMultipartFile(
                    "file",
                    "reponse.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
                );
            }
        }

        @Test
        @DisplayName("CSV : la quantite confirmee est reportee sur la ligne correspondante")
        void csvQuantiteConfirmee() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, csv("1111111;7\n"));

            assertThat(reponse.isAllLinesInRupture()).isFalse();
            assertThat(reponse.getExtraItems()).isEmpty();
            assertThat(reponse.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getCodeCip()).isEqualTo("1111111");
                assertThat(item.getQuantite()).isEqualTo(10);
                assertThat(item.getQuantitePriseEnCompte()).isEqualTo(7);
                assertThat(item.getProduitLibelle()).isEqualTo("PRODUIT 1");
                assertThat(item.getCodeEan()).isEqualTo("EAN10");
            });
            verify(orderLineService).updateOrderLineQuantityReceived(ligne, 7);
            verify(orderLineService).saveAll(Set.of(ligne));
            verify(commandeRepository).save(commande);
        }

        @Test
        @DisplayName("CSV : une quantite nulle retire la ligne de la commande")
        void csvLigneEnRupture() {
            OrderLine servie = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            OrderLine rupture = orderLine(901, fournisseurProduit(2, "2222222"), 4, 0);
            Commande commande = commande(12, servie, rupture);
            commande.setGrossAmount(5600);
            commande.setOrderAmount(11200);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.CIP_QTE,
                csv("1111111;7\n2222222;0\n")
            );

            assertThat(reponse.getItems()).hasSize(2);
            assertThat(commande.getOrderLines()).containsExactly(servie);
            assertThat(commande.getGrossAmount()).isEqualTo(5600 - 1600);
            assertThat(commande.getOrderAmount()).isEqualTo(11200 - 3200);
            verify(orderLineService).deleteAll(Set.of(rupture));
        }

        @Test
        @DisplayName("CSV : toutes les lignes en rupture laissent la commande intacte")
        void csvToutEnRupture() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, csv("1111111;0\n"));

            assertThat(reponse.isAllLinesInRupture()).isTrue();
            assertThat(commande.getOrderLines()).containsExactly(ligne);
            verify(commandeRepository, never()).save(any(Commande.class));
            verify(orderLineService, never()).deleteAll(any());
        }

        @Test
        @DisplayName("CSV : un code absent de la commande est classe en article supplementaire")
        void csvArticleSupplementaire() {
            Commande commande = commande(12, orderLine(900, fournisseurProduit(1, "1111111"), 10, 0));
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, csv("8888888;3\n"));

            assertThat(reponse.getItems()).isEmpty();
            assertThat(reponse.getExtraItems()).singleElement().satisfies(item -> {
                assertThat(item.getCodeCip()).isEqualTo("8888888");
                assertThat(item.getQuantitePriseEnCompte()).isEqualTo(3);
            });
        }

        @Test
        @DisplayName("CSV : une ligne illisible est ignoree sans interrompre l'import")
        void csvLigneIllisible() {
            Commande commande = commande(12, orderLine(900, fournisseurProduit(1, "1111111"), 10, 0));
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.CIP_QTE,
                csv("1111111;7\nligne;cassee\n")
            );

            assertThat(reponse.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("CSV : la ligne est reconnue par le code EAN du produit fournisseur")
        void csvReconnaissanceParEan() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.CIP_QTE,
                csv("EAN-FP-1;7\n")
            );

            assertThat(reponse.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("le nom du fichier est obligatoire")
        void nomDeFichierObligatoire() {
            MultipartFile sansNom = new MockMultipartFile("file", "", "text/csv", new byte[0]);

            assertThatThrownBy(() -> service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, sansNom))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("nom du fichier");
        }

        @Test
        @DisplayName("CSV : le format LABOREX reporte prix d'achat, prix de vente et unites gratuites")
        void csvFormatLaborex() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.LABOREX,
                csv("ETAB;FACT;LIGNE;CIP;LIB;QTE;UG;RECU;PA;PV;BL;TVA\nE1;F1;1;1111111;DOLIPRANE;10;2;7;450;900;BL-1;18\n")
            );

            assertThat(reponse.getItems()).hasSize(1);
            assertThat(ligne.getOrderCostAmount()).isEqualTo(450);
            assertThat(ligne.getOrderUnitPrice()).isEqualTo(900);
            assertThat(ligne.getFreeQty()).isEqualTo(2);
        }

        @Test
        @DisplayName("CSV : la ligne est reconnue par le code EAN laboratoire du produit")
        void csvReconnaissanceParEanLaboratoire() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, csv("EAN10;7\n"));

            assertThat(reponse.getItems()).hasSize(1);
            assertThat(reponse.getExtraItems()).isEmpty();
        }

        @Test
        @DisplayName("le nom du fichier absent est refuse")
        void nomDeFichierAbsent() {
            MultipartFile sansNom = org.mockito.Mockito.mock(MultipartFile.class);
            when(sansNom.getOriginalFilename()).thenReturn(null);

            assertThatThrownBy(() -> service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, sansNom))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("nom du fichier");
        }

        @Test
        @DisplayName("CSV : une erreur de lecture renvoie une reponse vide")
        void csvErreurDeLecture() throws IOException {
            referenceCommande(commande(12));
            MultipartFile enErreur = org.mockito.Mockito.mock(MultipartFile.class);
            when(enErreur.getOriginalFilename()).thenReturn("reponse.csv");
            when(enErreur.getInputStream()).thenThrow(new IOException("flux coupe"));

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, enErreur);

            assertThat(reponse.getItems()).isNull();
            assertThat(reponse.isAllLinesInRupture()).isFalse();
        }

        @Test
        @DisplayName("Excel : une erreur de lecture renvoie une reponse vide")
        void excelErreurDeLecture() throws IOException {
            referenceCommande(commande(12));
            MultipartFile enErreur = org.mockito.Mockito.mock(MultipartFile.class);
            when(enErreur.getOriginalFilename()).thenReturn("reponse.xlsx");
            when(enErreur.getInputStream()).thenThrow(new IOException("flux coupe"));

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, enErreur);

            assertThat(reponse.getItems()).isNull();
        }

        @Test
        @DisplayName("Excel : une cellule de code absente ou d'un type non gere est ignoree")
        void excelCelluleCodeIgnoree() throws IOException {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            referenceCommande(commande(12, ligne));

            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("reponse");
                // ligne 0 : pas de cellule en colonne 0 du tout
                sheet.createRow(0).createCell(1).setCellValue(5d);
                // ligne 1 : cellule booleenne, type non gere par extractStringCell
                Row row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue(true);
                row1.createCell(1).setCellValue(5d);
                workbook.write(out);
                MultipartFile fichier = new MockMultipartFile("file", "reponse.xlsx", "application/vnd.ms-excel", out.toByteArray());

                VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, CommandeModel.CIP_QTE, fichier);

                assertThat(reponse.getItems()).isEmpty();
                assertThat(reponse.getExtraItems()).isEmpty();
                assertThat(reponse.isAllLinesInRupture()).isTrue();
            }
        }

        @Test
        @DisplayName("Excel : la quantite confirmee est reportee sur la ligne correspondante")
        void excelQuantiteConfirmee() throws IOException {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.CIP_QTE,
                excel(new Object[][] { { "1111111", 7 } })
            );

            assertThat(reponse.getItems()).singleElement().extracting(VerificationResponseCommandeDTO.Item::getQuantitePriseEnCompte).isEqualTo(7);
            verify(orderLineService).updateOrderLineQuantityReceived(ligne, 7);
        }

        @Test
        @DisplayName("Excel : la ligne d'en-tete du format LABOREX est ignoree")
        void excelAvecEntete() throws IOException {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);
            Object[] entete = { "a", "b", "c", "CIP", "e", "f", "g", "QTE" };
            Object[] donnee = { "a", "b", "c", "1111111", "e", "f", "g", 7 };

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.LABOREX,
                excel(new Object[][] { entete, donnee })
            );

            assertThat(reponse.getItems()).hasSize(1);
            assertThat(reponse.getExtraItems()).isEmpty();
        }

        @Test
        @DisplayName("Excel : les lignes vides, sans quantite ou illisibles sont ignorees")
        void excelLignesIgnorees() throws IOException {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 10, 0);
            Commande commande = commande(12, ligne);
            referenceCommande(commande);

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(
                COMMANDE_ID,
                CommandeModel.CIP_QTE,
                excel(
                    new Object[][] {
                        { "", 5 }, // code vide
                        { "1111111", null }, // pas de cellule quantite
                        { "1111111", "pas un nombre" }, // quantite illisible
                        { 1111111L, 7 } // code numerique
                    }
                )
            );

            assertThat(reponse.getExtraItems()).singleElement().extracting(VerificationResponseCommandeDTO.Item::getCodeCip).isEqualTo("1111111.0");
        }
    }

    @Nested
    @DisplayName("commandes issues des suggestions")
    class DepuisSuggestion {

        private Suggestion suggestion(SuggestionLine... lignes) {
            Fournisseur fournisseur = new Fournisseur();
            fournisseur.setId(FOURNISSEUR_ID);
            Suggestion suggestion = new Suggestion();
            suggestion.setId(50);
            suggestion.setFournisseur(fournisseur);
            suggestion.setSuggestionLines(new LinkedHashSet<>(List.of(lignes)));
            return suggestion;
        }

        private SuggestionLine suggestionLine(int id, int quantity, FournisseurProduit fp) {
            SuggestionLine line = new SuggestionLine();
            line.setId(id);
            line.setQuantity(quantity);
            line.setFournisseurProduit(fp);
            return line;
        }

        @Test
        @DisplayName("createCommandeFromSuggestion reprend toutes les lignes de la suggestion")
        void depuisSuggestionComplete() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            SuggestionLine ligne = suggestionLine(1, 6, fp);
            OrderLine orderLine = orderLine(900, fp, 6, 0);
            when(orderLineService.buildOrderLine(ligne, FOURNISSEUR_ID)).thenReturn(orderLine);

            CommandeId id = service.createCommandeFromSuggestion(suggestion(ligne), null);

            assertThat(id.getId()).isEqualTo(77);
            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderLines()).containsExactly(orderLine);
            assertThat(captor.getValue().getGrossAmount()).isEqualTo(2400);
            assertThat(captor.getValue().getOrderAmount()).isEqualTo(4800);
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(FOURNISSEUR_ID);
        }

        @Test
        @DisplayName("createCommandeFromSuggestion accepte un fournisseur cible different")
        void depuisSuggestionAvecFournisseurCible() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            SuggestionLine ligne = suggestionLine(1, 6, fp);
            when(orderLineService.buildOrderLine(ligne, FOURNISSEUR_ID)).thenReturn(orderLine(900, fp, 6, 0));

            service.createCommandeFromSuggestion(suggestion(ligne), 9);

            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(9);
        }

        @Test
        @DisplayName("createCommandeFromSelection ne retient que les lignes aux quantites positives")
        void depuisSelection() {
            FournisseurProduit fp1 = fournisseurProduit(1, "1111111");
            FournisseurProduit fp2 = fournisseurProduit(2, "2222222");
            SuggestionLine ligne1 = suggestionLine(1, 6, fp1);
            SuggestionLine ligne2 = suggestionLine(2, 4, fp2);
            Suggestion suggestion = suggestion(ligne1, ligne2);
            OrderLine orderLine = orderLine(900, fp1, 6, 0);
            when(orderLineService.buildOrderLine(ligne1, FOURNISSEUR_ID)).thenReturn(orderLine);

            CommandeId id = service.createCommandeFromSelection(
                suggestion,
                List.of(new CommanderSelectionDTO.LigneSelection(1, 9), new CommanderSelectionDTO.LigneSelection(2, 0)),
                null
            );

            assertThat(id.getId()).isEqualTo(77);
            assertThat(orderLine.getQuantityRequested()).isEqualTo(9);
            assertThat(suggestion.getSuggestionLines()).containsExactly(ligne2);
            verify(suggestionLineRepository).deleteAll(List.of(ligne1));
            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            assertThat(captor.getValue().getGrossAmount()).isEqualTo(3600);
            assertThat(captor.getValue().getOrderLines()).containsExactly(orderLine);
        }

        @Test
        @DisplayName("createCommandeFromSelection accepte un fournisseur cible different")
        void depuisSelectionAvecFournisseurCible() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            SuggestionLine ligne = suggestionLine(1, 6, fp);
            when(orderLineService.buildOrderLine(ligne, FOURNISSEUR_ID)).thenReturn(orderLine(900, fp, 6, 0));

            service.createCommandeFromSelection(suggestion(ligne), List.of(new CommanderSelectionDTO.LigneSelection(1, 3)), 9);

            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            assertThat(captor.getValue().getFournisseur().getId()).isEqualTo(9);
        }

        @Test
        @DisplayName("importSuggestionIntoCommande cumule sur la ligne existante et supprime la suggestion videe")
        void importDansCommandeLigneExistante() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine existante = orderLine(900, fp, 5, 0);
            Commande commande = commande(12, existante);
            referenceCommande(commande);
            SuggestionLine ligne = suggestionLine(1, 6, fp);
            Suggestion suggestion = suggestion(ligne);
            when(suggestionRepository.findById(50)).thenReturn(Optional.of(suggestion));
            when(orderLineService.buildOrderLine(ligne, FOURNISSEUR_ID)).thenReturn(orderLine(901, fp, 6, 0));

            service.importSuggestionIntoCommande(COMMANDE_ID, 50);

            assertThat(existante.getQuantityRequested()).isEqualTo(11);
            assertThat(commande.getOrderLines()).containsExactly(existante);
            assertThat(commande.getGrossAmount()).isEqualTo(4400);
            verify(suggestionLineRepository).deleteAllInBatch(List.of(ligne));
            verify(suggestionRepository).delete(suggestion);
            verify(commandeRepository).save(commande);
        }

        @Test
        @DisplayName("importSuggestionLinesIntoCommande n'importe que les lignes selectionnees")
        void importLignesSelectionnees() {
            FournisseurProduit fp1 = fournisseurProduit(1, "1111111");
            FournisseurProduit fp2 = fournisseurProduit(2, "2222222");
            Commande commande = commande(12);
            referenceCommande(commande);
            SuggestionLine ligne1 = suggestionLine(1, 6, fp1);
            SuggestionLine ligne2 = suggestionLine(2, 4, fp2);
            Suggestion suggestion = suggestion(ligne1, ligne2);
            OrderLine nouvelle = orderLine(901, fp1, 6, 0);
            when(suggestionRepository.findById(50)).thenReturn(Optional.of(suggestion));
            when(orderLineService.buildOrderLine(ligne1, FOURNISSEUR_ID)).thenReturn(nouvelle);

            service.importSuggestionLinesIntoCommande(COMMANDE_ID, 50, List.of(1));

            assertThat(commande.getOrderLines()).containsExactly(nouvelle);
            assertThat(suggestion.getSuggestionLines()).containsExactly(ligne2);
            verify(suggestionRepository).save(suggestion);
            verify(suggestionRepository, never()).delete(any(Suggestion.class));
        }

        @Test
        @DisplayName("l'import est refuse si la commande n'est plus au statut demande")
        void importRefuseSurCommandeCloturee() {
            Commande commande = commande(12);
            commande.setOrderStatus(OrderStatut.CLOSED);
            referenceCommande(commande);

            assertThatThrownBy(() -> service.importSuggestionIntoCommande(COMMANDE_ID, 50))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("CLOSED");
        }
    }

    @Nested
    @DisplayName("commandes SEMOIS")
    class Semois {

        private static StockProduit stock(int quantity) {
            StockProduit stockProduit = new StockProduit();
            stockProduit.setQtyStock(quantity);
            stockProduit.setQtyUG(0);
            return stockProduit;
        }

        @Test
        @DisplayName("cree une commande a partir des lignes SEMOIS")
        void creation() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            fp.getProduit().getStockProduits().add(stock(15));
            OrderLine ligne = orderLine(900, fp, 6, 0);
            when(fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(10, FOURNISSEUR_ID)).thenReturn(Optional.of(fp));
            when(orderLineService.buildOrderLine(any(OrderLineDTO.class), eq(fp))).thenReturn(ligne);

            service.createCommandeFromSemoisLines(FOURNISSEUR_ID, List.of(new SemoisCommanderDTO.LigneSemois(10, FOURNISSEUR_ID, 6)));

            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            Commande commande = captor.getValue();
            assertThat(commande.getOrderLines()).containsExactly(ligne);
            assertThat(commande.getGrossAmount()).isEqualTo(2400);
            assertThat(commande.getOrderAmount()).isEqualTo(4800);
            assertThat(commande.getFinalAmount()).isEqualTo(4800);
            ArgumentCaptor<OrderLineDTO> dtoCaptor = ArgumentCaptor.forClass(OrderLineDTO.class);
            verify(orderLineService).buildOrderLine(dtoCaptor.capture(), eq(fp));
            assertThat(dtoCaptor.getValue().getTotalQuantity()).isEqualTo(15);
            assertThat(dtoCaptor.getValue().getQuantityRequested()).isEqualTo(6);
        }

        @Test
        @DisplayName("ne cree rien quand aucun produit fournisseur n'est trouve")
        void aucuneLigneValide() {
            when(fournisseurProduitRepository.findOneByProduitIdAndFournisseurId(anyInt(), anyInt())).thenReturn(Optional.empty());

            service.createCommandeFromSemoisLines(FOURNISSEUR_ID, List.of(new SemoisCommanderDTO.LigneSemois(10, FOURNISSEUR_ID, 6)));

            verify(commandeRepository, never()).save(any(Commande.class));
        }

        @Test
        @DisplayName("une liste vide ou nulle est ignoree")
        void listeVide() {
            service.createCommandeFromSemoisLines(FOURNISSEUR_ID, List.of());
            service.createCommandeFromSemoisLines(FOURNISSEUR_ID, null);

            verifyNoInteractions(commandeIdGeneratorService);
            verify(commandeRepository, never()).save(any(Commande.class));
        }
    }

    @Nested
    @DisplayName("changement de grossiste")
    class ChangementDeGrossiste {

        @Test
        @DisplayName("reaffecte chaque ligne au nouveau fournisseur et recalcule les montants")
        void changeGrossiste() {
            OrderLine ligne = orderLine(900, fournisseurProduit(1, "1111111"), 5, 0);
            Commande commande = commande(12, ligne);
            commande.setGrossAmount(999);
            referenceCommande(commande);
            CommandeDTO dto = new CommandeDTO();
            dto.setCommandeId(COMMANDE_ID);
            dto.setFournisseurId(9);

            service.changeGrossiste(dto);

            assertThat(commande.getFournisseur().getId()).isEqualTo(9);
            assertThat(commande.getGrossAmount()).isEqualTo(2000);
            assertThat(commande.getOrderAmount()).isEqualTo(4000);
            assertThat(commande.getUpdatedAt()).isNotNull();
            verify(orderLineService).changeFournisseurProduit(ligne, 9);
            verify(commandeRepository).save(commande);
        }
    }

    @Nested
    @DisplayName("statistiques de service fournisseur")
    class StatsService {

        @Test
        @DisplayName("arrondit le taux et le delai a une decimale")
        void statsRenseignees() {
            when(commandeRepository.fetchStatsService(eq(FOURNISSEUR_ID), any(LocalDate.class))).thenReturn(new Object[] { 92.47, 3.26 });

            FournisseurStatsServiceDTO stats = service.getStatsService(FOURNISSEUR_ID, 30);

            assertThat(stats.tauxService()).isEqualTo(92.5);
            assertThat(stats.delaiMoyen()).isEqualTo(3.3);
            assertThat(stats.periodeJours()).isEqualTo(30);
        }

        @Test
        @DisplayName("renvoie des statistiques nulles quand la requete ne remonte rien")
        void statsAbsentes() {
            when(commandeRepository.fetchStatsService(anyInt(), any(LocalDate.class))).thenReturn(null);

            assertThat(service.getStatsService(FOURNISSEUR_ID, 60)).isEqualTo(new FournisseurStatsServiceDTO(0.0, 0.0, 60));
        }

        @Test
        @DisplayName("renvoie des statistiques nulles quand la ligne est incomplete")
        void statsIncompletes() {
            when(commandeRepository.fetchStatsService(anyInt(), any(LocalDate.class))).thenReturn(new Object[] { 92.4 });

            assertThat(service.getStatsService(FOURNISSEUR_ID, 90).tauxService()).isZero();
        }

        @Test
        @DisplayName("renvoie des statistiques nulles quand le taux est absent")
        void tauxAbsent() {
            when(commandeRepository.fetchStatsService(anyInt(), any(LocalDate.class))).thenReturn(new Object[] { null, 3.0 });

            assertThat(service.getStatsService(FOURNISSEUR_ID, 90).delaiMoyen()).isZero();
        }
    }

    @Nested
    @DisplayName("creation d'un reliquat")
    class Reliquat {

        @Test
        @DisplayName("reprend les quantites manquantes des lignes partiellement servies")
        void reliquatCree() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine partielle = orderLine(900, fp, 10, 4);
            OrderLine servie = orderLine(901, fournisseurProduit(2, "2222222"), 5, 5);
            OrderLine jamaisRecue = orderLine(902, fournisseurProduit(3, "3333333"), 7, 0);
            jamaisRecue.setQuantityReceived(null);
            Commande source = commande(12, partielle, servie, jamaisRecue);
            referenceCommande(source);
            OrderLine nouvelle1 = orderLine(910, fp, 6, 0);
            OrderLine nouvelle2 = orderLine(911, jamaisRecue.getFournisseurProduit(), 7, 0);
            when(orderLineService.buildOrderLine(any(OrderLineDTO.class), eq(fp))).thenReturn(nouvelle1);
            when(orderLineService.buildOrderLine(any(OrderLineDTO.class), eq(jamaisRecue.getFournisseurProduit()))).thenReturn(nouvelle2);

            CommandeLiteDTO reliquat = service.createReliquat(COMMANDE_ID);

            assertThat(reliquat.getId()).isEqualTo(77);
            ArgumentCaptor<Commande> captor = ArgumentCaptor.forClass(Commande.class);
            verify(commandeRepository).save(captor.capture());
            Commande commande = captor.getValue();
            assertThat(commande.getReliquatDeCommandeId()).isEqualTo(12);
            assertThat(commande.getOrderLines()).containsExactly(nouvelle1, nouvelle2);
            // 6*400 + 7*400
            assertThat(commande.getGrossAmount()).isEqualTo(5200);
            assertThat(commande.getOrderAmount()).isEqualTo(10400);
            assertThat(commande.getFinalAmount()).isEqualTo(10400);
            ArgumentCaptor<OrderLineDTO> dtoCaptor = ArgumentCaptor.forClass(OrderLineDTO.class);
            verify(orderLineService, org.mockito.Mockito.times(2)).buildOrderLine(dtoCaptor.capture(), any(FournisseurProduit.class));
            assertThat(dtoCaptor.getAllValues()).extracting(OrderLineDTO::getQuantityRequested).containsExactly(6, 7);
            assertThat(dtoCaptor.getAllValues()).extracting(OrderLineDTO::getTotalQuantity).containsExactly(5, 5);
        }

        @Test
        @DisplayName("refuse le reliquat quand toutes les lignes sont servies")
        void aucuneLignePartielle() {
            Commande source = commande(12, orderLine(900, fournisseurProduit(1, "1111111"), 5, 5));
            referenceCommande(source);

            assertThatThrownBy(() -> service.createReliquat(COMMANDE_ID))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Aucune ligne partielle");
        }

        @Test
        @DisplayName("un stock initial absent est ramene a zero")
        void stockInitialAbsent() {
            FournisseurProduit fp = fournisseurProduit(1, "1111111");
            OrderLine partielle = orderLine(900, fp, 10, 4);
            partielle.setInitStock(null);
            referenceCommande(commande(12, partielle));
            when(orderLineService.buildOrderLine(any(OrderLineDTO.class), eq(fp))).thenReturn(orderLine(910, fp, 6, 0));

            service.createReliquat(COMMANDE_ID);

            ArgumentCaptor<OrderLineDTO> dtoCaptor = ArgumentCaptor.forClass(OrderLineDTO.class);
            verify(orderLineService).buildOrderLine(dtoCaptor.capture(), eq(fp));
            assertThat(dtoCaptor.getValue().getTotalQuantity()).isZero();
        }
    }

    @Nested
    @DisplayName("strategies d'import par modele")
    class Strategies {

        @ParameterizedTest(name = "{0}")
        @EnumSource(CommandeModel.class)
        @DisplayName("chaque modele dispose d'un mapping de colonnes pour la reponse grossiste")
        void chaqueModeleEstMappe(CommandeModel model) throws IOException {
            Commande commande = commande(12);
            referenceCommande(commande);
            MultipartFile fichier = new MockMultipartFile(
                "file",
                "reponse.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelVide()
            );

            VerificationResponseCommandeDTO reponse = service.importerReponseCommande(COMMANDE_ID, model, fichier);

            assertThat(reponse.isAllLinesInRupture()).isTrue();
        }

        private byte[] excelVide() throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.createSheet("vide");
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }
}
