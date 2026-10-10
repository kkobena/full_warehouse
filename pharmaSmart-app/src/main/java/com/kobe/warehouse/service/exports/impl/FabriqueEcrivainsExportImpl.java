package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.service.exports.EcrivainExport;
import com.kobe.warehouse.service.exports.FabriqueEcrivainsExport;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FabriqueEcrivainsExportImpl implements FabriqueEcrivainsExport {

    @Override
    public EcrivainExport ouvrir(FormatExport format, List<ColonneExport> colonnes, OutputStream sortie) throws IOException {
        return switch (format) {
            case CSV -> new EcrivainCsv(colonnes, sortie);
            case XLSX -> new EcrivainXlsx(colonnes, sortie);
        };
    }
}
