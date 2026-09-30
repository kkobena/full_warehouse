package com.kobe.warehouse.service.customer;

import java.util.List;

/**
 * Fiches présumées du même patient.
 *
 * @param type      {@code ASSURE} ou {@code STANDARD} : on ne fusionne que des fiches de même nature
 * @param criteres  ce qui les rapproche : nom, téléphone, date de naissance
 */
public record DoublonClientGroupeDTO(String type, List<String> criteres, List<DoublonClientDTO> clients) {}
