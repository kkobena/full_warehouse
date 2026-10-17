package com.kobe.warehouse.service.etiquette.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.ordonnance.Ordonnance;
import com.kobe.warehouse.domain.ordonnance.OrdonnanceLigne;
import com.kobe.warehouse.domain.ordonnance.Prescripteur;
import com.kobe.warehouse.repository.OrdonnanceRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.etiquette.EtiquettePreparationDTO;
import com.kobe.warehouse.service.etiquette.zpl.impl.ZplEtiquetteServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Les trois usages du lot 3. Le contenu d'une posologie et d'une préparation ne se relit nulle
 * part après coup : si le gabarit l'oublie, l'erreur part chez le patient.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EtiquetteUsageServiceImpl")
class EtiquetteUsageServiceImplTest {

    private static final double POINTS_PAR_MM = 72.0 / 25.4;
    private static final LocalDate PRESCRIPTION = LocalDate.of(2026, 10, 16);

    @Mock
    private StorageService storageService;

    @Mock
    private OrdonnanceRepository ordonnanceRepository;

    private EtiquetteUsageServiceImpl service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine moteur = new SpringTemplateEngine();
        moteur.setTemplateResolver(resolver);

        Magasin magasin = new Magasin();
        magasin.setId(1);
        magasin.setName("pharmacie du plateau");
        AppUser user = new AppUser();
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);

        service = new EtiquetteUsageServiceImpl(moteur, storageService, ordonnanceRepository, new ZplEtiquetteServiceImpl());
    }

    private static Produit produit() {
        Produit produit = new Produit();
        produit.setId(500);
        produit.setLibelle("DOLIPRANE 1000MG");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setCodeCip("3794609");
        fp.setPrixUni(1500);
        fp.setProduit(produit);
        produit.setFournisseurProduitPrincipal(fp);
        return produit;
    }

    private static OrdonnanceLigne ligne(int id, String posologie, Integer dureeJours) {
        OrdonnanceLigne ligne = new OrdonnanceLigne()
            .setRang(id)
            .setProduit(produit())
            .setPosologie(posologie)
            .setDureeJours(dureeJours)
            .setQuantitePrescrite(1);
        ReflectionTestUtils.setField(ligne, "id", id);
        return ligne;
    }

    private Ordonnance ordonnance(OrdonnanceLigne... lignes) {
        Customer customer = new Customer();
        customer.setFirstName("Awa");
        customer.setLastName("KOUASSI");
        Ordonnance ordonnance = new Ordonnance()
            .setCustomer(customer)
            .setPrescripteur(new Prescripteur().setNom("TRAORE").setPrenom("Issa").setSpecialite("cardiologue"))
            .setDatePrescription(PRESCRIPTION);
        for (OrdonnanceLigne ligne : lignes) {
            ordonnance.ajouterLigne(ligne);
        }
        when(ordonnanceRepository.findById(7)).thenReturn(Optional.of(ordonnance));
        return ordonnance;
    }

    /** Le gabarit PDF ne se relit que dans le PDF : un contenu oublié y serait invisible. */
    private static String texteDuPdf(byte[] pdf) throws Exception {
        PdfReader lecteur = new PdfReader(pdf);
        try {
            PdfTextExtractor extracteur = new PdfTextExtractor(lecteur);
            StringBuilder texte = new StringBuilder();
            for (int page = 1; page <= lecteur.getNumberOfPages(); page++) {
                texte.append(extracteur.getTextFromPage(page)).append('\n');
            }
            return texte.toString();
        } finally {
            lecteur.close();
        }
    }

    private static EtiquettePreparationDTO preparation() {
        return new EtiquettePreparationDTO(
            "Pommade salicylée 5 %",
            "Acide salicylique 5 g, vaseline qsp 100 g",
            "KOUASSI Awa",
            "Dr TRAORE Issa",
            "Appliquer matin et soir",
            LocalDate.of(2026, 10, 16),
            LocalDate.of(2027, 1, 16),
            "À usage externe"
        );
    }

    @Nested
    @DisplayName("reglette")
    class Reglette {

        @Test
        @DisplayName("le gabarit PDF met le prix et le libelle sur la planche")
        void contenuDuPdf() throws Exception {
            String texte = texteDuPdf(service.imprimerReglette(produit(), 4, 1, FormatEtiquette.A4_21_63X38));

            assertThat(texte).contains("DOLIPRANE 1000MG");
            assertThat(texte).contains("1 500 CFA");
            assertThat(texte).contains("3794609");
        }

        @Test
        @DisplayName("sur rouleau, une page par reglette a la taille de l etiquette")
        void surRouleau() throws Exception {
            byte[] pdf = service.imprimerReglette(produit(), 3, 1, FormatEtiquette.ROULEAU_58X40);

            PdfReader lecteur = new PdfReader(pdf);
            try {
                assertThat(lecteur.getNumberOfPages()).isEqualTo(3);
                Rectangle page = lecteur.getPageSize(1);
                assertThat((double) page.getWidth()).isCloseTo(58.0 * POINTS_PAR_MM, within(1.0));
                assertThat((double) page.getHeight()).isCloseTo(40.0 * POINTS_PAR_MM, within(1.0));
            } finally {
                lecteur.close();
            }
        }

        @Test
        @DisplayName("porte le prix et le code, jamais de lot")
        void contenu() {
            String zpl = service.genererZplReglette(produit(), 1, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("1 500 CFA");
            assertThat(zpl).contains("3794609");
            assertThat(zpl).doesNotContain("Lot ");
        }
    }

    @Nested
    @DisplayName("posologie")
    class Posologie {

        @Test
        @DisplayName("reprend patient, prescripteur et instructions de l ordonnance")
        void contenuDeLOrdonnance() {
            ordonnance(ligne(1, "1 comprimé matin et soir", 7));

            String zpl = service.genererZplPosologie(7, null, 1, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            // Rien n'est ressaisi : tout vient de l'ordonnance déjà enregistrée.
            assertThat(zpl).contains("Awa KOUASSI");
            assertThat(zpl).contains("Dr Issa TRAORE — cardiologue");
            assertThat(zpl).contains("1 comprimé matin et soir");
            assertThat(zpl).contains("Pendant 7 jours");
            assertThat(zpl).contains("DOLIPRANE 1000MG");
        }

        @Test
        @DisplayName("le gabarit PDF ecrit bien ce que l ordonnance porte")
        void contenuDuPdf() throws Exception {
            ordonnance(ligne(1, "1 comprimé matin et soir", 7));

            String texte = texteDuPdf(service.imprimerPosologie(7, null, 1, 1, FormatEtiquette.A4_14_99X38));

            assertThat(texte).contains("Awa KOUASSI");
            assertThat(texte).contains("DOLIPRANE 1000MG");
            assertThat(texte).contains("1 comprimé matin et soir");
            assertThat(texte).contains("Pendant 7 jours");
            // Une posologie ne porte pas de prix.
            assertThat(texte).doesNotContain("CFA");
        }

        @Test
        @DisplayName("une etiquette par ligne de l ordonnance")
        void uneEtiquetteParLigne() throws Exception {
            ordonnance(ligne(1, "1 comprimé matin", 7), ligne(2, "1 sachet le soir", 5));

            byte[] pdf = service.imprimerPosologie(7, null, 1, 1, FormatEtiquette.ROULEAU_58X40);

            PdfReader lecteur = new PdfReader(pdf);
            try {
                assertThat(lecteur.getNumberOfPages()).isEqualTo(2);
            } finally {
                lecteur.close();
            }
        }

        @Test
        @DisplayName("une ligne precise n imprime qu elle")
        void uneSeuleLigne() {
            ordonnance(ligne(1, "1 comprimé matin", 7), ligne(2, "1 sachet le soir", 5));

            String zpl = service.genererZplPosologie(7, 2, 1, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("1 sachet le soir");
            assertThat(zpl).doesNotContain("1 comprimé matin");
        }

        @Test
        @DisplayName("une ordonnance introuvable le dit")
        void ordonnanceIntrouvable() {
            when(ordonnanceRepository.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.imprimerPosologie(404, null, 1, 1, FormatEtiquette.ROULEAU_58X40))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("404");
        }

        @Test
        @DisplayName("une ligne sans duree n invente pas de duree")
        void sansDuree() {
            ordonnance(ligne(1, "1 comprimé au besoin", null));

            String zpl = service.genererZplPosologie(7, null, 1, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).doesNotContain("Pendant");
        }
    }

    @Nested
    @DisplayName("preparation magistrale")
    class Preparation {

        @Test
        @DisplayName("porte la composition, les dates et la mention")
        void contenu() {
            String zpl = service.genererZplPreparation(
                preparation(),
                1,
                FormatEtiquette.ROULEAU_58X40,
                ResolutionEtiquette.DPI_203
            );

            assertThat(zpl).contains("Acide salicylique 5 g, vaseline qsp 100 g");
            assertThat(zpl).contains("Préparé le 16/10/2026");
            assertThat(zpl).contains("À utiliser avant le 16/01/2027");
            assertThat(zpl).contains("À usage externe");
        }

        @Test
        @DisplayName("sans date de preparation, c est celle du jour")
        void dateDuJour() {
            EtiquettePreparationDTO saisie = new EtiquettePreparationDTO(
                "Solution",
                "Eau purifiée qsp 100 ml",
                null,
                null,
                null,
                null,
                null,
                null
            );

            String zpl = service.genererZplPreparation(saisie, 1, FormatEtiquette.ROULEAU_58X40, ResolutionEtiquette.DPI_203);

            assertThat(zpl).contains("Préparé le " + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            // Pas de péremption saisie : l'étiquette n'en invente pas.
            assertThat(zpl).doesNotContain("À utiliser avant");
        }

        @Test
        @DisplayName("le gabarit PDF ecrit la composition et les deux dates")
        void contenuDuPdf() throws Exception {
            String texte = texteDuPdf(service.imprimerPreparation(preparation(), 2, 1, FormatEtiquette.A4_14_99X38));

            assertThat(texte).contains("Pommade salicylée 5 %");
            assertThat(texte).contains("Acide salicylique");
            assertThat(texte).contains("Préparé le 16/10/2026");
            assertThat(texte).contains("À utiliser avant le 16/01/2027");
            assertThat(texte).contains("À usage externe");
        }
    }

    @Nested
    @DisplayName("case de depart")
    class CaseDeDepart {

        @Test
        @DisplayName("une planche entamee se reprend, un rouleau l ignore")
        void casesConsommees() throws Exception {
            byte[] rouleau = service.imprimerReglette(produit(), 1, 12, FormatEtiquette.ROULEAU_58X40);

            PdfReader lecteur = new PdfReader(rouleau);
            try {
                // Sans normalisation, onze étiquettes blanches auraient défilé avant la bonne.
                assertThat(lecteur.getNumberOfPages()).isEqualTo(1);
            } finally {
                lecteur.close();
            }
        }
    }
}
