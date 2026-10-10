package com.kobe.warehouse.service.exports;

import java.io.IOException;

/** Écrit un export ligne à ligne, sans garder les lignes en mémoire ; l'en-tête est écrit à l'ouverture. */
public interface EcrivainExport extends AutoCloseable {
    /** @param ligne une valeur par colonne, dans l'ordre des colonnes de l'export */
    void ecrire(Object[] ligne) throws IOException;

    @Override
    void close() throws IOException;
}
