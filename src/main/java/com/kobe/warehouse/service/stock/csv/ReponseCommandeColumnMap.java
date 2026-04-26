package com.kobe.warehouse.service.stock.csv;

/**
 * Mapping des colonnes d'un fichier de réponse grossiste (accusé de réception).
 * cipCol : index 0-based de la colonne contenant le code CIP
 * qteCol : index 0-based de la colonne contenant la quantité confirmée
 * hasHeader : true si la première ligne est un en-tête à ignorer
 */
public record ReponseCommandeColumnMap(int cipCol, int qteCol, boolean hasHeader) {}
