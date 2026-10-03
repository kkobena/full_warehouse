package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.FormProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.sale.ProduitSignauxService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Recherche produit du comptoir : forme, DCI, dosage et statut générique dans chaque résultat
 * (migration V2.1.22). Les fonctions sont appelées telles que l'application les appelle.
 */
@DisplayName("Recherche produit : forme, DCI, dosage et statut générique")
class RechercheProduitIdentiteIntegrationTest extends AbstractStockIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("Chaque résultat porte la forme, la DCI et le dosage du produit")
    void identiteDuProduit() throws Exception {
        String libelle = unique("RECHPROD");
        Produit produit = produit(libelle);
        produit.setForme(forme("Comprimés " + libelle));
        Dci amoxicilline = dci("AMOXICILLINE " + libelle);
        Dci clavulanique = dci("ACIDE CLAVULANIQUE " + libelle);
        produit.remplacerDcis(List.of(amoxicilline, clavulanique));
        em.flush();
        em.createNativeQuery("UPDATE produit_dci SET dosage_valeur = 500, dosage_unite = 'mg' WHERE produit_id = :id AND rang = 1")
            .setParameter("id", produit.getId())
            .executeUpdate();
        em.createNativeQuery("UPDATE produit_dci SET dosage_valeur = 62.5, dosage_unite = 'mg' WHERE produit_id = :id AND rang = 2")
            .setParameter("id", produit.getId())
            .executeUpdate();

        JsonNode resultat = chercher(libelle);

        assertEquals(1, resultat.size());
        assertEquals("Comprimés " + libelle, resultat.get(0).get("forme").asText());
        assertEquals("AMOXICILLINE " + libelle + " + ACIDE CLAVULANIQUE " + libelle, resultat.get(0).get("dci").asText());
        assertEquals("500 mg + 62.5 mg", resultat.get(0).get("dosage").asText(), "zéros inutiles retirés, ordre des molécules respecté");
        assertTrue(resultat.get(0).get("typegenerique").isNull());
    }

    @Test
    @DisplayName("Un produit sans forme ni DCI reste trouvé, avec ces champs vides")
    void produitSansIdentite() throws Exception {
        String libelle = unique("NUDEPROD");
        produit(libelle);

        JsonNode resultat = chercher(libelle);

        assertEquals(1, resultat.size());
        assertTrue(resultat.get(0).get("forme").isNull());
        assertTrue(resultat.get(0).get("dci").isNull());
        assertTrue(resultat.get(0).get("dosage").isNull());
    }

    @Test
    @DisplayName("Le statut générique vient d'un rapprochement accepté d'office ou validé, jamais d'une simple proposition")
    void statutGenerique() throws Exception {
        String libelle = unique("GENEPROD");
        Produit produit = produit(libelle);
        em.createNativeQuery(
            "INSERT INTO ref_specialite (cis, libelle, commercialisee, type_generique) VALUES ('99100001', 'TEST GENERIQUE', TRUE, 'GENERIQUE')"
        )
            .executeUpdate();
        em.createNativeQuery(
            "INSERT INTO produit_ref_specialite (produit_id, cis, statut, score, decision) VALUES (:id, '99100001', 'A_VERIFIER', 50, 'EN_ATTENTE')"
        )
            .setParameter("id", produit.getId())
            .executeUpdate();

        assertTrue(chercher(libelle).get(0).get("typegenerique").isNull(), "une proposition à relire n'oriente pas le comptoir");

        em.createNativeQuery("UPDATE produit_ref_specialite SET decision = 'AUTO' WHERE produit_id = :id").setParameter("id", produit.getId()).executeUpdate();

        assertEquals("GENERIQUE", chercher(libelle).get(0).get("typegenerique").asText());
    }

    @Test
    @DisplayName("Le lot en stock le plus proche de sa péremption est indiqué avec sa date ; un lot lointain ou vide non")
    void peremptionProche() throws Exception {
        String libelle = unique("PEREPROD");
        Produit produit = produit(libelle);

        assertTrue(chercher(libelle).get(0).get("peremptionlot").isNull(), "aucun lot");

        insererLot(produit, "LOTLOIN", 400, 5);
        assertTrue(chercher(libelle).get(0).get("peremptionlot").isNull(), "lot au-delà du seuil");

        insererLot(produit, "LOTVIDE", 10, 0);
        assertTrue(chercher(libelle).get(0).get("peremptionlot").isNull(), "lot proche mais épuisé");

        insererLot(produit, "LOTPLUSTARD", 60, 2);
        insererLot(produit, "LOTPROCHE", 30, 2);
        JsonNode resultat = chercher(libelle).get(0);
        assertEquals("LOTPROCHE", resultat.get("peremptionlot").asText(), "le plus proche d'abord");
        assertEquals(LocalDate.now().plusDays(30).toString(), resultat.get("peremptiondate").asText());
    }

    @Test
    @DisplayName("Les signaux du panier portent le même lot, la même date et la date limite du seuil")
    void signauxDuPanier() {
        Produit produit = produit(unique("SIGNPROD"));
        insererLot(produit, "LOTPROCHE", 30, 2);
        ProduitSignauxService service = new ProduitSignauxService();
        ReflectionTestUtils.setField(service, "em", em);

        var signaux = service.chargerSignaux(List.of(produit.getId())).getFirst();

        assertEquals("LOTPROCHE", signaux.peremptionLot());
        assertEquals(LocalDate.now().plusDays(30).toString(), signaux.peremptionDate());
        assertEquals(LocalDate.now().plusDays(90).toString(), signaux.dateLimitePeremption());
    }

    @Test
    @DisplayName("Les signaux du panier portent le stock restant (rayon et réserve) et le seuil mini")
    void signauxStockFaible() {
        Produit produit = produitEnStock(unique("FAIBPROD"), 4);
        em.createNativeQuery("UPDATE produit SET qty_seuil_mini = 5 WHERE id = :id").setParameter("id", produit.getId()).executeUpdate();
        ProduitSignauxService service = new ProduitSignauxService();
        ReflectionTestUtils.setField(service, "em", em);

        var signaux = service.chargerSignaux(List.of(produit.getId())).getFirst();

        assertEquals(4, signaux.stockRestant());
        assertEquals(5, signaux.seuilMini());
    }

    @Test
    @DisplayName("Le statut légal est renvoyé pour repérer les stupéfiants")
    void statutLegal() throws Exception {
        String libelle = unique("STUPPROD");
        Produit produit = produit(libelle);
        em.createNativeQuery("UPDATE produit SET statut_legal = 'STUPEFIANTS' WHERE id = :id").setParameter("id", produit.getId()).executeUpdate();

        assertEquals("STUPEFIANTS", chercher(libelle).get(0).get("statutlegal").asText());
    }

    @Test
    @DisplayName("La recherche par storage porte les mêmes champs")
    void rechercheParStorage() throws Exception {
        String libelle = unique("STORPROD");
        Produit produit = produitEnStock(libelle, 5);
        produit.setForme(forme("Gélules " + libelle));
        em.flush();

        Object json = em
            .createNativeQuery("SELECT CAST(search_produits_by_storage_json(:q, :storage, 5) AS text)")
            .setParameter("q", libelle)
            .setParameter("storage", STORAGE_RAYON_ID)
            .getSingleResult();

        JsonNode resultat = JSON.readTree(json.toString());
        assertFalse(resultat.isEmpty());
        assertEquals("Gélules " + libelle, resultat.get(0).get("forme").asText());
    }

    private JsonNode chercher(String libelle) throws Exception {
        Object json = em
            .createNativeQuery("SELECT CAST(search_produits_json(:q, :magasin, 5) AS text)")
            .setParameter("q", libelle)
            .setParameter("magasin", MAGASIN_ID)
            .getSingleResult();
        return JSON.readTree(json.toString());
    }

    private void insererLot(Produit produit, String numero, int joursAvantPeremption, int quantite) {
        em.createNativeQuery(
            "INSERT INTO lot (num_lot, produit_id, quantity, quantity_received_ug, created_date, expiry_date, current_quantity, statut, prixachat, prixunit) "
                + "VALUES (:num, :id, 10, 0, now(), CURRENT_DATE + :jours, :qte, 'AVAILABLE', 0, 0)"
        )
            .setParameter("num", numero)
            .setParameter("id", produit.getId())
            .setParameter("jours", joursAvantPeremption)
            .setParameter("qte", quantite)
            .executeUpdate();
    }

    private FormProduit forme(String libelle) {
        FormProduit forme = new FormProduit();
        forme.setLibelle(libelle);
        em.persist(forme);
        em.flush();
        return forme;
    }

    private Dci dci(String libelle) {
        Dci dci = new Dci();
        dci.setLibelle(libelle);
        dci.setCode(unique("D"));
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
}
