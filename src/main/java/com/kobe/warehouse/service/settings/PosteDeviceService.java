package com.kobe.warehouse.service.settings;

import com.kobe.warehouse.domain.enumeration.DeviceType;
import com.kobe.warehouse.service.settings.dto.PosteDeviceRecord;
import java.util.List;
import java.util.Optional;

public interface PosteDeviceService {

    /**
     * Liste tous les périphériques configurés pour un poste.
     */
    List<PosteDeviceRecord> findByPoste(Integer posteId);

    /**
     * Liste les périphériques d'un type donné pour un poste.
     */
    List<PosteDeviceRecord> findByPosteAndType(Integer posteId, DeviceType deviceType);

    /**
     * Récupère le périphérique actif pour un poste et un type donné.
     */
    Optional<PosteDeviceRecord> findActiveDevice(Integer posteId, DeviceType deviceType);

    /**
     * Ajoute ou met à jour un périphérique pour un poste.
     * Si c'est le seul de son type, il est automatiquement activé.
     */
    PosteDeviceRecord save(PosteDeviceRecord record);

    /**
     * Active un périphérique (désactive les autres du même type pour ce poste).
     * C'est une action manuelle de l'utilisateur pour choisir son device préféré.
     */
    void activate(Long deviceId);


    /**
     * Supprime un périphérique configuré.
     */
    void delete(Long deviceId);
}

