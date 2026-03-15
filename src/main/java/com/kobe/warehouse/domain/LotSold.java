package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Snapshot immuable d'un lot vendu, persisté en JSON dans la colonne {@code lots} de {@code sales_line}.
 *
 * <p>Le champ {@code expiryDate} est un snapshot de la date de péremption au moment de la vente.
 * Il permet la traçabilité des lots (rappels, contrôles ANSM/FMD) même si le lot est ultérieurement
 * modifié ou supprimé.</p>
 */
public record LotSold(int id, String numLot, int quantity, LocalDate expiryDate) implements Serializable {}
