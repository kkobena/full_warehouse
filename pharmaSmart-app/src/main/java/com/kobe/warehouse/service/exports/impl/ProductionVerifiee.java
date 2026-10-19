package com.kobe.warehouse.service.exports.impl;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Appel d'un service d'édition qui déclare une exception vérifiée (tableur, moteur de rapport). Il n'y a rien à en faire ici :
 * l'échec remonte avec sa cause — la génération en tâche de fond l'inscrit dans l'historique, la production immédiate le renvoie
 * en erreur.
 */
@FunctionalInterface
interface ProductionVerifiee {
    byte[] produire() throws Exception;

    static byte[] tenter(ProductionVerifiee production) {
        try {
            return production.produire();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
