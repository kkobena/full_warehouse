package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.TiersPayant;

public interface TiersPayantMapper {
    default TiersPayantDto fromEntity(TiersPayant tiersPayant) {
        return new TiersPayantDto().setAdresse(tiersPayant.getAdresse())
            .setCategorie(tiersPayant.getCategorie())
            .setCodeOrganisme(tiersPayant.getCodeOrganisme())
            .setCodeRegroupement(tiersPayant.getCodeRegroupement())
            .setConsoMensuelle(tiersPayant.getConsoMensuelle())
            .setPlafondConso(tiersPayant.getPlafondConso())
            .setPlafondAbsolu(tiersPayant.getPlafondAbsolu())
            .setPlafondClient(tiersPayant.getPlafondClient())
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
            .setNbreFacture(tiersPayant.getNbreFacture())
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
            .setPlafondClient(tiersPayant.getPlafondClient())
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
            .setNbreFacture(tiersPayant.getNbreFacture())
            .setRemiseForfaitaire(tiersPayant.getRemiseForfaitaire())
            .setTelephone(tiersPayant.getTelephone())
            .setTelephoneFixe(tiersPayant.getTelephoneFixe())
            .setToBeExclude(tiersPayant.getToBeExclude());
    }
}
