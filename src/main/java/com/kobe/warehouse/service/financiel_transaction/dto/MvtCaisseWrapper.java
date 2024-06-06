package com.kobe.warehouse.service.financiel_transaction.dto;

import org.springframework.data.domain.Page;

public class MvtCaisseWrapper {
  private Page<MvtCaisseDTO> mvtCaisses;
  private MvtCaisseSum mvtCaisseSum;

  public MvtCaisseWrapper() {}

  public Page<MvtCaisseDTO> getMvtCaisses() {
    return mvtCaisses;
  }

  public MvtCaisseWrapper setMvtCaisses(Page<MvtCaisseDTO> mvtCaisses) {
    this.mvtCaisses = mvtCaisses;
    return this;
  }

  public MvtCaisseSum getMvtCaisseSum() {
    return mvtCaisseSum;
  }

  public MvtCaisseWrapper setMvtCaisseSum(MvtCaisseSum mvtCaisseSum) {
    this.mvtCaisseSum = mvtCaisseSum;
    return this;
  }
}
