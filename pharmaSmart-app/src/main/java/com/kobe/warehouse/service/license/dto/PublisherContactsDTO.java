package com.kobe.warehouse.service.license.dto;

import com.kobe.warehouse.license.LicenseProperties;
import java.util.List;

/**
 * Coordonnées de l'éditeur, à qui le client adresse sa demande d'activation.
 *
 * <p>À ne pas confondre avec les contacts du <em>revendeur</em>, portés par la licence elle-même :
 * ceux-ci viennent de la configuration et restent donc disponibles licence absente ou expirée.
 *
 * @param name   raison sociale affichée
 * @param emails adresses de réception ; la première sert de destinataire à la demande
 * @param phones numéros affichés au client
 */
public record PublisherContactsDTO(String name, List<String> emails, List<String> phones) {

    public static PublisherContactsDTO of(LicenseProperties.Publisher publisher) {
        return new PublisherContactsDTO(publisher.getName(), publisher.getEmails(),
            publisher.getPhones());
    }
}
