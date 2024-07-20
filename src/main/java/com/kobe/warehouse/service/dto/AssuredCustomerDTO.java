package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AssuredCustomerDTO extends CustomerDTO {
  private RemiseClient remise;
  private String sexe;

  @JsonDeserialize(using = LocalDateDeserializer.class)
  @PastOrPresent
  private LocalDate datNaiss;

  private String num;
  private Long assureId;
  private String numAyantDroit;
  private Long remiseId;
  private Long tiersPayantId;
  private Long plafondConso;
  private Long plafondJournalier;
  private PrioriteTiersPayant priorite;
  private Integer taux;
  private Boolean plafondAbsolu;
  private List<AssuredCustomerDTO> ayantDroits = new ArrayList<>();
  private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
  private TiersPayantDto tiersPayant;

  public AssuredCustomerDTO(
      AssuredCustomer customer,
      List<ClientTiersPayantDTO> clientTiersPayants,
      List<AssuredCustomerDTO> ayantDroits) {
    super(customer);
    this.remise = customer.getRemise();
    this.sexe = customer.getSexe();
    this.numAyantDroit = customer.getNumAyantDroit();
    this.tiersPayants =
        clientTiersPayants.stream()
            .sorted(
                Comparator.comparing(ClientTiersPayantDTO::getCategorie, Comparator.naturalOrder()))
            .toList();
    this.ayantDroits = ayantDroits;
    this.datNaiss = customer.getDatNaiss();
    if (!this.tiersPayants.isEmpty()) {
      Optional<ClientTiersPayantDTO> clientTiersPayantOp =
          this.tiersPayants.stream()
              .filter(t -> t.getPriorite() == PrioriteTiersPayant.T0)
              .findFirst();
      if (clientTiersPayantOp.isPresent()) {
        ClientTiersPayantDTO pr = clientTiersPayantOp.get();
        this.tiersPayantId = pr.getTiersPayantId();
        this.plafondConso = pr.getPlafondConso();
        this.plafondJournalier = pr.getPlafondJournalier();
        this.priorite = pr.getPriorite();
        this.taux = pr.getTaux();
        this.num = pr.getNum();
        this.plafondAbsolu = pr.getPlafondAbsolu();
        this.tiersPayant = buildTiersPayan(pr);
      }
    }
  }

  public AssuredCustomerDTO() {
    super();
  }

  public AssuredCustomerDTO(AssuredCustomer customer) {
    super(customer);
    this.num = customer.getNumAyantDroit();
    this.numAyantDroit = customer.getNumAyantDroit();
    this.datNaiss = customer.getDatNaiss();
  }

  public TiersPayantDto getTiersPayant() {
    return tiersPayant;
  }

  public AssuredCustomerDTO setTiersPayant(TiersPayantDto tiersPayant) {
    this.tiersPayant = tiersPayant;
    return this;
  }

  private TiersPayantDto buildTiersPayan(ClientTiersPayantDTO tiersPayant) {

    return new TiersPayantDto()
        .setId(tiersPayant.getTiersPayantId())
        .setName(tiersPayant.getTiersPayantName())
        .setFullName(tiersPayant.getTiersPayantFullName());
  }

  public Long getRemiseId() {
    return remiseId;
  }

  public AssuredCustomerDTO setRemiseId(Long remiseId) {
    this.remiseId = remiseId;
    return this;
  }

  public Boolean getPlafondAbsolu() {
    return plafondAbsolu;
  }

  public AssuredCustomerDTO setPlafondAbsolu(Boolean plafondAbsolu) {
    this.plafondAbsolu = plafondAbsolu;
    return this;
  }

  public Long getTiersPayantId() {
    return tiersPayantId;
  }

  public AssuredCustomerDTO setTiersPayantId(Long tiersPayantId) {
    this.tiersPayantId = tiersPayantId;
    return this;
  }

  public Long getPlafondConso() {
    return plafondConso;
  }

  public AssuredCustomerDTO setPlafondConso(Long plafondConso) {
    this.plafondConso = plafondConso;
    return this;
  }

  public Long getPlafondJournalier() {
    return plafondJournalier;
  }

  public AssuredCustomerDTO setPlafondJournalier(Long plafondJournalier) {
    this.plafondJournalier = plafondJournalier;
    return this;
  }

  public PrioriteTiersPayant getPriorite() {
    return priorite;
  }

  public AssuredCustomerDTO setPriorite(PrioriteTiersPayant priorite) {
    this.priorite = priorite;
    return this;
  }

  public Integer getTaux() {
    return taux;
  }

  public AssuredCustomerDTO setTaux(Integer taux) {
    this.taux = taux;
    return this;
  }

  public String getNum() {
    //   this.tiersPayants.stream().filter(t->t.getPriorite()==
    // PrioriteTiersPayant.T0).findFirst().ifPresent(tp->num=tp.getNum());
    return num;
  }

  public AssuredCustomerDTO setNum(String num) {
    this.num = num;
    return this;
  }

  public String getNumAyantDroit() {
    return numAyantDroit;
  }

  public AssuredCustomerDTO setNumAyantDroit(String numAyantDroit) {
    this.numAyantDroit = numAyantDroit;
    return this;
  }

  public List<AssuredCustomerDTO> getAyantDroits() {
    return ayantDroits;
  }

  public AssuredCustomerDTO setAyantDroits(List<AssuredCustomerDTO> ayantDroits) {
    this.ayantDroits = ayantDroits;
    return this;
  }

  public List<ClientTiersPayantDTO> getTiersPayants() {
    return tiersPayants;
  }

  public AssuredCustomerDTO setTiersPayants(List<ClientTiersPayantDTO> tiersPayants) {
    this.tiersPayants = tiersPayants;
    return this;
  }

  public Long getAssureId() {
    return assureId;
  }

  public AssuredCustomerDTO setAssureId(Long assureId) {
    this.assureId = assureId;
    return this;
  }

  public RemiseClient getRemise() {
    return remise;
  }

  public AssuredCustomerDTO setRemise(RemiseClient remise) {
    this.remise = remise;
    return this;
  }

  public String getSexe() {
    return sexe;
  }

  public AssuredCustomerDTO setSexe(String sexe) {
    this.sexe = sexe;
    return this;
  }

  public LocalDate getDatNaiss() {
    return datNaiss;
  }

  public AssuredCustomerDTO setDatNaiss(LocalDate datNaiss) {
    this.datNaiss = datNaiss;
    return this;
  }
}
