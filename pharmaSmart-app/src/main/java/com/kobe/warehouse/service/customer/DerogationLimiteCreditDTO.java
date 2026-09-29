package com.kobe.warehouse.service.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Demande de vente à crédit au-delà de la limite, pour une vente donnée. Sans le droit
 * {@code pr-depasser-limite-credit}, l'utilisateur fournit la clé d'un collègue qui le détient.
 */
public record DerogationLimiteCreditDTO(
    @NotNull Long saleId,
    @NotNull LocalDate saleDate,
    @NotNull @Positive Integer montant,
    @NotBlank @Size(max = 255) String motif,
    String actionAuthorityKey
) {}
