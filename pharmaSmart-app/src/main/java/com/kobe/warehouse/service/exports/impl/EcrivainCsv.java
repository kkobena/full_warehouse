package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.service.exports.EcrivainExport;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV pour un tableur réglé en français : UTF-8 avec BOM (sans lui, Excel lit mal les accents), séparateur « ; », virgule
 * décimale, dates jj/mm/aaaa ; texte entre guillemets.
 */
class EcrivainCsv implements EcrivainExport {

    private static final char BOM = '\uFEFF';
    private static final String SEPARATEUR = ";";
    private static final String FIN_DE_LIGNE = "\r\n";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<ColonneExport> colonnes;
    private final Writer sortie;

    EcrivainCsv(List<ColonneExport> colonnes, OutputStream flux) throws IOException {
        this.colonnes = colonnes;
        this.sortie = new BufferedWriter(new OutputStreamWriter(flux, StandardCharsets.UTF_8));
        sortie.write(BOM);
        ecrireLigne(colonnes.stream().map(colonne -> (Object) colonne.libelle()).toArray());
    }

    @Override
    public void ecrire(Object[] ligne) throws IOException {
        ecrireLigne(ligne);
    }

    private void ecrireLigne(Object[] valeurs) throws IOException {
        for (int rang = 0; rang < colonnes.size(); rang++) {
            if (rang > 0) {
                sortie.write(SEPARATEUR);
            }
            sortie.write(formater(rang < valeurs.length ? valeurs[rang] : null));
        }
        sortie.write(FIN_DE_LIGNE);
    }

    private static String formater(Object valeur) {
        return switch (valeur) {
            case null -> "";
            case LocalDateTime horodatage -> horodatage.format(DATE_HEURE);
            case LocalDate date -> date.format(DATE);
            case Double nombre -> BigDecimal.valueOf(nombre).stripTrailingZeros().toPlainString().replace('.', ',');
            case BigDecimal nombre -> nombre.stripTrailingZeros().toPlainString().replace('.', ',');
            case Number nombre -> nombre.toString();
            case Boolean booleen -> booleen ? "Oui" : "Non";
            default -> "\"" + valeur.toString().replace("\"", "\"\"") + "\"";
        };
    }

    @Override
    public void close() throws IOException {
        sortie.close();
    }
}
