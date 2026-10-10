package com.kobe.warehouse.service.dto.exports;

import java.nio.file.Path;

/** Fichier prêt à télécharger : son emplacement, le nom proposé au navigateur, son type. */
public record FichierExporteDTO(Path chemin, String nom, String typeMime) {}
