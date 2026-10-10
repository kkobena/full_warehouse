package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.ExportFichierRepository;
import com.kobe.warehouse.service.exports.EcrivainExport;
import com.kobe.warehouse.service.exports.FabriqueEcrivainsExport;
import com.kobe.warehouse.service.exports.modele.TravailExport;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Génère un fichier d'export en tâche de fond : lecture en flux (lot de 500), écriture ligne à ligne, statut tenu à jour dans
 * l'historique. Un échec garde sa trace (message) et ne laisse pas de fichier partiel.
 */
@Component
class GenerateurExports {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateurExports.class);
    private static final int ERREUR_MAX = 500;

    private final ExportFichierRepository exportFichierRepository;
    private final LecteurExports lecteurExports;
    private final FabriqueEcrivainsExport fabriqueEcrivainsExport;
    private final RepertoireExports repertoireExports;
    private final AppConfigurationService appConfigurationService;
    private final TransactionTemplate ecriture;
    private final TransactionTemplate lecture;

    GenerateurExports(
        ExportFichierRepository exportFichierRepository,
        LecteurExports lecteurExports,
        FabriqueEcrivainsExport fabriqueEcrivainsExport,
        RepertoireExports repertoireExports,
        AppConfigurationService appConfigurationService,
        PlatformTransactionManager transactionManager
    ) {
        this.exportFichierRepository = exportFichierRepository;
        this.lecteurExports = lecteurExports;
        this.fabriqueEcrivainsExport = fabriqueEcrivainsExport;
        this.repertoireExports = repertoireExports;
        this.appConfigurationService = appConfigurationService;
        this.ecriture = new TransactionTemplate(transactionManager);
        this.lecture = new TransactionTemplate(transactionManager);
        this.lecture.setReadOnly(true);
    }

    @Async
    public void generer(Long id) {
        TravailExport travail = ecriture.execute(statut -> commencer(id));
        if (travail == null) {
            return;
        }
        Path chemin = repertoireExports.preparer(travail.fichier());
        try {
            Long lignes = lecture.execute(statut -> ecrire(travail, chemin));
            long taille = Files.size(chemin);
            ecriture.executeWithoutResult(statut -> terminer(id, lignes, taille));
        } catch (RuntimeException | IOException e) {
            LOG.error("Export {} en échec", id, e);
            repertoireExports.supprimer(travail.fichier());
            ecriture.executeWithoutResult(statut -> echouer(id, e));
        }
    }

    private TravailExport commencer(Long id) {
        ExportFichier fichier = exportFichierRepository.findById(id).orElse(null);
        if (fichier == null || fichier.getStatut() != StatutExport.EN_ATTENTE) {
            return null;
        }
        fichier.setStatut(StatutExport.EN_COURS);
        return new TravailExport(fichier.getExport(), fichier.getFormat(), fichier.getDu(), fichier.getAu(), fichier.getFichier());
    }

    private Long ecrire(TravailExport travail, Path chemin) {
        long lignes = 0;
        try (
            OutputStream sortie = new BufferedOutputStream(Files.newOutputStream(chemin));
            EcrivainExport ecrivain = fabriqueEcrivainsExport.ouvrir(travail.format(), travail.export().getColonnes(), sortie);
            Stream<Object[]> flux = lecteurExports.lire(travail.export(), travail.du(), travail.au())
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

    private void terminer(Long id, Long lignes, long taille) {
        ExportFichier fichier = exportFichierRepository.findById(id).orElseThrow();
        LocalDateTime maintenant = LocalDateTime.now();
        fichier
            .setStatut(StatutExport.TERMINE)
            .setNombreLignes(lignes)
            .setTaille(taille)
            .setTermineLe(maintenant)
            .setExpireLe(maintenant.plusDays(appConfigurationService.getRetentionExportsJours()));
    }

    private void echouer(Long id, Exception cause) {
        String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        exportFichierRepository
            .findById(id)
            .ifPresent(fichier ->
                fichier
                    .setStatut(StatutExport.ECHEC)
                    .setTermineLe(LocalDateTime.now())
                    .setErreur(message.length() > ERREUR_MAX ? message.substring(0, ERREUR_MAX) : message)
            );
    }
}
