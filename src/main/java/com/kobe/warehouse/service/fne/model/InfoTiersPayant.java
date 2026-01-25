package com.kobe.warehouse.service.fne.model;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;

public record InfoTiersPayant(Integer id, String name, String fullName, String telephone,
                              String email, String adresse, String ncc, TiersPayantCategorie categorie) {


}
