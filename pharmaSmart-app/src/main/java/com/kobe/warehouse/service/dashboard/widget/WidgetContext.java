package com.kobe.warehouse.service.dashboard.widget;

import java.time.LocalDate;

/**
 * Contexte commun à tous les widgets d'un dashboard : période, magasin et utilisateur connecté.
 *
 * <p>{@code login} est toujours posé par le serveur, jamais reçu du client : les widgets « moi »
 * filtrent dessus.
 */
public record WidgetContext(LocalDate startDate, LocalDate endDate, Integer magasinId, String login) {}
