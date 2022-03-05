package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;

import java.time.Instant;

public interface TiersPayantMapper {
    default TiersPayantDto fromEntity(TiersPayant tiersPayant) {
        return new TiersPayantDto().setAdresse(tiersPayant.getAdresse())
            .setCategorie(tiersPayant.getCategorie())
            .setCodeOrganisme(tiersPayant.getCodeOrganisme())
            .setCodeRegroupement(tiersPayant.getCodeRegroupement())
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
            .setStatut(tiersPayant.getStatut());

    }

    default TiersPayant entityFromDto(TiersPayantDto tiersPayant) {
        return new TiersPayant().setAdresse(tiersPayant.getAdresse())
            .setCategorie(tiersPayant.getCategorie())
            .setCodeOrganisme(tiersPayant.getCodeOrganisme())
            .setCodeRegroupement(tiersPayant.getCodeRegroupement())
            .setConsoMensuelle(tiersPayant.getConsoMensuelle())
            .setPlafondConso(tiersPayant.getPlafondConso())
            .setPlafondAbsolu(tiersPayant.getPlafondAbsolu())
            .setCreated(Instant.now())
            .setUpdated(Instant.now())
            .setEmail(tiersPayant.getEmail())
            .setName(tiersPayant.getName())
            .setGroupeTiersPayant(fromId(tiersPayant.getGroupeTiersPayantId()))
            .setFullName(tiersPayant.getFullName())
            .setMontantMaxParFcture(tiersPayant.getMontantMaxParFcture())
            .setNbreBons(tiersPayant.getNbreBons())
            .setNbreBordereaux(tiersPayant.getNbreBordereaux())
            .setRemiseForfaitaire(tiersPayant.getRemiseForfaitaire())
            .setTelephone(tiersPayant.getTelephone())
            .setTelephoneFixe(tiersPayant.getTelephoneFixe())
            .setToBeExclude(tiersPayant.getToBeExclude());
    }

    default GroupeTiersPayant fromId(Long groupeTiersPayantId) {
        if (groupeTiersPayantId == null) return null;
        return new GroupeTiersPayant().setId(groupeTiersPayantId);
    }

    default TiersPayant entityFromDto(TiersPayantDto dto,TiersPayant tiersPayant) {
        return tiersPayant.setAdresse(dto.getAdresse())
            .setCategorie(dto.getCategorie())
            .setCodeOrganisme(dto.getCodeOrganisme())
            .setCodeRegroupement(tiersPayant.getCodeRegroupement())
            .setConsoMensuelle(dto.getConsoMensuelle())
            .setPlafondConso(dto.getPlafondConso())
            .setPlafondAbsolu(dto.getPlafondAbsolu())
            .setUpdated(Instant.now())
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
            .setToBeExclude(dto.getToBeExclude());
    }

}
