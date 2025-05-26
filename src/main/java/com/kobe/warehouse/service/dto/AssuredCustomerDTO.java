package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private Integer plafondConso;
    private Integer plafondJournalier;
    private PrioriteTiersPayant priorite;
    private Integer taux;
    private boolean plafondAbsolu;
    private List<AssuredCustomerDTO> ayantDroits = new ArrayList<>();
    private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
    private TiersPayantDto tiersPayant;
    private TiersPayantCategorie typeTiersPayant;

    public AssuredCustomerDTO(
        AssuredCustomer customer,
        List<ClientTiersPayantDTO> clientTiersPayants,
        List<AssuredCustomerDTO> ayantDroits
    ) {
        super(customer);
        this.remise = customer.getRemise();
        this.sexe = customer.getSexe();
        this.numAyantDroit = customer.getNumAyantDroit();
        this.tiersPayants = clientTiersPayants
            .stream()
            .sorted(Comparator.comparing(ClientTiersPayantDTO::getCategorie, Comparator.naturalOrder()))
            .toList();
        this.ayantDroits = ayantDroits;
        this.datNaiss = customer.getDatNaiss();
        if (!this.tiersPayants.isEmpty()) {
            Optional<ClientTiersPayantDTO> clientTiersPayantOp =
                this.tiersPayants.stream().filter(t -> t.getPriorite() == PrioriteTiersPayant.R0).findFirst();
            if (clientTiersPayantOp.isPresent()) {
                ClientTiersPayantDTO pr = clientTiersPayantOp.get();
                this.tiersPayantId = pr.getTiersPayantId();
                this.plafondConso = pr.getPlafondConso();
                this.plafondJournalier = pr.getPlafondJournalier();
                this.priorite = pr.getPriorite();
                this.taux = pr.getTaux();
                this.num = pr.getNum();
                this.plafondAbsolu = pr.isPlafondAbsolu();
                this.tiersPayant = buildTiersPayan(pr);
                this.typeTiersPayant = pr.getTypeTiersPayant();
            }
            if (Objects.isNull(this.typeTiersPayant)) {
                this.typeTiersPayant = this.tiersPayants.getFirst().getTypeTiersPayant();
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
        /*  this.tiersPayants = customer.getClientTiersPayants()
            .stream().map(ClientTiersPayantDTO::new)
            .sorted(Comparator.comparing(ClientTiersPayantDTO::getCategorie, Comparator.naturalOrder()))
            .toList();*/
    }

    private void updateAssuranceInfo(AssuredCustomerDTO assuredCustomer, AssuredCustomer customer) {
        customer
            .getClientTiersPayants()
            .forEach(c -> {
                var tp = new ClientTiersPayantDTO();
                TiersPayant tiersPayant = c.getTiersPayant();
                tp.setId(c.getId());
                tp.setNum(c.getNum());
                tp.setTiersPayantFullName(tiersPayant.getFullName());
                tp.setPriorite(c.getPriorite());
                assuredCustomer.getTiersPayants().add(tp);
            });
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

    public Long getTiersPayantId() {
        return tiersPayantId;
    }

    public AssuredCustomerDTO setTiersPayantId(Long tiersPayantId) {
        this.tiersPayantId = tiersPayantId;
        return this;
    }

    public boolean isPlafondAbsolu() {
        return plafondAbsolu;
    }

    public void setPlafondAbsolu(boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
    }

    public Integer getPlafondConso() {
        return plafondConso;
    }

    public void setPlafondConso(Integer plafondConso) {
        this.plafondConso = plafondConso;
    }

    public Integer getPlafondJournalier() {
        return plafondJournalier;
    }

    public void setPlafondJournalier(Integer plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
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

    public TiersPayantCategorie getTypeTiersPayant() {
        return typeTiersPayant;
    }

    public AssuredCustomerDTO setTypeTiersPayant(TiersPayantCategorie typeTiersPayant) {
        this.typeTiersPayant = typeTiersPayant;
        return this;
    }
}
