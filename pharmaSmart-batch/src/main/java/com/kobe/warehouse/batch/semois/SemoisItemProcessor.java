package com.kobe.warehouse.batch.semois;

import com.kobe.warehouse.domain.Produit;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Passe les produits au writer sans transformation.
 * Le batch-loading (VMM, stocks, classeConfigMap) est conservé dans le writer
 * pour préserver l'efficacité SQL de {@code SemoisCalculationService.processBatch()}.
 *
 * Phase 2 : ce processor sera remplacé par un appel à
 * {@code SemoisCalculationService.calculateForProduit(Produit)} qui exposera
 * la logique de calcul par item sans l'étape de persistance.
 */
@Component
public class SemoisItemProcessor implements ItemProcessor<Produit, Produit> {

    @Override
    public Produit process(Produit produit) {
        return produit;
    }
}
