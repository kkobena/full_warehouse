package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ClientTiersPayantDTO {
    private Long id;
    private String tiersPayantName;
    private String tiersPayantFullName;
    private Long tiersPayantId;
    private String num;
    private Long plafondConso;
    private Long plafondJournalier;
    private LocalDateTime created;
    private LocalDateTime updated;
    private PrioriteTiersPayant priorite;
    private TiersPayantStatut statut;
    private int categorie;
    private Integer taux;
    private Boolean plafondAbsolu;
    private TiersPayantDto tiersPayant;
    private TiersPayantCategorie typeTiersPayant;
    private String numBon;

    public ClientTiersPayantDTO() {
    }

    public ClientTiersPayantDTO(ClientTiersPayant c) {
        this.id = c.getId();
        TiersPayant tiersPayant = c.getTiersPayant();
        this.tiersPayantName = tiersPayant.getName();
        this.tiersPayantFullName = tiersPayant.getFullName();
        this.num = c.getNum();
        this.plafondConso = c.getPlafondConso();
        this.plafondJournalier = c.getPlafondJournalier();
        this.created = c.getCreated();
        this.updated = c.getUpdated();
        this.priorite = c.getPriorite();
        this.categorie = c.getPriorite().getValue();
        this.statut = c.getStatut();
        this.taux = c.getTaux();
        this.plafondAbsolu = c.getPlafondAbsolu();
        this.tiersPayantId = tiersPayant.getId();
        this.typeTiersPayant = tiersPayant.getCategorie();
        this.tiersPayant = new TiersPayantDto().setName(tiersPayant.getName()).setId(tiersPayant.getId()).setFullName(tiersPayant.getFullName());
    }

  public ClientTiersPayantDTO setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

  public ClientTiersPayantDTO setTiersPayant(TiersPayantDto tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

  public ClientTiersPayantDTO setCategorie(int categorie) {
        this.categorie = categorie;
        return this;
    }

  public ClientTiersPayantDTO setTypeTiersPayant(TiersPayantCategorie typeTiersPayant) {
        this.typeTiersPayant = typeTiersPayant;
        return this;
    }

  public ClientTiersPayantDTO setId(Long id) {
        this.id = id;
        return this;
    }

  public ClientTiersPayantDTO setTiersPayantName(String tiersPayantName) {
        this.tiersPayantName = tiersPayantName;
        return this;
    }

  public ClientTiersPayantDTO setTiersPayantFullName(String tiersPayantFullName) {
        this.tiersPayantFullName = tiersPayantFullName;
        return this;
    }

  public ClientTiersPayantDTO setNum(String num) {
        this.num = num;
        return this;
    }

  public ClientTiersPayantDTO setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

  public ClientTiersPayantDTO setPlafondJournalier(Long plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
        return this;
    }

  public ClientTiersPayantDTO setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public ClientTiersPayantDTO setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

  public ClientTiersPayantDTO setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
        return this;
    }

  public ClientTiersPayantDTO setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

  public ClientTiersPayantDTO setTaux(Integer taux) {
        this.taux = taux;
        return this;
    }

  public ClientTiersPayantDTO setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

  public ClientTiersPayantDTO setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }
}
