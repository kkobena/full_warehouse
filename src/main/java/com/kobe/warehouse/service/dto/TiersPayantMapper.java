package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.time.LocalDateTime;
import java.util.Objects;

public interface TiersPayantMapper {
    default TiersPayantDto fromEntity(TiersPayant tiersPayant) {
        return new TiersPayantDto()
            .setAdresse(tiersPayant.getAdresse())
            .setCategorie(tiersPayant.getCategorie())
            .setCodeOrganisme(tiersPayant.getCodeOrganisme())
            .setConsoMensuelle(tiersPayant.getConsoMensuelle())
            .setPlafondConso(tiersPayant.getPlafondConso())
            .setPlafondAbsolu(tiersPayant.getPlafondAbsolu())
            .setCreated(tiersPayant.getCreated())
            .setUpdated(tiersPayant.getUpdated())
            .setEmail(tiersPayant.getEmail())
            .setName(tiersPayant.getName())
            .setFullName(tiersPayant.getFullName())
            .setId(tiersPayant.getId())
            .setGroupeTiersPayant(tiersPayant.getGroupeTiersPayant())
            .setMontantMaxParFcture(tiersPayant.getMontantMaxParFcture())
            .setNbreBons(tiersPayant.getNbreBons())
            .setNbreBordereaux(tiersPayant.getNbreBordereaux())
            .setRemiseForfaitaire(tiersPayant.getRemiseForfaitaire())
            .setTelephone(tiersPayant.getTelephone())
            .setTelephoneFixe(tiersPayant.getTelephoneFixe())
            .setToBeExclude(tiersPayant.getToBeExclude())
            .setModelFacture(tiersPayant.getModelFacture())
            .setStatut(tiersPayant.getStatut());
    }

    default TiersPayant entityFromDto(TiersPayantDto payantDto) {
        return new TiersPayant()
            .setAdresse(payantDto.getAdresse())
            .setCategorie(payantDto.getCategorie())
            .setCodeOrganisme(payantDto.getCodeOrganisme())
            .setConsoMensuelle(payantDto.getConsoMensuelle())
            .setPlafondConso(payantDto.getPlafondConso())
            .setPlafondAbsolu(payantDto.getPlafondAbsolu())
            .setCreated(LocalDateTime.now())
            .setUpdated(LocalDateTime.now())
            .setEmail(payantDto.getEmail())
            .setName(payantDto.getName())
            .setGroupeTiersPayant(fromId(payantDto.getGroupeTiersPayantId()))
            .setFullName(payantDto.getFullName())
            .setMontantMaxParFcture(payantDto.getMontantMaxParFcture())
            .setNbreBons(payantDto.getNbreBons())
            .setNbreBordereaux(payantDto.getNbreBordereaux())
            .setRemiseForfaitaire(payantDto.getRemiseForfaitaire())
            .setTelephone(payantDto.getTelephone())
            .setTelephoneFixe(payantDto.getTelephoneFixe())
            .setToBeExclude(payantDto.getToBeExclude())
            .setModelFacture(payantDto.getModelFacture());
    }

    default GroupeTiersPayant fromId(Long groupeTiersPayantId) {
        if (groupeTiersPayantId == null) {
            return null;
        }
        return new GroupeTiersPayant().setId(groupeTiersPayantId);
    }

    default TiersPayant entityFromDto(TiersPayantDto dto, TiersPayant tiersPayant) {

        return tiersPayant
            .setAdresse(dto.getAdresse())
            .setCategorie(dto.getCategorie())
            .setCodeOrganisme(dto.getCodeOrganisme())
            .setConsoMensuelle(dto.getConsoMensuelle())
            .setPlafondConso(dto.getPlafondConso())
            .setPlafondAbsolu(dto.getPlafondAbsolu())
            .setUpdated(LocalDateTime.now())
            .setEmail(dto.getEmail())
            .setName(dto.getName())
            .setGroupeTiersPayant(fromId(dto.getGroupeTiersPayantId()))
            .setFullName(dto.getFullName())
            .setMontantMaxParFcture(dto.getMontantMaxParFcture())
            .setNbreBons(dto.getNbreBons())
            .setNbreBordereaux(dto.getNbreBordereaux())
            .setRemiseForfaitaire(dto.getRemiseForfaitaire())
            .setTelephone(dto.getTelephone())
            .setTelephoneFixe(dto.getTelephoneFixe())
            .setToBeExclude(dto.getToBeExclude())
            .setModelFacture(dto.getModelFacture());
    }
}
