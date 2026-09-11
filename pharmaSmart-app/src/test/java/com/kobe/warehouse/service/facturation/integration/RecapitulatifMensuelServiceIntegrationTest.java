package com.kobe.warehouse.service.facturation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelDto;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelService} sur un vrai
 * PostgreSQL.
 *
 * <p>Le récapitulatif est un relevé de compte : pour chaque organisme et pour un mois donné, ce
 * qu'il devait déjà, ce qui lui a été facturé, ce qu'il a réglé, et ce qu'il doit au total. Le
 * report à nouveau est le point délicat — il se calcule par une seconde requête sur <em>toutes</em>
 * les factures antérieures encore dues, indépendamment du mois affiché. Un report faux fausse le
 * cumul, et rien dans le mois lui-même ne le trahit : seule une base portant plusieurs mois le
 * montre.
 */
@DisplayName("RecapitulatifMensuelService — relevé mensuel par organisme sur PostgreSQL")
class RecapitulatifMensuelServiceIntegrationTest extends AbstractFacturationIntegrationTest {

    private static final YearMonth MOIS = YearMonth.from(LocalDate.now());

    @Test
    @DisplayName("Le relevé totalise le facturé, le réglé et le solde du mois par organisme")
    void relevePourUnOrganisme() {
        TiersPayant organisme = tiersPayant("CNAM RECAP");
        factureDatee(organisme, 100_000, 40_000, InvoiceStatut.PARTIALLY_PAID, ceMoisCi(3));
        factureDatee(organisme, 60_000, 60_000, InvoiceStatut.PAID, ceMoisCi(12));
        viderLeCache();

        RecapitulatifMensuelDto releve = releveDuMois().getContent().getFirst();

        assertEquals(organisme.getFullName(), releve.tiersPayantName());
        assertEquals(MOIS, releve.periode());
        assertEquals(0, BigDecimal.valueOf(160_000).compareTo(releve.totalFacture()));
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(releve.totalRegle()));
        assertEquals(0, BigDecimal.valueOf(60_000).compareTo(releve.soldeActuel()));
        assertEquals(2, releve.nombreFactures());
        assertEquals(1, releve.nombreImpayees(), "seule la facture partiellement réglée reste due");
    }

    @Test
    @DisplayName("Chaque facture du mois apparaît en ligne avec son restant dû")
    void lignesDuReleve() {
        TiersPayant organisme = tiersPayant("LIGNES RECAP");
        FactureTiersPayant facture = factureDatee(organisme, 80_000, 30_000, InvoiceStatut.PARTIALLY_PAID, ceMoisCi(5));
        viderLeCache();

        RecapitulatifMensuelRow ligne = releveDuMois().getContent().getFirst().lignes().getFirst();

        assertEquals(facture.getNumFacture(), ligne.numFacture());
        assertEquals(ceMoisCi(5), ligne.invoiceDate());
        assertEquals(0, BigDecimal.valueOf(80_000).compareTo(ligne.montantNet()));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(ligne.montantRegle()));
        assertEquals(0, BigDecimal.valueOf(50_000).compareTo(ligne.restantDu()));
        assertEquals(InvoiceStatut.PARTIALLY_PAID, ligne.statut());
    }

    @Test
    @DisplayName("L'échéance de chaque ligne suit le délai de règlement de l'organisme")
    void echeanceSelonLeDelai() {
        TiersPayant organisme = tiersPayant("DELAI RECAP");
        organisme.setDelaiReglement(45);
        factureDatee(organisme, 50_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(2));
        viderLeCache();

        RecapitulatifMensuelRow ligne = releveDuMois().getContent().getFirst().lignes().getFirst();

        assertEquals(ceMoisCi(2).plusDays(45), ligne.echeance());
    }

    @Test
    @DisplayName("Le report à nouveau reprend ce qui restait dû des mois précédents")
    void reportANouveau() {
        TiersPayant organisme = tiersPayant("REPORT RECAP");
        // Mois précédent : 90 000 facturés, 20 000 réglés → 70 000 encore dus.
        factureDatee(organisme, 90_000, 20_000, InvoiceStatut.PARTIALLY_PAID, moisPrecedent(10));
        factureDatee(organisme, 40_000, 10_000, InvoiceStatut.PARTIALLY_PAID, ceMoisCi(4));
        viderLeCache();

        RecapitulatifMensuelDto releve = releveDuMois().getContent().getFirst();

        assertEquals(0, BigDecimal.valueOf(70_000).compareTo(releve.soldePrecedent()));
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(releve.soldeActuel()), "40 000 facturés, 10 000 réglés");
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(releve.soldeCumule()), "le report plus le mois");
    }

    @Test
    @DisplayName("Une facture antérieure soldée ne pèse pas sur le report")
    void reportIgnoreLesFacturesSoldees() {
        TiersPayant organisme = tiersPayant("SOLDE RECAP");
        factureDatee(organisme, 90_000, 90_000, InvoiceStatut.PAID, moisPrecedent(8));
        factureDatee(organisme, 20_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(6));
        viderLeCache();

        RecapitulatifMensuelDto releve = releveDuMois().getContent().getFirst();

        assertEquals(0, BigDecimal.ZERO.compareTo(releve.soldePrecedent()), "le mois précédent est soldé");
        assertEquals(0, BigDecimal.valueOf(20_000).compareTo(releve.soldeCumule()));
    }

    @Test
    @DisplayName("Sans passé, le report est nul et le cumul se confond avec le mois")
    void sansReport() {
        TiersPayant organisme = tiersPayant("NEUF RECAP");
        factureDatee(organisme, 25_000, 5_000, InvoiceStatut.PARTIALLY_PAID, ceMoisCi(7));
        viderLeCache();

        RecapitulatifMensuelDto releve = releveDuMois().getContent().getFirst();

        assertEquals(0, BigDecimal.ZERO.compareTo(releve.soldePrecedent()));
        assertEquals(0, releve.soldeActuel().compareTo(releve.soldeCumule()));
    }

    @Test
    @DisplayName("Une facture d'un autre mois ne rentre pas dans le relevé")
    void horsDuMois() {
        TiersPayant organisme = tiersPayant("AUTRE MOIS");
        factureDatee(organisme, 50_000, 0, InvoiceStatut.NOT_PAID, moisPrecedent(15));
        viderLeCache();

        assertTrue(releveDuMois().getContent().isEmpty(), "aucune facture émise ce mois-ci");
    }

    @Test
    @DisplayName("Chaque organisme a son propre relevé")
    void unReleveParOrganisme() {
        factureDatee(tiersPayant("ORG A"), 30_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(9));
        factureDatee(tiersPayant("ORG B"), 70_000, 70_000, InvoiceStatut.PAID, ceMoisCi(9));
        viderLeCache();

        List<RecapitulatifMensuelDto> releves = releveDuMois().getContent();

        assertEquals(2, releves.size());
        assertEquals(
            0,
            BigDecimal.valueOf(100_000).compareTo(
                releves.stream().map(RecapitulatifMensuelDto::totalFacture).reduce(BigDecimal.ZERO, BigDecimal::add)
            )
        );
    }

    @Test
    @DisplayName("Le relevé se restreint aux organismes demandés")
    void filtreParOrganisme() {
        TiersPayant vise = tiersPayant("VISE RECAP");
        factureDatee(vise, 30_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(11));
        factureDatee(tiersPayant("IGNORE RECAP"), 70_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(11));
        viderLeCache();

        List<RecapitulatifMensuelDto> releves = services.recapitulatifMensuelService
            .getRecapitulatif(
                new RecapitulatifMensuelParams(MOIS.getYear(), MOIS.getMonthValue(), List.of(vise.getId()), List.of(), null),
                page()
            )
            .getContent();

        assertEquals(1, releves.size());
        assertEquals(vise.getFullName(), releves.getFirst().tiersPayantName());
    }

    @Test
    @DisplayName("Le relevé sépare les factures individuelles des factures de groupe")
    void filtreParTypeDeFacture() {
        TiersPayant individuel = tiersPayant("INDIVIDUEL RECAP");
        factureDatee(individuel, 30_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(13));
        GroupeTiersPayant groupe = groupe("GROUPE RECAP");
        facturePorteuse(groupe, 80_000, ceMoisCi(13));
        viderLeCache();

        List<RecapitulatifMensuelDto> individuelles = releve(TypeFacture.INDIVIDUAL).getContent();
        List<RecapitulatifMensuelDto> groupees = releve(TypeFacture.GROUPED).getContent();

        assertEquals(1, individuelles.size());
        assertEquals(individuel.getFullName(), individuelles.getFirst().tiersPayantName());
        assertEquals(1, groupees.size());
        assertEquals(groupe.getName(), groupees.getFirst().tiersPayantName(), "la porteuse est identifiée par son groupe");
    }

    @Test
    @DisplayName("La pagination découpe les organismes sans en perdre le compte")
    void pagination() {
        factureDatee(tiersPayant("PAGE RECAP A"), 10_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(14));
        factureDatee(tiersPayant("PAGE RECAP B"), 20_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(14));
        factureDatee(tiersPayant("PAGE RECAP C"), 30_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(14));
        viderLeCache();

        var premiere = services.recapitulatifMensuelService.getRecapitulatif(params(null), PageRequest.of(0, 2));
        var seconde = services.recapitulatifMensuelService.getRecapitulatif(params(null), PageRequest.of(1, 2));

        assertEquals(2, premiere.getContent().size());
        assertEquals(1, seconde.getContent().size());
        assertEquals(3, premiere.getTotalElements());
    }

    @Test
    @DisplayName("Une page au-delà du dernier organisme est vide plutôt qu'en erreur")
    void pageAuDela() {
        factureDatee(tiersPayant("UNE SEULE RECAP"), 10_000, 0, InvoiceStatut.NOT_PAID, ceMoisCi(16));
        viderLeCache();

        var page = services.recapitulatifMensuelService.getRecapitulatif(params(null), PageRequest.of(5, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("Les exports du relevé ne sont pas encore écrits")
    void exportsNonImplementes() {
        // Le service rend un tableau vide au lieu d'un document : le jour où l'export sera écrit,
        // ce test échouera et rappellera de le couvrir pour de bon.
        assertEquals(0, services.recapitulatifMensuelService.exportPdf(params(null)).length);
        assertEquals(0, services.recapitulatifMensuelService.exportExcel(params(null)).length);
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private LocalDate ceMoisCi(int jour) {
        return MOIS.atDay(jour);
    }

    private LocalDate moisPrecedent(int jour) {
        return MOIS.minusMonths(1).atDay(jour);
    }

    private RecapitulatifMensuelParams params(TypeFacture typeFacture) {
        return new RecapitulatifMensuelParams(MOIS.getYear(), MOIS.getMonthValue(), List.of(), List.of(), typeFacture);
    }

    private org.springframework.data.domain.Page<RecapitulatifMensuelDto> releveDuMois() {
        return services.recapitulatifMensuelService.getRecapitulatif(params(null), page());
    }

    private org.springframework.data.domain.Page<RecapitulatifMensuelDto> releve(TypeFacture typeFacture) {
        return services.recapitulatifMensuelService.getRecapitulatif(params(typeFacture), page());
    }

    private FactureTiersPayant factureDatee(
        TiersPayant organisme,
        int montantNet,
        int montantRegle,
        InvoiceStatut statut,
        LocalDate dateEmission
    ) {
        FactureTiersPayant facture = facture(organisme, false, List.of(), dateEmission);
        facture.setMontantTtc(BigDecimal.valueOf(montantNet));
        facture.setMontantNet(BigDecimal.valueOf(montantNet));
        facture.setMontantRegle(montantRegle);
        facture.setStatut(statut);
        em.flush();
        return facture;
    }

    private FactureTiersPayant facturePorteuse(GroupeTiersPayant groupe, int montantNet, LocalDate dateEmission) {
        FactureTiersPayant facture = facture(null, false, List.of(), dateEmission);
        facture.setGroupeTiersPayant(groupe);
        facture.setMontantTtc(BigDecimal.valueOf(montantNet));
        facture.setMontantNet(BigDecimal.valueOf(montantNet));
        facture.setStatut(InvoiceStatut.NOT_PAID);
        em.flush();
        return facture;
    }
}
