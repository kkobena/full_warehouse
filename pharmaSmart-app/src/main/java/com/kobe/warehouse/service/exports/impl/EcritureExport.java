package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import com.kobe.warehouse.repository.exports.ExportDonneesRepository;
import com.kobe.warehouse.service.exports.EcrivainExport;
import com.kobe.warehouse.service.exports.FabriqueEcrivainsExport;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Écrit un export dans un flux : un export de données se lit en flux (par lots) et s'écrit ligne à ligne sans tenir le résultat
 * en mémoire, une édition est bâtie d'un bloc par le service de son écran.
 *
 * <p>Partagée par les deux chemins de production : la demande du catalogue, qui écrit dans la réponse et rend le fichier tout de
 * suite, et la génération en tâche de fond d'un modèle programmé, qui écrit dans un fichier.
 */
@Component
class EcritureExport {

    private static final DateTimeFormatter DATE_INTITULE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExportDonneesRepository exportDonneesRepository;
    private final ProducteurRapports producteurRapports;
    private final FabriqueEcrivainsExport fabriqueEcrivainsExport;

    EcritureExport(
        ExportDonneesRepository exportDonneesRepository,
        ProducteurRapports producteurRapports,
        FabriqueEcrivainsExport fabriqueEcrivainsExport
    ) {
        this.exportDonneesRepository = exportDonneesRepository;
        this.producteurRapports = producteurRapports;
        this.fabriqueEcrivainsExport = fabriqueEcrivainsExport;
    }

    /** @return le nombre de lignes écrites, inconnu (et donc vide) pour une édition bâtie d'un bloc */
    Long ecrire(
        SujetExport export,
        FormatExport format,
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres,
        OutputStream sortie
    ) {
        return switch (export) {
            case ExportDonnees donnees -> ecrireLignes(donnees, format, du, au, filtres, sortie);
            case RapportExport rapport -> ecrireEdition(rapport, format, du, au, filtres, sortie);
        };
    }

    private Long ecrireLignes(
        ExportDonnees donnees,
        FormatExport format,
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres,
        OutputStream sortie
    ) {
        long lignes = 0;
        try (
            EcrivainExport ecrivain = fabriqueEcrivainsExport.ouvrir(format, donnees.getColonnes(), intituler(donnees, du, au, filtres), sortie);
            Stream<Object[]> flux = exportDonneesRepository.lireLignes(donnees, du, au, filtres)
        ) {
            for (Object[] ligne : (Iterable<Object[]>) flux::iterator) {
                ecrivain.ecrire(ligne);
                lignes++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return lignes;
    }

    private Long ecrireEdition(
        RapportExport rapport,
        FormatExport format,
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres,
        OutputStream sortie
    ) {
        try {
            sortie.write(producteurRapports.produire(rapport, format, du, au, filtres));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    /** « Lignes de vente — du 01/10/2026 au 31/10/2026 · Famille : 2 valeurs », en tête du PDF. */
    private static String intituler(SujetExport export, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        String periode = du == null
            ? "état du " + LocalDate.now().format(DATE_INTITULE)
            : "du " + du.format(DATE_INTITULE) + " au " + au.format(DATE_INTITULE);
        List<String> retenus = RedacteurFiltresExport.rediger(filtres);
        return export.getLibelle() + " — " + periode + (retenus.isEmpty() ? "" : " · " + String.join(" · ", retenus));
    }
}
