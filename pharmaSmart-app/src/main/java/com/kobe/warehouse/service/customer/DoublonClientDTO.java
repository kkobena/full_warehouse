package com.kobe.warehouse.service.customer;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une fiche d'un groupe de doublons, avec de quoi choisir celle à conserver. */
public record DoublonClientDTO(
    Integer id,
    String code,
    String firstName,
    String lastName,
    String phone,
    LocalDate datNaiss,
    String typeAssure,
    long nombreAchats,
    LocalDateTime derniereVisite
) {}
