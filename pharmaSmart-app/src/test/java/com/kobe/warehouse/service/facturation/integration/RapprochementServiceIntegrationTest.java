package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Banque;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.facturation.dto.EtatRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.LigneRapprochementDto;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.facturation.service.RapprochementService} sur un vrai
 * PostgreSQL.
 *
 * <p>L'état de rapprochement confronte ce qui a été facturé à ce qui a été encaissé, organisme par
 * organisme. Il est bâti sur deux requêtes Criteria — les factures de la période, puis les
 * règlements de chaque facture — puis regroupé et totalisé en mémoire. L'échéance de chaque facture
 * se calcule à partir du délai de règlement de l'organisme, lu en base, avec repli sur le
 * paramétrage : c'est un croisement que seule une vraie base met à l'épreuve.
 */
@DisplayName("RapprochementService — état de rapprochement sur PostgreSQL")
class RapprochementServiceIntegrationTest extends AbstractFacturationIntegrationTest {

    @Test
    @DisplayName("L'état regroupe les factures par organisme et totalise facturé, réglé et écart")
    void etatParOrganisme() {
        TiersPayant organisme = tiersPayant("CNAM RAPPRO");
        factureReglee(organisme, 100_000, 40_000);
        factureReglee(organisme, 60_000, 60_000);
        factureReglee(tiersPayant("MUGEF RAPPRO"), 30_000, 0);
        viderLeCache();

        List<EtatRapprochementDto> etats = services.rapprochementService.getEtatRapprochement(params(null), page()).getContent();

        assertEquals(2, etats.size(), "un organisme, un état");
        EtatRapprochementDto etat = etats.stream().filter(e -> e.tiersPayantName().equals("CNAM RAPPRO")).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.valueOf(160_000).compareTo(etat.totalFacture()));
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(etat.totalRegle()));
        assertEquals(0, BigDecimal.valueOf(60_000).compareTo(etat.ecartTotal()), "ce qui reste à recouvrer");
        assertEquals(2, etat.lignes().size());
    }

    @Test
    @DisplayName("L'écart d'une facture est ce qu'elle reste à devoir")
    void ecartParFacture() {
        TiersPayant organisme = tiersPayant("ECART");
        factureReglee(organisme, 80_000, 30_000);
        viderLeCache();

        LigneRapprochementDto ligne = services.rapprochementService
            .getEtatRapprochement(params(null), page())
            .getContent()
            .getFirst()
            .lignes()
            .getFirst();

        assertEquals(0, BigDecimal.valueOf(80_000).compareTo(ligne.montantFacture()));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(ligne.montantRegle()));
        assertEquals(0, BigDecimal.valueOf(50_000).compareTo(ligne.ecart()));
        assertEquals(InvoiceStatut.PARTIALLY_PAID, ligne.statut());
    }

    @Test
    @DisplayName("L'échéance suit le délai de règlement propre à l'organisme")
    void echeanceSelonLeDelaiDeLOrganisme() {
        TiersPayant organisme = tiersPayant("DELAI 45");
        organisme.setDelaiReglement(45);
        factureReglee(organisme, 50_000, 0);
        viderLeCache();

        LigneRapprochementDto ligne = services.rapprochementService
            .getEtatRapprochement(params(null), page())
            .getContent()
            .getFirst()
            .lignes()
            .getFirst();

        assertEquals(LocalDate.now().plusDays(45), ligne.echeance());
    }

    @Test
    @DisplayName("Un organisme sans délai saisi hérite des 30 jours par défaut de la colonne")
    void echeanceParDefaut() {
        // Le repli du service sur le paramétrage de l'officine est inatteignable : la colonne
        // delai_reglement est NOT NULL DEFAULT 30, un organisme porte donc toujours un délai.
        TiersPayant organisme = tiersPayant("SANS DELAI");
        factureReglee(organisme, 50_000, 0);
        viderLeCache();

        LigneRapprochementDto ligne = services.rapprochementService
            .getEtatRapprochement(params(null), page())
            .getContent()
            .getFirst()
            .lignes()
            .getFirst();

        assertEquals(LocalDate.now().plusDays(30), ligne.echeance());
    }

    @Test
    @DisplayName("Chaque facture porte le détail de ses règlements")
    void detailDesReglements() {
        TiersPayant organisme = tiersPayant("REGLEMENTS");
        FactureTiersPayant facture = factureReglee(organisme, 100_000, 70_000);
        reglement(facture, 40_000, "ACOMPTE 1");
        reglement(facture, 30_000, "ACOMPTE 2");
        viderLeCache();

        LigneRapprochementDto ligne = services.rapprochementService
            .getEtatRapprochement(params(null), page())
            .getContent()
            .getFirst()
            .lignes()
            .getFirst();

        assertEquals(2, ligne.reglements().size());
        assertEquals(70_000, ligne.reglements().stream().mapToInt(r -> r.paidAmount()).sum());
        assertTrue(ligne.reglements().stream().allMatch(r -> r.paymentMode().equals(ModePaimentCode.VIREMENT.name())));
        assertTrue(ligne.reglements().stream().anyMatch(r -> "ACOMPTE 1".equals(r.commentaire())));
        assertTrue(ligne.reglements().stream().allMatch(r -> "SGBCI".equals(r.banque())));
    }

    @Test
    @DisplayName("Le filtre par organisme ne retient que ses factures")
    void filtreParOrganisme() {
        TiersPayant vise = tiersPayant("VISE RAPPRO");
        factureReglee(vise, 20_000, 0);
        factureReglee(tiersPayant("AUTRE RAPPRO"), 30_000, 0);
        viderLeCache();

        List<EtatRapprochementDto> etats = services.rapprochementService
            .getEtatRapprochement(params(vise.getId()), page())
            .getContent();

        assertEquals(1, etats.size());
        assertEquals("VISE RAPPRO", etats.getFirst().tiersPayantName());
    }

    @Test
    @DisplayName("Le filtre par statut ne retient que les factures dans cet état")
    void filtreParStatut() {
        TiersPayant organisme = tiersPayant("STATUT");
        factureReglee(organisme, 40_000, 40_000);
        factureReglee(organisme, 50_000, 0);
        viderLeCache();

        List<EtatRapprochementDto> etats = services.rapprochementService
            .getEtatRapprochement(
                new RapprochementParams(null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), Set.of(InvoiceStatut.PAID)),
                page()
            )
            .getContent();

        assertEquals(1, etats.getFirst().lignes().size());
        assertEquals(InvoiceStatut.PAID, etats.getFirst().lignes().getFirst().statut());
    }

    @Test
    @DisplayName("Une facture hors période ne remonte pas")
    void horsPeriode() {
        TiersPayant organisme = tiersPayant("HORS PERIODE");
        factureReglee(organisme, 40_000, 0);
        viderLeCache();

        List<EtatRapprochementDto> etats = services.rapprochementService
            .getEtatRapprochement(
                new RapprochementParams(null, LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5), Set.of()),
                page()
            )
            .getContent();

        assertTrue(etats.isEmpty());
    }

    @Test
    @DisplayName("La pagination découpe les organismes sans en perdre le compte")
    void pagination() {
        factureReglee(tiersPayant("PAGE A"), 10_000, 0);
        factureReglee(tiersPayant("PAGE B"), 20_000, 0);
        factureReglee(tiersPayant("PAGE C"), 30_000, 0);
        viderLeCache();

        var premierePage = services.rapprochementService.getEtatRapprochement(params(null), PageRequest.of(0, 2));
        var secondePage = services.rapprochementService.getEtatRapprochement(params(null), PageRequest.of(1, 2));

        assertEquals(2, premierePage.getContent().size());
        assertEquals(1, secondePage.getContent().size());
        assertEquals(3, premierePage.getTotalElements());
    }

    @Test
    @DisplayName("Une page au-delà du dernier organisme est vide plutôt qu'en erreur")
    void pageAuDela() {
        factureReglee(tiersPayant("UNE SEULE"), 10_000, 0);
        viderLeCache();

        var page = services.rapprochementService.getEtatRapprochement(params(null), PageRequest.of(5, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("L'export Excel rend un classeur non vide")
    void exportExcel() {
        factureReglee(tiersPayant("EXCEL"), 10_000, 5_000);
        viderLeCache();

        byte[] classeur = services.rapprochementService.exportExcel(params(null));

        assertTrue(classeur.length > 0);
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private RapprochementParams params(Integer tiersPayantId) {
        return new RapprochementParams(tiersPayantId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), Set.of());
    }

    private FactureTiersPayant factureReglee(TiersPayant organisme, int montantNet, int montantRegle) {
        FactureTiersPayant facture = facture(organisme, false, List.of());
        facture.setMontantTtc(BigDecimal.valueOf(montantNet));
        facture.setMontantNet(BigDecimal.valueOf(montantNet));
        facture.setMontantRegle(montantRegle);
        facture.setStatut(montantRegle == 0 ? InvoiceStatut.NOT_PAID : montantRegle < montantNet ? InvoiceStatut.PARTIALLY_PAID : InvoiceStatut.PAID);
        em.flush();
        return facture;
    }

    private InvoicePayment reglement(FactureTiersPayant facture, int montant, String commentaire) {
        Banque banque = new Banque().setNom("SGBCI").setCode("SG");
        em.persist(banque);

        InvoicePayment paiement = new InvoicePayment();
        paiement.setId(services.transactionIdGeneratorService.nextId());
        paiement.setTransactionDate(LocalDate.now());
        paiement.setFactureTiersPayant(facture);
        paiement.setCashRegister(em.find(CashRegister.class, caisse.getId()));
        paiement.setPaymentMode(new PaymentMode().code(ModePaimentCode.VIREMENT.name()));
        paiement.setBanque(banque);
        paiement.setMontantVerse(montant);
        paiement.setPaidAmount(montant);
        paiement.setReelAmount(montant);
        paiement.setExpectedAmount(montant);
        paiement.setCommentaire(commentaire);
        paiement.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT);
        paiement.setTransactionNumber(unique("TR"));
        em.persist(paiement);
        em.flush();
        return paiement;
    }
}
