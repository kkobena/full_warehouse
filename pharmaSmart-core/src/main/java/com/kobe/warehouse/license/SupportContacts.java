package com.kobe.warehouse.license;

import java.util.List;

/**
 * Coordonnées du revendeur, portées par la licence et reprises telles quelles dans la bannière,
 * les toasts, l'écran d'activation et le {@code payload} des erreurs 402 : un utilisateur bloqué
 * doit toujours avoir un moyen de contact sous les yeux.
 */
public record SupportContacts(String resellerName, List<String> phones, List<String> emails, String whatsapp, String website) {
    public SupportContacts {
        phones = phones == null ? List.of() : List.copyOf(phones);
        emails = emails == null ? List.of() : List.copyOf(emails);
    }

    public static SupportContacts empty() {
        return new SupportContacts(null, List.of(), List.of(), null, null);
    }

    /** Premier téléphone disponible — utilisé dans les messages courts (toast, payload 402). */
    public String primaryPhone() {
        return phones.isEmpty() ? null : phones.getFirst();
    }
}
