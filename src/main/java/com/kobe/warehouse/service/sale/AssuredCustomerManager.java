package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;

import java.util.Optional;

/**
 * Service dédié à la gestion des clients assurés (AssuredCustomer).
 * Ce service encapsule toute la logique liée aux clients assurés dans les ventes tiers-payant.
 */
public interface AssuredCustomerManager {

    /**
     * Récupère un ayant-droit à partir de son identifiant.
     *
     * @param ayantDroitId l'identifiant de l'ayant-droit
     * @return Optional contenant l'ayant-droit si l'ID n'est pas null
     */
    Optional<AssuredCustomer> getAyantDroitFromId(Integer ayantDroitId);

    /**
     * Met à jour les informations d'un client assuré.
     *
     * @param assuredCustomer le client assuré à mettre à jour
     * @param customer les nouvelles données du client
     * @throws InvalidPhoneNumberException si le numéro de téléphone est invalide
     */
    void updateAssuredCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer) throws InvalidPhoneNumberException;

    /**
     * Vérifie si deux clients assurés sont identiques.
     *
     * @param assuredCustomer le client assuré du domaine
     * @param customer le DTO du client
     * @return true si les clients ont le même identifiant
     */
    boolean isSameCustomer(AssuredCustomer assuredCustomer, AssuredCustomerDTO customer);
}
