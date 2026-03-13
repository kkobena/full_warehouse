package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service d'import CSV des quantités d'inventaire.
 * Remplace importDetail() de InventaireServiceImpl avec :
 *  - validation ligne par ligne (format, colonnes manquantes)
 *  - rapport détaillé : importées / ignorées (CIP inconnu) / rejetées (format invalide)
 *  - filtre par storeInventoryId (évite de mettre à jour d'autres inventaires ouverts)
 */
public interface InventaireImportService {

    /**
     * Importe un fichier CSV (format : codeCIP;quantité, délimiteur ';').
     * Retourne un rapport d'import avec le détail des erreurs ligne par ligne.
     */
    ImportResultRecord importDetail(Long storeInventoryId, MultipartFile multipartFile);
}
