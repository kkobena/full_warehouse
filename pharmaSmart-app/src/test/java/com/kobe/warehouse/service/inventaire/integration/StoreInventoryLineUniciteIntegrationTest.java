package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'unicité de {@code store_inventory_line} porte sur le triplet
 * {@code (produit, inventaire, emplacement)} depuis V1.2.0 : un même produit peut être compté en
 * rayon puis en réserve au sein d'une même session, et c'est tout l'objet de la colonne
 * {@code storage_id} ajoutée par cette migration.
 *
 * <p>Cela n'était pourtant pas vrai en base. V1.2.0 croyait remplacer l'ancienne unicité à deux
 * colonnes en la supprimant par son nom — mais visait le nom qu'aurait généré un {@code UNIQUE}
 * déclaré en ligne, alors que V1.0.1 avait créé la table avec un nom généré par Hibernate. Le
 * {@code DROP CONSTRAINT IF EXISTS} n'a donc rien trouvé et n'a rien dit : les deux contraintes ont
 * coexisté, et la plus stricte a continué d'interdire le second comptage. V2.0.7 supprime
 * l'ancienne en la retrouvant par sa définition plutôt que par son nom.
 *
 * <p>Ce test tient les deux bouts : ce qui doit désormais passer, et ce qui doit continuer d'être
 * refusé.
 */
@DisplayName("store_inventory_line — unicité par produit, inventaire et emplacement")
class StoreInventoryLineUniciteIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Un même produit se compte en rayon et en réserve dans la même session")
    void memeProduitSurDeuxEmplacements() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Produit produit = produitEnStock(unique("DOLIPRANE"), 40);

        ligneSur(inventaire, produit, rayon);
        ligneSur(inventaire, produit, reserve);
        viderLeCache();

        assertEquals(
            2,
            compter(
                "SELECT COUNT(*) FROM store_inventory_line WHERE produit_id = %d AND store_inventory_id = %d".formatted(
                        produit.getId(),
                        inventaire.getId()
                    )
            )
        );
    }

    @Test
    @DisplayName("Le même produit sur le même emplacement reste refusé")
    void memeProduitSurLeMemeEmplacement() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Produit produit = produitEnStock(unique("DOLIPRANE"), 40);
        ligneSur(inventaire, produit, rayon);
        viderLeCache();

        assertThrows(PersistenceException.class, () -> {
            ligneSur(inventaire, produit, rayon);
            em.flush();
        });
    }

    @Test
    @DisplayName("Le même produit se recompte dans une autre session au même emplacement")
    void memeProduitDansDeuxSessions() {
        StoreInventory premier = inventaire(InventoryCategory.MAGASIN);
        StoreInventory second = inventaire(InventoryCategory.MAGASIN);
        Produit produit = produitEnStock(unique("DOLIPRANE"), 40);

        ligneSur(premier, produit, rayon);
        ligneSur(second, produit, rayon);
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM store_inventory_line WHERE produit_id = " + produit.getId()));
    }

    @Test
    @DisplayName("Il ne reste qu'une seule unicité sur la table, et c'est celle du triplet")
    void uneSeuleUniciteSubsiste() {
        assertEquals(
            1,
            compter(
                """
                SELECT COUNT(*) FROM pg_constraint con
                  JOIN pg_class rel ON rel.oid = con.conrelid
                  JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                 WHERE rel.relname = 'store_inventory_line'
                   AND nsp.nspname = current_schema()
                   AND con.contype = 'u'
                """
            ),
            "l'ancienne unicité à deux colonnes a bien été supprimée par V2.0.7"
        );

        assertTrue(
            compter(
                """
                SELECT COUNT(*) FROM pg_constraint con
                  JOIN pg_class rel ON rel.oid = con.conrelid
                  JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                 WHERE rel.relname = 'store_inventory_line'
                   AND nsp.nspname = current_schema()
                   AND con.contype = 'u'
                   AND (SELECT array_agg(att.attname::text ORDER BY att.attname)
                          FROM unnest(con.conkey) AS k(attnum)
                          JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = k.attnum)
                       = ARRAY['produit_id', 'storage_id', 'store_inventory_id']
                """
            ) == 1,
            "et celle qui subsiste porte bien sur les trois colonnes"
        );
    }

    /** La fabrique du socle range la ligne sur l'emplacement de l'inventaire ; ici on l'impose. */
    private StoreInventoryLine ligneSur(StoreInventory inventaire, Produit produit, com.kobe.warehouse.domain.Storage storage) {
        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setStoreInventory(inventaire);
        ligne.setProduit(produit);
        ligne.setStorage(storage);
        ligne.setUpdated(false);
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }
}
