package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.ExportFichierRepository;
import com.kobe.warehouse.service.exports.modele.TravailExport;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Génère un fichier d'export en tâche de fond : un export de données se lit en flux (lot de 500) et s'écrit ligne à ligne, une
 * édition est bâtie d'un bloc par le service de son écran. Le statut est tenu à jour dans l'historique ; un échec garde sa trace
 * (message) et ne laisse pas de fichier partiel.
 *
 * <p>Le contexte de sécurité du demandeur est reporté sur le fil de la tâche : les services du pilotage n'ouvrent que les
 * indicateurs et les axes que ses droits autorisent, et une édition doit être bâtie avec les droits de qui l'a demandée.
 */
@Component
class GenerateurExports {

    private static final Logger LOG = LoggerFactory.getLogger(GenerateurExports.class);
    private static final int ERREUR_MAX = 500;

    private final ExportFichierRepository exportFichierRepository;
    private final EcritureExport ecritureExport;
    private final RepertoireExports repertoireExports;
    private final AppConfigurationService appConfigurationService;
    private final TransactionTemplate ecriture;
    private final TransactionTemplate lecture;

    GenerateurExports(
        ExportFichierRepository exportFichierRepository,
        EcritureExport ecritureExport,
        RepertoireExports repertoireExports,
        AppConfigurationService appConfigurationService,
        PlatformTransactionManager transactionManager
    ) {
        this.exportFichierRepository = exportFichierRepository;
        this.ecritureExport = ecritureExport;
        this.repertoireExports = repertoireExports;
        this.appConfigurationService = appConfigurationService;
        this.ecriture = new TransactionTemplate(transactionManager);
        this.lecture = new TransactionTemplate(transactionManager);
        this.lecture.setReadOnly(true);
    }

    /**
     * @param contexte le contexte de sécurité du demandeur, ou {@code null} pour un export programmé — une édition n'étant pas
     *     programmable, elle ne se génère jamais sans contexte
     */
    @Async
    public void generer(Long id, SecurityContext contexte) {
        if (contexte != null) {
            SecurityContextHolder.setContext(contexte);
        }
        try {
            produire(id);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void produire(Long id) {
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
        return new TravailExport(
            fichier.getExport(),
            fichier.getFormat(),
            fichier.getDu(),
            fichier.getAu(),
            fichier.getFiltres(),
            fichier.getFichier()
        );
    }

    /** @return le nombre de lignes écrites, inconnu (et donc vide) pour une édition bâtie d'un bloc */
    private Long ecrire(TravailExport travail, Path chemin) {
        try (OutputStream sortie = new BufferedOutputStream(Files.newOutputStream(chemin))) {
            return ecritureExport.ecrire(travail.export(), travail.format(), travail.du(), travail.au(), travail.filtres(), sortie);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
