package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_planification")
public class HistoriquePlanification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "planification_id", nullable = false)
    private Integer planificationId;

    @NotNull
    @Column(name = "execution_debut", nullable = false)
    private LocalDateTime executionDebut;

    @Column(name = "execution_fin")
    private LocalDateTime executionFin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ExecutionStatut statut;

    @Column(name = "generation_code")
    private Integer generationCode;

    @Column(name = "nombre_factures")
    private Integer nombreFactures;

    @Column(name = "message", length = 500)
    private String message;

    public Long getId() {
        return id;
    }

    public HistoriquePlanification setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getPlanificationId() {
        return planificationId;
    }

    public HistoriquePlanification setPlanificationId(Integer planificationId) {
        this.planificationId = planificationId;
        return this;
    }

    public LocalDateTime getExecutionDebut() {
        return executionDebut;
    }

    public HistoriquePlanification setExecutionDebut(LocalDateTime executionDebut) {
        this.executionDebut = executionDebut;
        return this;
    }

    public LocalDateTime getExecutionFin() {
        return executionFin;
    }

    public HistoriquePlanification setExecutionFin(LocalDateTime executionFin) {
        this.executionFin = executionFin;
        return this;
    }

    public ExecutionStatut getStatut() {
        return statut;
    }

    public HistoriquePlanification setStatut(ExecutionStatut statut) {
        this.statut = statut;
        return this;
    }

    public Integer getGenerationCode() {
        return generationCode;
    }

    public HistoriquePlanification setGenerationCode(Integer generationCode) {
        this.generationCode = generationCode;
        return this;
    }

    public Integer getNombreFactures() {
        return nombreFactures;
    }

    public HistoriquePlanification setNombreFactures(Integer nombreFactures) {
        this.nombreFactures = nombreFactures;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public HistoriquePlanification setMessage(String message) {
        this.message = message;
        return this;
    }
}
