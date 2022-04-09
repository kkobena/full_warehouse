package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;

import java.time.Instant;

public class ClientTiersPayantDTO {
    private Long id;
    private String tiersPayantName;
    private String tiersPayantFullName;
    private Long tiersPayantId;
    private String num;
    private Long plafondConso;
    private Long plafondJournalier;
    private Instant created;
    private Instant updated;
    private PrioriteTiersPayant priorite;
    private TiersPayantStatut statut;
    private int  categorie;
    private Integer taux;
    private Boolean plafondAbsolu;
    private TiersPayantDto tiersPayant;
    public TiersPayantDto getTiersPayant() {
        return tiersPayant;
    }

    public int getCategorie() {
        return categorie;
    }

    public ClientTiersPayantDTO setCategorie(int categorie) {
        this.categorie = categorie;
        return this;
    }

    public ClientTiersPayantDTO setTiersPayant(TiersPayantDto tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }



    public Long getId() {
        return id;
    }

    public ClientTiersPayantDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTiersPayantName() {
        return tiersPayantName;
    }

    public ClientTiersPayantDTO setTiersPayantName(String tiersPayantName) {
        this.tiersPayantName = tiersPayantName;
        return this;
    }

    public String getTiersPayantFullName() {
        return tiersPayantFullName;
    }

    public ClientTiersPayantDTO setTiersPayantFullName(String tiersPayantFullName) {
        this.tiersPayantFullName = tiersPayantFullName;
        return this;
    }

    public String getNum() {
        return num;
    }

    public ClientTiersPayantDTO setNum(String num) {
        this.num = num;
        return this;
    }

    public Long getPlafondConso() {
        return plafondConso;
    }

    public ClientTiersPayantDTO setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

    public Long getPlafondJournalier() {
        return plafondJournalier;
    }

    public ClientTiersPayantDTO setPlafondJournalier(Long plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public ClientTiersPayantDTO setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public ClientTiersPayantDTO setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public PrioriteTiersPayant getPriorite() {
        return priorite;
    }

    public ClientTiersPayantDTO setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
        return this;
    }

    public TiersPayantStatut getStatut() {
        return statut;
    }

    public ClientTiersPayantDTO setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public Integer getTaux() {
        return taux;
    }

    public ClientTiersPayantDTO setTaux(Integer taux) {
        this.taux = taux;
        return this;
    }

    public ClientTiersPayantDTO() {
    }

    public Long getTiersPayantId() {
        return tiersPayantId;
    }

    public ClientTiersPayantDTO setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

    public Boolean getPlafondAbsolu() {
        return plafondAbsolu;
    }

    public ClientTiersPayantDTO setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
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
        this.categorie=c.getPriorite().getValue();
        this.statut = c.getStatut();
        this.taux = c.getTaux();
        this.plafondAbsolu = c.getPlafondAbsolu();
        this.tiersPayantId = tiersPayant.getId();
        this.tiersPayant = new TiersPayantDto().setName(tiersPayant.getName()).setId(tiersPayant.getId()).setFullName(tiersPayant.getFullName());
    }
}
