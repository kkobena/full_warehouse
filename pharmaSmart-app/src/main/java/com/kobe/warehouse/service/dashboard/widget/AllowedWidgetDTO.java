package com.kobe.warehouse.service.dashboard.widget;

/**
 * Widget autorisé pour le rôle de l'utilisateur. {@code licensed = false} : le widget existe mais
 * la licence ne l'inclut pas — le catalogue l'affiche grisé.
 */
public record AllowedWidgetDTO(String key, boolean licensed) {}
