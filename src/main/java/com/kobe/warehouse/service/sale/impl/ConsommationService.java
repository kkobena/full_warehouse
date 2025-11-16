package com.kobe.warehouse.service.sale.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.service.dto.Consommation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * Service for managing consumption (consommation) records.
 * This service consolidates consumption update logic previously duplicated in
 * ThirdPartySaleServiceImpl's updateClientTiersPayantAccount() and updateTiersPayantAccount() methods.
 */
@Service
@Transactional
public class ConsommationService {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * Updates consumption for an entity that tracks monthly consumption.
     *
     * @param entity the entity to update (ClientTiersPayant or TiersPayant)
     * @param montant the amount to add to consumption
     * @param dateTime the date and time of the consumption
     * @param saver consumer function to save the entity
     * @param <T> the type of entity that has consumption tracking
     */
    public <T extends HasConsommation> void updateConsommation(T entity, Integer montant, LocalDateTime dateTime, Consumer<T> saver) {
        if (montant == null) {
            return;
        }

        Set<Consommation> consommations = CollectionUtils.isEmpty(entity.getConsommations()) ? new HashSet<>() : entity.getConsommations();

        int consommationId = buildConsommationId(dateTime);

        consommations
            .stream()
            .filter(c -> c.getId() == consommationId)
            .findFirst()
            .ifPresentOrElse(
                conso -> conso.setConsommation(conso.getConsommation() + montant),
                () -> consommations.add(buildConsommation(montant, dateTime))
            );

        entity.setConsommations(consommations);

        // Handle both Integer and Long types for consoMensuelle
        Number currentConso = entity.getConsoMensuelle();
        long currentValue = (currentConso != null) ? currentConso.longValue() : 0L;
        long newValue = currentValue + montant;

        // Set based on the original type
        if (currentConso instanceof Integer || currentConso == null) {
            entity.setConsoMensuelle((int) newValue);
        } else {
            entity.setConsoMensuelle(newValue);
        }

        entity.setUpdated(LocalDateTime.now());

        saver.accept(entity);
    }

    /**
     * Builds a consumption ID from the current date.
     *
     * @return consumption ID in format yyyyMM
     */
    public int buildConsommationId() {
        return buildConsommationId(LocalDateTime.now());
    }

    /**
     * Builds a consumption ID from a specific date time.
     *
     * @param dateTime the date time to use
     * @return consumption ID in format yyyyMM
     */
    private int buildConsommationId(LocalDateTime dateTime) {
        return Integer.parseInt(dateTimeFormatter.format(dateTime));
    }

    /**
     * Creates a new Consommation object.
     *
     * @param montant the consumption amount
     * @param dateTime the date time for the consumption
     * @return a new Consommation instance
     */
    private Consommation buildConsommation(Integer montant, LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        Consommation consommation = new Consommation();
        consommation.setId(buildConsommationId(dateTime));
        consommation.setConsommation(montant);
        consommation.setMonth((short) date.getMonthValue());
        consommation.setYear(date.getYear());
        return consommation;
    }

    /**
     * Interface for entities that track consumption.
     * Note: getConsoMensuelle() returns Number to support both Integer and Long types.
     * JsonIgnoreProperties annotation prevents Jackson serialization issues with this interface.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public interface HasConsommation {
        Set<Consommation> getConsommations();
        void setConsommations(Set<Consommation> consommations);
        Number getConsoMensuelle();
        void setConsoMensuelle(Number consoMensuelle);
        void setUpdated(LocalDateTime updated);
    }
}
