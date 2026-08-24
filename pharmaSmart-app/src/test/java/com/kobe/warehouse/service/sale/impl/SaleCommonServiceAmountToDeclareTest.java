package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le montant déclarable d'une ligne doit suivre ses quantités et ses prix.
 *
 * <p>Il n'était renseigné qu'à la création de la ligne : toute modification ultérieure de quantité
 * ou de prix le laissait sur son ancienne valeur. L'écart restait invisible tant qu'aucun rapport ne
 * lisait la colonne, et ne l'aurait plus été une fois la déclaration du CA branchée dessus.
 *
 * <p>Ces tests verrouillent aussi l'invariant qui fonde toute la chaîne : <strong>la somme des
 * lignes égale le montant de la vente</strong>. Les rapports agrègent par ligne ; si la vente et ses
 * lignes divergent, les écrans de comptabilité cessent de se recouper.
 */
@DisplayName("Montant déclarable — cohérence entre la vente et ses lignes")
class SaleCommonServiceAmountToDeclareTest {

    private SaleCommonService service;

    @BeforeEach
    void setUp() {
        // computeSaleEagerAmount ne calcule qu'à partir des lignes : aucun collaborateur n'est sollicité.
        service = new SaleCommonService(null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("À quantité et prix inchangés, le montant déclarable vaut le montant de la ligne")
    void montantDeclarableInitial() {
        Sales vente = venteAvecLignes(ligne(2, 1_500), ligne(1, 4_000));

        service.computeSaleEagerAmount(vente);

        assertEquals(3_000, ligneNumero(vente, 0).getAmountToBeTakenIntoAccount());
        assertEquals(4_000, ligneNumero(vente, 1).getAmountToBeTakenIntoAccount());
    }

    @Test
    @DisplayName("Un changement de quantité est répercuté")
    void changementDeQuantite() {
        Sales vente = venteAvecLignes(ligne(2, 1_500));
        service.computeSaleEagerAmount(vente);

        ligneNumero(vente, 0).setQuantityRequested(5);
        service.computeSaleEagerAmount(vente);

        assertEquals(7_500, ligneNumero(vente, 0).getAmountToBeTakenIntoAccount());
    }

    @Test
    @DisplayName("Un changement de prix est répercuté")
    void changementDePrix() {
        Sales vente = venteAvecLignes(ligne(3, 1_000));
        service.computeSaleEagerAmount(vente);

        ligneNumero(vente, 0).setRegularUnitPrice(2_500);
        service.computeSaleEagerAmount(vente);

        assertEquals(7_500, ligneNumero(vente, 0).getAmountToBeTakenIntoAccount());
    }

    @Test
    @DisplayName("La somme des lignes égale toujours le montant de la vente")
    void sommeDesLignesEgaleLaVente() {
        Sales vente = venteAvecLignes(ligne(2, 1_500), ligne(1, 4_000), ligne(7, 320));

        service.computeSaleEagerAmount(vente);
        assertEquals(vente.getAmountToBeTakenIntoAccount(), sommeDesLignes(vente));

        ligneNumero(vente, 2).setQuantityRequested(1);
        ligneNumero(vente, 0).setRegularUnitPrice(999);
        service.computeSaleEagerAmount(vente);

        assertEquals(vente.getAmountToBeTakenIntoAccount(), sommeDesLignes(vente));
        assertEquals(2 * 999 + 4_000 + 320, sommeDesLignes(vente));
    }

    // ===== Utilitaires =====

    private int sommeDesLignes(Sales vente) {
        return vente.getSalesLines().stream().mapToInt(SalesLine::getAmountToBeTakenIntoAccount).sum();
    }

    private SalesLine ligneNumero(Sales vente, int index) {
        return vente.getSalesLines().stream().toList().get(index);
    }

    private Sales venteAvecLignes(SalesLine... lignes) {
        Sales vente = new CashSale();
        LinkedHashSet<SalesLine> set = new LinkedHashSet<>();
        long id = 1;
        for (SalesLine ligne : lignes) {
            // Sans identifiant distinct, deux lignes sont égales et le Set n'en garde qu'une.
            ligne.setId(id++);
            set.add(ligne);
        }
        vente.setSalesLines(set);
        return vente;
    }

    private SalesLine ligne(int quantite, int prixUnitaire) {
        SalesLine ligne = new SalesLine();
        ligne.setProduit(new Produit());
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(prixUnitaire);
        ligne.setNetUnitPrice(prixUnitaire);
        ligne.setCostAmount(0);
        ligne.setTaxValue(0);
        ligne.setDiscountAmount(0);
        return ligne;
    }
}
