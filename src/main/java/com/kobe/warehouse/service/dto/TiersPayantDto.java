package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.OrdreTrisFacture;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TiersPayantDto implements Serializable {

    private Long id;
    private String name;
    private String fullName;
    private Integer nbreBons;
    private Long montantMaxParFcture;
    private Integer nbreFacture;

    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String codeOrganisme;

    private Long consoMensuelle;
    private Boolean plafondAbsolu;
    private String adresse;
    private String telephone;
    private String telephoneFixe;
    private String email;
    private Boolean toBeExclude;
    private Long plafondConso;
    private Long plafondClient;
    private TiersPayantStatut statut;
    private TiersPayantCategorie categorie;
    private Long remiseForfaitaire;
    private Integer nbreBordereaux;
    private LocalDateTime created;
    private LocalDateTime updated;
    private GroupeTiersPayant groupeTiersPayant;
    private String groupeTiersPayantName;
    private Long groupeTiersPayantId;
    private String modelFacture;
    private OrdreTrisFacture ordreTrisFacture;
    private List<AssuredCustomerDTO> clients = new ArrayList<>();


    public TiersPayantDto() {}

    public String getModelFacture() {
        return modelFacture;
    }

    public TiersPayantDto setModelFacture(String modelFacture) {
        this.modelFacture = modelFacture;
        return this;
    }


    public Long getId() {
        return id;
    }

    public TiersPayantDto setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public TiersPayantDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public TiersPayantDto setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Integer getNbreBons() {
        return nbreBons;
    }

    public TiersPayantDto setNbreBons(Integer nbreBons) {
        this.nbreBons = nbreBons;
        return this;
    }

    public Long getMontantMaxParFcture() {
        return montantMaxParFcture;
    }

    public TiersPayantDto setMontantMaxParFcture(Long montantMaxParFcture) {
        this.montantMaxParFcture = montantMaxParFcture;
        return this;
    }

    public Integer getNbreFacture() {
        return nbreFacture;
    }

    public TiersPayantDto setNbreFacture(Integer nbreFacture) {
        this.nbreFacture = nbreFacture;
        return this;
    }

    public String getCodeOrganisme() {
        return codeOrganisme;
    }

    public TiersPayantDto setCodeOrganisme(String codeOrganisme) {
        this.codeOrganisme = codeOrganisme;
        return this;
    }

    public Long getConsoMensuelle() {
        return consoMensuelle;
    }

    public TiersPayantDto setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

    public Boolean getPlafondAbsolu() {
        return plafondAbsolu;
    }

    public TiersPayantDto setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }

    public String getAdresse() {
        return adresse;
    }

    public TiersPayantDto setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public TiersPayantDto setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public TiersPayantDto setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TiersPayantDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getToBeExclude() {
        return toBeExclude;
    }

    public TiersPayantDto setToBeExclude(Boolean toBeExclude) {
        this.toBeExclude = toBeExclude;
        return this;
    }

    public Long getPlafondConso() {
        return plafondConso;
    }

    public TiersPayantDto setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

    public Long getPlafondClient() {
        return plafondClient;
    }

    public TiersPayantDto setPlafondClient(Long plafondClient) {
        this.plafondClient = plafondClient;
        return this;
    }

    public TiersPayantStatut getStatut() {
        return statut;
    }

    public TiersPayantDto setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public TiersPayantCategorie getCategorie() {
        return categorie;
    }

    public TiersPayantDto setCategorie(TiersPayantCategorie categorie) {
        this.categorie = categorie;
        return this;
    }

    public Long getRemiseForfaitaire() {
        return remiseForfaitaire;
    }

    public TiersPayantDto setRemiseForfaitaire(Long remiseForfaitaire) {
        this.remiseForfaitaire = remiseForfaitaire;
        return this;
    }

    public Integer getNbreBordereaux() {
        return nbreBordereaux;
    }

    public TiersPayantDto setNbreBordereaux(Integer nbreBordereaux) {
        this.nbreBordereaux = nbreBordereaux;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public TiersPayantDto setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public TiersPayantDto setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public GroupeTiersPayant getGroupeTiersPayant() {
        return groupeTiersPayant;
    }

    public TiersPayantDto setGroupeTiersPayant(GroupeTiersPayant groupeTiersPayant) {
        this.groupeTiersPayant = groupeTiersPayant;
        return this;
    }

    public String getGroupeTiersPayantName() {
        return groupeTiersPayantName;
    }

    public TiersPayantDto setGroupeTiersPayantName(String groupeTiersPayantName) {
        this.groupeTiersPayantName = groupeTiersPayantName;
        return this;
    }

    public Long getGroupeTiersPayantId() {
        return groupeTiersPayantId;
    }

    public TiersPayantDto setGroupeTiersPayantId(Long groupeTiersPayantId) {
        this.groupeTiersPayantId = groupeTiersPayantId;
        return this;
    }

    public List<AssuredCustomerDTO> getClients() {
        return clients;
    }

    public TiersPayantDto setClients(List<AssuredCustomerDTO> clients) {
        this.clients = clients;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TiersPayantDto entity = (TiersPayantDto) o;
        return Objects.equals(this.id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public TiersPayantDto buildLite(TiersPayant tiersPayant) {
        return this.setId(tiersPayant.getId()).setName(tiersPayant.getName()).setFullName(tiersPayant.getFullName());
    }
}
