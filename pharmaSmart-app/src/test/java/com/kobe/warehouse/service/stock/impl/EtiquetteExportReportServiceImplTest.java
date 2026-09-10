package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.report.Constant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EtiquetteExportReportServiceImpl")
class EtiquetteExportReportServiceImplTest {

    private static final String HTML = "<html><head><title>Etiquettes</title></head><body><p>etiquettes</p></body></html>";

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private StorageService storageService;

    private EtiquetteExportReportServiceImpl service;

    private Magasin magasin;

    @BeforeEach
    void setUp() {
        service = new EtiquetteExportReportServiceImpl(fileStorageProperties, templateEngine, storageService);
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setName("pharmacie du plateau");
        magasin.setFullName("PHARMACIE DU PLATEAU");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML);
    }

    private static String aujourdhui() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static FournisseurProduit fournisseurProduit(String codeCip, int prixUni) {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setCodeCip(codeCip);
        fp.setPrixUni(prixUni);
        fp.setProduit(produit);
        return fp;
    }

    private static OrderLine orderLine(String codeCip, int prixUni) {
        OrderLine line = new OrderLine();
        line.setId(900);
        line.setOrderDate(LocalDate.now());
        line.setFournisseurProduit(fournisseurProduit(codeCip, prixUni));
        return line;
    }

    private static Produit produit(String codeCip, int prixUni) {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        produit.setFournisseurProduitPrincipal(fournisseurProduit(codeCip, prixUni));
        return produit;
    }

    @SuppressWarnings("unchecked")
    private List<List<EtiquetteDTO>> paquetsRendus() {
        return (List<List<EtiquetteDTO>>) service.getParameters().get(Constant.ITEMS);
    }

    private List<EtiquetteDTO> etiquettesRendues() {
        return paquetsRendus().stream().flatMap(List::stream).toList();
    }

    @Nested
    @DisplayName("export depuis un bon de livraison")
    class ExportDepuisBonDeLivraison {

        @Test
        @DisplayName("produit un PDF")
        void produitUnPdf() {
            byte[] pdf = service.export(List.of(orderLine("1234567", 1500)), 1);

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("mappe le code, le prix, le libelle et la date de chaque etiquette")
        void mappeLesEtiquettes() {
            service.export(List.of(orderLine("1234567", 1500)), 1);

            EtiquetteDTO dto = etiquettesRendues().getFirst();
            assertThat(dto.getCode()).isEqualTo("1234567");
            assertThat(dto.getPrix()).endsWith("CFA");
            assertThat(dto.getLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getDate()).isEqualTo(aujourdhui());
            assertThat(dto.isPrint()).isTrue();
        }

        @Test
        @DisplayName("met la raison sociale de l officine en majuscules")
        void raisonSocialeEnMajuscules() {
            service.export(List.of(orderLine("1234567", 1500)), 1);

            assertThat(etiquettesRendues().getFirst().getMagasin()).isEqualTo("PHARMACIE DU PLATEAU");
        }

        @Test
        @DisplayName("ecarte les lignes sans code CIP")
        void ecarteLesLignesSansCodeCip() {
            service.export(List.of(orderLine("1234567", 1500), orderLine(null, 900), orderLine("", 900)), 1);

            assertThat(etiquettesRendues()).hasSize(1);
        }

        @ParameterizedTest(name = "un depart a {0} n insere aucune etiquette blanche")
        @ValueSource(ints = { 0, 1 })
        void sansEtiquetteBlanche(int startAt) {
            service.export(List.of(orderLine("1234567", 1500)), startAt);

            assertThat(etiquettesRendues()).hasSize(1);
            assertThat(etiquettesRendues()).allSatisfy(e -> assertThat(e.isPrint()).isTrue());
        }

        @Test
        @DisplayName("un depart au-dela de la premiere case insere des etiquettes blanches")
        void avecEtiquettesBlanches() {
            service.export(List.of(orderLine("1234567", 1500)), 3);

            List<EtiquetteDTO> etiquettes = etiquettesRendues();
            assertThat(etiquettes).hasSize(4);
            assertThat(etiquettes.subList(0, 3)).allSatisfy(e -> assertThat(e.isPrint()).isFalse());
            assertThat(etiquettes.getLast().isPrint()).isTrue();
        }

        @Test
        @DisplayName("decoupe les etiquettes en rangees de cinq")
        void decoupeEnRangeesDeCinq() {
            List<OrderLine> lignes = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) {
                lignes.add(orderLine(String.format("%07d", i), 1500));
            }

            service.export(lignes, 1);

            assertThat(paquetsRendus()).hasSize(3);
            assertThat(paquetsRendus().getFirst()).hasSize(5);
            assertThat(paquetsRendus().getLast()).hasSize(2);
        }

        @Test
        @DisplayName("transmet la case de depart au gabarit")
        void transmetLaCaseDeDepart() {
            service.export(List.of(orderLine("1234567", 1500)), 4);

            assertThat(service.getParameters()).containsEntry(Constant.ETIQUETES_BEGIN, 4);
            assertThat(service.getParameters()).containsEntry(Constant.MAGASIN, magasin);
        }

        @Test
        @DisplayName("un bon sans ligne produit un PDF vide de contenu")
        void sansLigne() {
            byte[] pdf = service.export(List.of(), 1);

            assertThat(pdf).isNotEmpty();
            assertThat(etiquettesRendues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("exportForProduit")
    class ExportForProduit {

        @Test
        @DisplayName("repete l etiquette autant de fois que demande")
        void repeteLEtiquette() {
            byte[] pdf = service.exportForProduit(produit("1234567", 1500), 3, 1);

            assertThat(pdf).isNotEmpty();
            assertThat(etiquettesRendues()).hasSize(3);
        }

        @Test
        @DisplayName("mappe le produit et son fournisseur principal")
        void mappeLeProduit() {
            service.exportForProduit(produit("1234567", 1500), 1, 1);

            EtiquetteDTO dto = etiquettesRendues().getFirst();
            assertThat(dto.getCode()).isEqualTo("1234567");
            assertThat(dto.getLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(dto.getMagasin()).isEqualTo("PHARMACIE DU PLATEAU");
            assertThat(dto.getDate()).isEqualTo(aujourdhui());
            assertThat(dto.getPrix()).endsWith("CFA");
            assertThat(dto.isPrint()).isTrue();
        }

        @Test
        @DisplayName("insere les cases deja consommees de la planche")
        void inserteLesCasesConsommees() {
            service.exportForProduit(produit("1234567", 1500), 2, 3);

            List<EtiquetteDTO> etiquettes = etiquettesRendues();
            assertThat(etiquettes).hasSize(4);
            assertThat(etiquettes.subList(0, 2)).allSatisfy(e -> assertThat(e.isPrint()).isFalse());
        }

        @ParameterizedTest(name = "un depart a {0} ne consomme aucune case")
        @ValueSource(ints = { 0, 1 })
        void sansCaseConsommee(int startAt) {
            service.exportForProduit(produit("1234567", 1500), 2, startAt);

            assertThat(etiquettesRendues()).hasSize(2);
        }

        @Test
        @DisplayName("une quantite nulle ne produit aucune etiquette")
        void quantiteNulle() {
            byte[] pdf = service.exportForProduit(produit("1234567", 1500), 0, 1);

            assertThat(pdf).isNotEmpty();
            assertThat(etiquettesRendues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("contrat de rapport")
    class ContratDeRapport {

        @Test
        @DisplayName("le nom de fichier genere est fixe")
        void nomDeFichier() {
            assertThat(service.getGenerateFileName()).isEqualTo("etiquettes");
        }

        @Test
        @DisplayName("les etiquettes ne sont pas paginees")
        void sansPagination() {
            assertThat(service.getMaxiRowCount()).isZero();
        }

        @Test
        @DisplayName("les elements exposes sont les etiquettes du dernier rendu")
        void elementsExposes() {
            service.exportForProduit(produit("1234567", 1500), 2, 1);

            assertThat(service.getItems()).hasSize(2);
        }

        @Test
        @DisplayName("le rendu avec contexte y recopie les parametres")
        void renduAvecContexte() {
            service.exportForProduit(produit("1234567", 1500), 1, 1);
            Context context = new Context();

            assertThat(service.getTemplateAsHtml(context)).isEqualTo(HTML);
            assertThat(context.getVariable(Constant.MAGASIN)).isSameAs(magasin);
        }

        @Test
        @DisplayName("le rendu sans contexte explicite passe par les variables communes")
        void renduSansContexte() {
            service.exportForProduit(produit("1234567", 1500), 1, 1);

            assertThat(service.getTemplateAsHtml()).isEqualTo(HTML);
        }
    }
}
