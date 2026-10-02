package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;
import com.kobe.warehouse.domain.enumeration.TypeGenerique;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.DciRepository;
import com.kobe.warehouse.repository.ProduitRefSpecialiteRepository;
import com.kobe.warehouse.repository.RefSpecialiteCompositionRepository;
import com.kobe.warehouse.repository.RefSpecialiteRcpRepository;
import com.kobe.warehouse.service.referentiel.ProduitReferentielDTO;
import com.kobe.warehouse.service.referentiel.RapprochementProduitService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Rapprochement produit ↔ référentiel médicament, de bout en bout sur PostgreSQL : migrations
 * V2.1.19 et V2.1.20, fonctions SQL, déclencheur de dissociation, service et lecture de la fiche.
 *
 * <p>Le référentiel de la base de test est vide : chaque test y pose son mini-référentiel (une
 * molécule, un princeps et un générique du même groupe), annulé avec la transaction.
 */
@DisplayName("Rapprochement produit ↔ référentiel médicament sur PostgreSQL")
class RapprochementReferentielIntegrationTest extends AbstractStockIntegrationTest {

    private static final String MOLECULE = "TESTMOLECULEA";
    private static final String CIS_PRINCEPS = "99000001";
    private static final String CIS_GENERIQUE = "99000002";

    private RapprochementProduitService service;

    @BeforeEach
    void creerLeService() {
        service = new RapprochementProduitService(
            em,
            IntegrationPostgresDatabase.bean(ProduitRefSpecialiteRepository.class),
            IntegrationPostgresDatabase.bean(DciRepository.class),
            IntegrationPostgresDatabase.bean(RefSpecialiteCompositionRepository.class),
            IntegrationPostgresDatabase.bean(RefSpecialiteRcpRepository.class)
        );
    }

    // ===== les règles =====

    @Test
    @DisplayName("Un produit sûr, sans DCI, est accepté d'office et reçoit la DCI du référentiel")
    void produitSurSansDci() {
        poserLeReferentiel();
        Dci dci = creerDciDuCatalogue();
        Produit produit = produit("TESTBRAND 500MG CPR B/30");

        service.rapprocherNouveauxProduits();

        assertEquals("SUR", lireChamp(produit, "statut"));
        assertEquals("AUTO", lireChamp(produit, "decision"));
        assertEquals(CIS_PRINCEPS, lireChamp(produit, "cis"));
        assertEquals("true", lireChamp(produit, "dci_posee"));
        assertEquals(List.of(dci.getId()), listerMolecules(produit));
        assertEquals(dci.getId().longValue(), compter("SELECT dci_id FROM produit WHERE id = " + produit.getId()), "produit.dci_id alignée sur le rang 1");
        assertEquals(500, compter("SELECT dosage_valeur FROM produit_dci WHERE produit_id = " + produit.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM produit_dci WHERE produit_id = " + produit.getId() + " AND rang <> 1"));
    }

    @Test
    @DisplayName("Un produit déjà doté de la même DCI est accepté sans que ses DCI changent")
    void produitDejaDoteDeLaMemeDci() {
        poserLeReferentiel();
        Dci dci = creerDciDuCatalogue();
        Produit produit = produit("TESTBRAND 500MG CPR B/60");
        produit.remplacerDcis(List.of(dci));
        em.flush();

        service.rapprocherNouveauxProduits();

        assertEquals("AUTO", lireChamp(produit, "decision"));
        assertEquals("false", lireChamp(produit, "dci_posee"), "rien n'a été posé : pas de badge");
        assertEquals(List.of(dci.getId()), listerMolecules(produit));
    }

    @Test
    @DisplayName("Une DCI du produit qui contredit le référentiel rétrograde la proposition : elle reste à relire")
    void conflitDeDci() {
        poserLeReferentiel();
        creerDciDuCatalogue();
        Dci autre = creerDci("TESTAUTREMOLECULE");
        Produit produit = produit("TESTBRAND 500MG CPR B/90");
        produit.remplacerDcis(List.of(autre));
        em.flush();

        service.rapprocherNouveauxProduits();

        assertEquals("A_VERIFIER", lireChamp(produit, "statut"));
        assertEquals("EN_ATTENTE", lireChamp(produit, "decision"));
        assertEquals(List.of(autre.getId()), listerMolecules(produit), "une DCI saisie par une personne n'est jamais écrasée");
    }

    @Test
    @DisplayName("Un produit nommé par sa molécule est rattaché par DCI, au princeps de préférence")
    void rattacheParLaMolecule() {
        poserLeReferentiel();
        Dci dci = creerDciDuCatalogue();
        Produit produit = produit("TESTMOLECULEA 500MG CPR B/30");

        service.rapprocherNouveauxProduits();

        assertEquals("PAR_DCI", lireChamp(produit, "statut"));
        assertEquals("AUTO", lireChamp(produit, "decision"));
        assertEquals(CIS_PRINCEPS, lireChamp(produit, "cis"));
        assertEquals(List.of(dci.getId()), listerMolecules(produit));
    }

    @Test
    @DisplayName("Sans DCI au catalogue, la proposition attend ; l'ajout de la DCI la fait aboutir")
    void ajoutDeDci() {
        poserLeReferentiel();
        Produit produit = produit("TESTBRAND 500MG CPR B/120");

        service.rapprocherNouveauxProduits();
        assertEquals("SUR", lireChamp(produit, "statut"));
        assertEquals("EN_ATTENTE", lireChamp(produit, "decision"), "la molécule n'est pas au catalogue : rien à poser");
        assertEquals(List.of(), listerMolecules(produit));

        Dci dci = creerDciDuCatalogueSansLier();
        service.rapprocherApresAjoutDci(List.of(dci.getId()));

        assertEquals("AUTO", lireChamp(produit, "decision"));
        assertEquals("true", lireChamp(produit, "dci_posee"));
        assertEquals(List.of(dci.getId()), listerMolecules(produit));
    }

    @Test
    @DisplayName("Retirer la DCI posée automatiquement rejette le rapprochement, qui n'est plus jamais réappliqué")
    void dissociation() {
        poserLeReferentiel();
        Dci dci = creerDciDuCatalogue();
        Produit produit = produit("TESTBRAND 500MG CPR B/150");
        service.rapprocherNouveauxProduits();
        assertEquals(List.of(dci.getId()), listerMolecules(produit));

        em.createNativeQuery("DELETE FROM produit_dci WHERE produit_id = :id").setParameter("id", produit.getId()).executeUpdate();
        em.createNativeQuery("UPDATE produit SET dci_id = NULL WHERE id = :id").setParameter("id", produit.getId()).executeUpdate();
        assertEquals("REJETE", lireChamp(produit, "decision"));
        assertTrue(lireChamp(produit, "motif").contains("DCI dissociée"));

        service.recalculerPropositions();

        assertEquals("REJETE", lireChamp(produit, "decision"));
        assertEquals(List.of(), listerMolecules(produit), "la DCI retirée ne revient pas");
    }

    @Test
    @DisplayName("Retirer une DCI étrangère à la spécialité ne rejette rien")
    void dissociationEtrangere() {
        poserLeReferentiel();
        Dci dci = creerDciDuCatalogue();
        Dci etrangere = creerDci("TESTETRANGERE");
        Produit produit = produit("TESTBRAND 500MG CPR B/180");
        service.rapprocherNouveauxProduits();
        viderLeCache();
        // La base a changé sous le contexte de persistance (DCI posée en SQL) : on repart d'une relecture.
        Produit relu = em.find(Produit.class, produit.getId());
        relu.remplacerDcis(List.of(em.find(Dci.class, dci.getId()), em.find(Dci.class, etrangere.getId())));
        em.flush();

        relu.remplacerDcis(List.of(em.find(Dci.class, dci.getId())));
        em.flush();

        assertEquals("AUTO", lireChamp(produit, "decision"));
    }

    @Test
    @DisplayName("Une décision humaine n'est jamais recalculée")
    void decisionsDefinitives() {
        poserLeReferentiel();
        creerDciDuCatalogue();
        Produit valide = produit("TESTBRAND 500MG CPR B/210");
        service.rapprocherNouveauxProduits();
        em.createNativeQuery("UPDATE produit_ref_specialite SET decision = 'VALIDE', statut = 'NON_TROUVE', cis = NULL WHERE produit_id = :id")
            .setParameter("id", valide.getId())
            .executeUpdate();

        service.recalculerPropositions();

        assertEquals("VALIDE", lireChamp(valide, "decision"));
        assertEquals("NON_TROUVE", lireChamp(valide, "statut"));
    }

    @Test
    @DisplayName("Tant que le référentiel est vide, rien n'est écrit : les produits ne sont pas figés en NON_TROUVE")
    void referentielVide() {
        Produit produit = produit("TESTBRAND 500MG CPR B/240");

        assertEquals(0, service.rapprocherNouveauxProduits());

        assertEquals(0, compter("SELECT COUNT(*) FROM produit_ref_specialite WHERE produit_id = " + produit.getId()));
    }

    @Test
    @DisplayName("Un produit sans correspondance est mémorisé NON_TROUVE, et ne reçoit aucune DCI")
    void sansCorrespondance() {
        poserLeReferentiel();
        Produit produit = produit("SHAMPOOING DOUX 100ML FLACON");

        service.rapprocherNouveauxProduits();

        assertEquals("NON_TROUVE", lireChamp(produit, "statut"));
        assertNull(lireChamp(produit, "cis"));
        assertEquals(List.of(), listerMolecules(produit));
    }

    @Test
    @DisplayName("Les produits de type DETAIL ne sont pas rapprochés : seul le conditionnement PACKAGE l'est")
    void detailIgnore() {
        poserLeReferentiel();
        Produit detail = produit("TESTBRAND 500MG CPR B/270");
        detail.setTypeProduit(TypeProduit.DETAIL);
        em.flush();

        service.rapprocherNouveauxProduits();

        assertEquals(0, compter("SELECT COUNT(*) FROM produit_ref_specialite WHERE produit_id = " + detail.getId()));
    }

    // ===== le service =====

    @Test
    @DisplayName("La fiche donne spécialité, molécules, RCP et substituts du catalogue pour un rapprochement de confiance")
    void lireFiche() {
        poserLeReferentiel();
        creerDciDuCatalogue();
        em.createNativeQuery(
            "INSERT INTO ref_specialite_rcp (cis, indications, posologie, contre_indications) VALUES (:cis, 'douleur', '1 comprimé', 'allergie connue')"
        )
            .setParameter("cis", CIS_PRINCEPS)
            .executeUpdate();
        Produit princeps = produit("TESTBRAND 500MG CPR B/300");
        Produit generique = produit("TESTGENERIK 500MG CPR B/300");
        service.rapprocherNouveauxProduits();
        viderLeCache();

        ProduitReferentielDTO fiche = service.lireFiche(princeps.getId());

        assertEquals(DecisionRapprochement.AUTO, fiche.decision());
        assertTrue(fiche.dciPoseeAutomatiquement());
        assertEquals(CIS_PRINCEPS, fiche.specialite().cis());
        assertEquals(TypeGenerique.PRINCEPS, fiche.specialite().typeGenerique());
        assertEquals(1, fiche.molecules().size());
        assertEquals("allergie connue", fiche.rcp().contreIndications());
        assertEquals(List.of(generique.getId()), fiche.substituts().stream().map(ProduitReferentielDTO.Substitut::produitId).toList());
        assertEquals(TypeGenerique.GENERIQUE, fiche.substituts().getFirst().typeGenerique());
    }

    @Test
    @DisplayName("La fiche n'oriente pas le conseil tant que le rapprochement est à relire")
    void ficheDUneProposition() {
        poserLeReferentiel();
        creerDciDuCatalogue();
        Dci autre = creerDci("TESTAUTREMOLECULE");
        Produit produit = produit("TESTBRAND 500MG CPR B/330");
        produit.remplacerDcis(List.of(autre));
        em.flush();
        service.rapprocherNouveauxProduits();
        viderLeCache();

        ProduitReferentielDTO fiche = service.lireFiche(produit.getId());

        assertEquals(DecisionRapprochement.EN_ATTENTE, fiche.decision());
        assertFalse(fiche.dciPoseeAutomatiquement());
        assertNull(fiche.specialite());
        assertNull(fiche.rcp());
        assertTrue(fiche.substituts().isEmpty());
        assertEquals(StatutRapprochement.NON_TROUVE, service.lireFiche(-1).statut(), "produit inconnu : fiche vide");
    }

    @Test
    @DisplayName("Valider une proposition à relire la fixe sans toucher aux DCI du produit ; la rejeter ferme la fiche")
    void validerEtRejeter() {
        poserLeReferentiel();
        creerDciDuCatalogue();
        Dci autre = creerDci("TESTAUTREMOLECULE");
        Produit produit = produit("TESTBRAND 500MG CPR B/360");
        produit.remplacerDcis(List.of(autre));
        em.flush();
        service.rapprocherNouveauxProduits();
        viderLeCache();

        service.valider(produit.getId());
        em.flush();
        assertEquals("VALIDE", lireChamp(produit, "decision"));
        assertEquals(List.of(autre.getId()), listerMolecules(produit));

        service.rejeter(produit.getId());
        em.flush();
        viderLeCache();
        assertEquals(DecisionRapprochement.REJETE, service.lireFiche(produit.getId()).decision());
        assertNull(service.lireFiche(produit.getId()).specialite());
    }

    @Test
    @DisplayName("Réinitialiser oublie la proposition : le produit redevient candidat")
    void reinitialiser() {
        poserLeReferentiel();
        Produit produit = produit("TESTBRAND 500MG CPR B/390");
        service.rapprocherNouveauxProduits();
        viderLeCache();

        service.reinitialiser(produit.getId());
        em.flush();
        assertEquals(0, compter("SELECT COUNT(*) FROM produit_ref_specialite WHERE produit_id = " + produit.getId()));

        service.rapprocherNouveauxProduits();
        assertEquals("SUR", lireChamp(produit, "statut"));
    }

    // ===== le jeu d'essai =====

    /** Une molécule, un princeps (TESTBRAND) et un générique (TESTGENERIK), 500 mg comprimé, dans un même groupe. */
    private void poserLeReferentiel() {
        executer("INSERT INTO ref_groupe_generique (id, libelle) VALUES (99001, 'TESTMOLECULEA 500 mg - TESTBRAND 500 mg, comprimé')");
        executer("INSERT INTO ref_substance (code, libelle) VALUES ('T0001', '" + MOLECULE + "')");
        executer("INSERT INTO ref_dci (libelle) VALUES ('" + MOLECULE + "')");
        creerSpecialite(CIS_PRINCEPS, "TESTBRAND 500 mg, comprimé", "PRINCEPS");
        creerSpecialite(CIS_GENERIQUE, "TESTGENERIK 500 mg, comprimé", "GENERIQUE");
    }

    private void creerSpecialite(String cis, String libelle, String type) {
        executer(
            "INSERT INTO ref_specialite (cis, libelle, forme, commercialisee, groupe_generique_id, type_generique) VALUES ('" +
            cis + "', '" + libelle + "', 'comprimé', TRUE, 99001, '" + type + "')"
        );
        executer(
            "INSERT INTO ref_specialite_composition (cis, substance_code, dci_id, nature, dosage_texte, dosage_valeur, dosage_unite, reference_dosage) " +
            "SELECT '" + cis + "', 'T0001', id, 'FT', '500 mg', 500, 'mg', 'un comprimé' FROM ref_dci WHERE libelle = '" + MOLECULE + "'"
        );
    }

    /** La DCI du catalogue, reliée au référentiel. */
    private Dci creerDciDuCatalogue() {
        Dci dci = creerDciDuCatalogueSansLier();
        em.createNativeQuery("SELECT ref_lier_dci()").getSingleResult();
        return dci;
    }

    private Dci creerDciDuCatalogueSansLier() {
        Dci dci = new Dci();
        dci.setLibelle(MOLECULE);
        dci.setCode(unique("TSTA"));
        em.persist(dci);
        em.flush();
        return dci;
    }

    private Dci creerDci(String libelle) {
        Dci dci = new Dci();
        dci.setLibelle(libelle);
        dci.setCode(unique("TST"));
        em.persist(dci);
        em.flush();
        return dci;
    }

    @Override
    protected Produit produit(String libelle) {
        Produit produit = super.produit(libelle);
        produit.setTypeProduit(TypeProduit.PACKAGE);
        em.flush();
        return produit;
    }

    private void executer(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }

    private String lireChamp(Produit produit, String colonne) {
        em.flush();
        List<?> lignes = em
            .createNativeQuery("SELECT " + colonne + " FROM produit_ref_specialite WHERE produit_id = :id")
            .setParameter("id", produit.getId())
            .getResultList();
        Object valeur = lignes.isEmpty() ? null : lignes.getFirst();
        return valeur == null ? null : valeur.toString();
    }

    private List<Integer> listerMolecules(Produit produit) {
        em.flush();
        return em
            .createNativeQuery("SELECT dci_id FROM produit_dci WHERE produit_id = :id ORDER BY rang", Integer.class)
            .setParameter("id", produit.getId())
            .getResultList();
    }
}
