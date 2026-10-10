package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;

/** Répertoire des fichiers exportés : sous-dossier « exports » du répertoire des rapports ({@code file.report}). */
@Component
class RepertoireExports {

    private final Path racine;

    RepertoireExports(FileStorageProperties fileStorageProperties) {
        this.racine = Paths.get(fileStorageProperties.getReportsDir(), "exports").toAbsolutePath().normalize();
    }

    /** Le fichier d'un nom généré par l'application ; refuse tout nom qui sortirait du répertoire. */
    Path resoudre(String nom) {
        Path chemin = racine.resolve(nom).normalize();
        if (!chemin.startsWith(racine)) {
            throw new IllegalArgumentException("Nom de fichier d'export invalide : " + nom);
        }
        return chemin;
    }

    Path preparer(String nom) {
        try {
            Files.createDirectories(racine);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return resoudre(nom);
    }

    void supprimer(String nom) {
        try {
            Files.deleteIfExists(resoudre(nom));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
