package com.kobe.warehouse.service.update;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Un exécutable client publiable aux postes de l'officine.
 *
 * @param version numéro extrait du nom de fichier (ex. {@code 1.4.2})
 * @param fileName nom du fichier tel qu'il est publié et téléchargé
 * @param path emplacement réel sur le poste serveur
 * @param publishedAt date de dépôt, exposée à titre informatif dans le manifeste
 */
public record ClientUpdateArtifact(String version, String fileName, Path path, Instant publishedAt) {}
