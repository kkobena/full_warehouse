package com.kobe.warehouse.service.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Ouvre l'écrivain du format demandé. */
public interface FabriqueEcrivainsExport {
    EcrivainExport ouvrir(FormatExport format, List<ColonneExport> colonnes, OutputStream sortie) throws IOException;
}
