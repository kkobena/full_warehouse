package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public class ThirdPartySaleDTO extends SaleDTO {
  @Getter
  private Long ayantDroitId;
  @Getter
  private String ayantDroitFirstName;
  @Getter
  private String ayantDroitLastName;
  @Getter
  private String ayantDroitNum;
  private List<ClientTiersPayantDTO> tiersPayants = new ArrayList<>();
  @Getter
  private List<ThirdPartySaleLineDTO> thirdPartySaleLines = new ArrayList<>();
  @Getter
  private Integer partTiersPayant;
  @Getter
  private Integer partAssure;
  @Getter
  private String numBon;
  @Getter
  private boolean sansBon;

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
    }
    buildTiersPayantDTOFromSale(thirdPartySales.getThirdPartySaleLines());
  }

  public ThirdPartySaleDTO setSansBon(boolean sansBon) {
    this.sansBon = sansBon;
    return this;
  }

  public ThirdPartySaleDTO setPartAssure(Integer partAssure) {
    this.partAssure = partAssure;
    return this;
  }

  public ThirdPartySaleDTO setPartTiersPayant(Integer partTiersPayant) {
    this.partTiersPayant = partTiersPayant;
    return this;
  }

  public ThirdPartySaleDTO setNumBon(String numBon) {
    this.numBon = numBon;
    return this;
  }

  public ThirdPartySaleDTO setAyantDroitId(Long ayantDroitId) {
    this.ayantDroitId = ayantDroitId;
    return this;
  }

  public ThirdPartySaleDTO setAyantDroitFirstName(String ayantDroitFirstName) {
    this.ayantDroitFirstName = ayantDroitFirstName;
    return this;
  }

  public ThirdPartySaleDTO setAyantDroitLastName(String ayantDroitLastName) {
    this.ayantDroitLastName = ayantDroitLastName;
    return this;
  }

  public ThirdPartySaleDTO setAyantDroitNum(String ayantDroitNum) {
    this.ayantDroitNum = ayantDroitNum;
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

  public ThirdPartySaleDTO setThirdPartySaleLines(List<ThirdPartySaleLineDTO> thirdPartySaleLines) {
    this.thirdPartySaleLines = thirdPartySaleLines;
    return this;
  }

  private void buildTiersPayantDTOFromSale(List<ThirdPartySaleLine> thirdPartySaleLines) {
    List<ClientTiersPayantDTO> clientTiersPayantDTOS = new ArrayList<>();
    List<ThirdPartySaleLineDTO> thirdPartySaleLineDTOS = new ArrayList<>();
    thirdPartySaleLines.forEach(
        thirdPartySaleLine -> {
          if (thirdPartySaleLine.getClientTiersPayant().getPriorite() == PrioriteTiersPayant.T0) {
            this.numBon = thirdPartySaleLine.getNumBon();
          }

          thirdPartySaleLineDTOS.add(new ThirdPartySaleLineDTO(thirdPartySaleLine));
          clientTiersPayantDTOS.add(
              new ClientTiersPayantDTO(thirdPartySaleLine.getClientTiersPayant())
                  .setNumBon(thirdPartySaleLine.getNumBon()));
        });
    if (StringUtils.isEmpty(this.numBon) && !thirdPartySaleLineDTOS.isEmpty()) {
      this.numBon = thirdPartySaleLines.get(0).getNumBon();
    }
    this.setThirdPartySaleLines(thirdPartySaleLineDTOS);
    this.setTiersPayants(clientTiersPayantDTOS);
  }


}
