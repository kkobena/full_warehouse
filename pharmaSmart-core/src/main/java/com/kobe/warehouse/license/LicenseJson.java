package com.kobe.warehouse.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Mapper dédié à la sérialisation des claims de licence.
 *
 * <p>Volontairement <strong>indépendant du mapper Spring</strong> : la représentation JSON des
 * claims fait partie du contrat de signature. Un changement de configuration Jackson côté
 * application (inclusion des nulls, format des dates…) ne doit pas pouvoir invalider des licences
 * déjà émises, ni faire diverger le CLI de l'éditeur du vérificateur embarqué.
 */
public final class LicenseJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // Une feature inconnue (émise par un CLI plus récent) est ignorée plutôt que fatale.
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    private LicenseJson() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
