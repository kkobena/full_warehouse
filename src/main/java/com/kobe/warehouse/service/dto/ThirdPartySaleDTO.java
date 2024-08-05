package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ThirdPartySaleDTO extends SaleDTO {

    private Long ayantDroitId;
    private String ayantDroitFirstName;
    private String ayantDroitLastName;
    private String ayantDroitNum;
    private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
    private List<ThirdPartySaleLineDTO> thirdPartySaleLines = new ArrayList<>();
    private Integer partTiersPayant;
    private Integer partAssure;
    private String numBon;
    private boolean sansBon;
    private AssuredCustomerDTO ayantDroit;

    public ThirdPartySaleDTO() {
        super();
    }

    public ThirdPartySaleDTO(ThirdPartySales thirdPartySales) {
        super(thirdPartySales);
        super.setCategorie("VO");
        this.partAssure = thirdPartySales.getPartAssure();
        this.partTiersPayant = thirdPartySales.getPartTiersPayant();
        AssuredCustomer assuredCustomer = thirdPartySales.getAyantDroit();
        if (assuredCustomer != null) {
            this.ayantDroitId = assuredCustomer.getId();
            this.ayantDroitFirstName = assuredCustomer.getFirstName();
            this.ayantDroitLastName = assuredCustomer.getLastName();
            this.ayantDroitNum = assuredCustomer.getNumAyantDroit();
            this.ayantDroit = new AssuredCustomerDTO(assuredCustomer);
        }
        buildTiersPayantDTOFromSale(thirdPartySales.getThirdPartySaleLines());
    }

    public AssuredCustomerDTO getAyantDroit() {
        return ayantDroit;
    }

    public ThirdPartySaleDTO setAyantDroit(AssuredCustomerDTO ayantDroit) {
        this.ayantDroit = ayantDroit;
        return this;
    }

    public List<ClientTiersPayantDTO> getTiersPayants() {
        if (tiersPayants != null) {
            tiersPayants.sort(Comparator.comparing(ClientTiersPayantDTO::getCategorie));
        }
        return tiersPayants;
    }

    public ThirdPartySaleDTO setTiersPayants(List<ClientTiersPayantDTO> tiersPayants) {
        this.tiersPayants = tiersPayants;
        return this;
    }

    private void buildTiersPayantDTOFromSale(List<ThirdPartySaleLine> thirdPartySaleLines) {
        List<ClientTiersPayantDTO> clientTiersPayantDTOS = new ArrayList<>();
        List<ThirdPartySaleLineDTO> thirdPartySaleLineDTOS = new ArrayList<>();
        thirdPartySaleLines.forEach(thirdPartySaleLine -> {
            if (thirdPartySaleLine.getClientTiersPayant().getPriorite() == PrioriteTiersPayant.T0) {
                this.numBon = thirdPartySaleLine.getNumBon();
            }

            thirdPartySaleLineDTOS.add(new ThirdPartySaleLineDTO(thirdPartySaleLine));
            clientTiersPayantDTOS.add(
                new ClientTiersPayantDTO(thirdPartySaleLine.getClientTiersPayant()).setNumBon(thirdPartySaleLine.getNumBon())
            );
        });
        if (StringUtils.isEmpty(this.numBon) && !thirdPartySaleLineDTOS.isEmpty()) {
            this.numBon = thirdPartySaleLines.getFirst().getNumBon();
        }
        this.setThirdPartySaleLines(thirdPartySaleLineDTOS);
        this.setTiersPayants(clientTiersPayantDTOS);
    }

    public Long getAyantDroitId() {
        return ayantDroitId;
    }

    public ThirdPartySaleDTO setAyantDroitId(Long ayantDroitId) {
        this.ayantDroitId = ayantDroitId;
        return this;
    }

    public String getAyantDroitFirstName() {
        return ayantDroitFirstName;
    }

    public ThirdPartySaleDTO setAyantDroitFirstName(String ayantDroitFirstName) {
        this.ayantDroitFirstName = ayantDroitFirstName;
        return this;
    }

    public String getAyantDroitLastName() {
        return ayantDroitLastName;
    }

    public ThirdPartySaleDTO setAyantDroitLastName(String ayantDroitLastName) {
        this.ayantDroitLastName = ayantDroitLastName;
        return this;
    }

    public String getAyantDroitNum() {
        return ayantDroitNum;
    }

    public ThirdPartySaleDTO setAyantDroitNum(String ayantDroitNum) {
        this.ayantDroitNum = ayantDroitNum;
        return this;
    }

    public List<ThirdPartySaleLineDTO> getThirdPartySaleLines() {
        return thirdPartySaleLines;
    }

    public ThirdPartySaleDTO setThirdPartySaleLines(List<ThirdPartySaleLineDTO> thirdPartySaleLines) {
        this.thirdPartySaleLines = thirdPartySaleLines;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public ThirdPartySaleDTO setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public ThirdPartySaleDTO setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public String getNumBon() {
        return numBon;
    }

    public ThirdPartySaleDTO setNumBon(String numBon) {
        this.numBon = numBon;
        return this;
    }

    public boolean isSansBon() {
        return sansBon;
    }

    public ThirdPartySaleDTO setSansBon(boolean sansBon) {
        this.sansBon = sansBon;
        return this;
    }
}
