package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import java.time.LocalDateTime;

/**
 * Mapper interface for converting between TiersPayant entity and TiersPayantDto.
 * Provides bidirectional mapping with support for creating new entities or updating existing ones.
 */
public interface TiersPayantMapper {

    /**
     * Converts a TiersPayant entity to a TiersPayantDto.
     *
     * @param tiersPayant the entity to convert
     * @return the corresponding DTO
     */
    default TiersPayantDto fromEntity(TiersPayant tiersPayant) {
        return new TiersPayantDto()
            .setId(tiersPayant.getId())
            .setName(tiersPayant.getName())
            .setFullName(tiersPayant.getFullName())
            .setNcc(tiersPayant.getNcc())
            .setAdresse(tiersPayant.getAdresse())
            .setCategorie(tiersPayant.getCategorie())
            .setStatut(tiersPayant.getStatut())
            .setCodeOrganisme(tiersPayant.getCodeOrganisme())
            .setConsoMensuelle(tiersPayant.getConsoMensuelle())
            .setPlafondConso(tiersPayant.getPlafondConso())
            .setPlafondAbsolu(tiersPayant.isPlafondAbsolu())
            .setPlafondAbsoluClient(tiersPayant.isPlafondAbsoluClient())
            .setPlafondConsoClient(tiersPayant.getPlafondConsoClient())
            .setPlafondJournalierClient(tiersPayant.getPlafondJournalierClient())
            .setMontantMaxParFcture(tiersPayant.getMontantMaxParFcture())
            .setNbreBons(tiersPayant.getNbreBons())
            .setNbreBordereaux(tiersPayant.getNbreBordereaux())
            .setRemiseForfaitaire(tiersPayant.getRemiseForfaitaire())
            .setEmail(tiersPayant.getEmail())
            .setTelephone(tiersPayant.getTelephone())
            .setTelephoneFixe(tiersPayant.getTelephoneFixe())
            .setToBeExclude(tiersPayant.isBeExclude())
            .setModelFacture(tiersPayant.getModelFacture())
            .setGroupeTiersPayant(tiersPayant.getGroupeTiersPayant())
            .setCreated(tiersPayant.getCreated())
            .setUpdated(tiersPayant.getUpdated());
    }

    /**
     * Creates a new TiersPayant entity from a TiersPayantDto.
     * Sets the created and updated timestamps to the current time.
     *
     * @param dto the DTO to convert
     * @return a new TiersPayant entity
     */
    default TiersPayant entityFromDto(TiersPayantDto dto) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setCreated(LocalDateTime.now());
        return mapDtoToEntity(dto, tiersPayant);
    }

    /**
     * Updates an existing TiersPayant entity from a TiersPayantDto.
     * Preserves the original created timestamp and updates the updated timestamp.
     *
     * @param dto the DTO with updated data
     * @param tiersPayant the existing entity to update
     * @return the updated TiersPayant entity
     */
    default TiersPayant entityFromDto(TiersPayantDto dto, TiersPayant tiersPayant) {
        return mapDtoToEntity(dto, tiersPayant);
    }

    /**
     * Maps all fields from a DTO to an entity.
     * This method contains the common mapping logic used by both create and update operations.
     *
     * @param dto the source DTO
     * @param entity the target entity
     * @return the updated entity
     */
    private TiersPayant mapDtoToEntity(TiersPayantDto dto, TiersPayant entity) {
        entity
            .setNcc(dto.getNcc())
            .setName(dto.getName())
            .setFullName(dto.getFullName())
            .setAdresse(dto.getAdresse())
            .setCategorie(dto.getCategorie())
            .setCodeOrganisme(dto.getCodeOrganisme())
            .setConsoMensuelle(dto.getConsoMensuelle())
            .setPlafondConso(dto.getPlafondConso())
            .setPlafondAbsolu(dto.isPlafondAbsolu())
            .setPlafondAbsoluClient(dto.isPlafondAbsoluClient())
            .setPlafondConsoClient(dto.getPlafondConsoClient())
            .setPlafondJournalierClient(dto.getPlafondJournalierClient())
            .setMontantMaxParFcture(dto.getMontantMaxParFcture())
            .setNbreBons(dto.getNbreBons())
            .setNbreBordereaux(dto.getNbreBordereaux())
            .setRemiseForfaitaire(dto.getRemiseForfaitaire())
            .setEmail(dto.getEmail())
            .setTelephone(dto.getTelephone())
            .setTelephoneFixe(dto.getTelephoneFixe())
            .setBeExclude(dto.isToBeExclude())
            .setModelFacture(dto.getModelFacture())
            .setGroupeTiersPayant(fromId(dto.getGroupeTiersPayantId()));

        entity.setUpdated(LocalDateTime.now());

        return entity;
    }

    /**
     * Creates a GroupeTiersPayant entity reference from an ID.
     * This is useful for setting relationships without loading the full entity.
     *
     * @param groupeTiersPayantId the ID of the group
     * @return a GroupeTiersPayant with only the ID set, or null if the ID is null
     */
    default GroupeTiersPayant fromId(Long groupeTiersPayantId) {
        if (groupeTiersPayantId == null) {
            return null;
        }
        return new GroupeTiersPayant().setId(groupeTiersPayantId);
    }
}
