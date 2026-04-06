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
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_certification_fne")
public class HistoriqueCertificationFne implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "planification_id")
    private Integer planificationId;

    @Column(name = "execution_debut")
    private LocalDateTime executionDebut;

    @Column(name = "execution_fin")
    private LocalDateTime executionFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    private ExecutionStatut statut;

    @Column(name = "nb_certifiees")
    private int nbCertifiees;

    @Column(name = "nb_echecs")
    private int nbEchecs;

    @Column(name = "message", length = 1000)
    private String message;

    public Long getId() { return id; }
    public HistoriqueCertificationFne setId(Long id) { this.id = id; return this; }

    public Integer getPlanificationId() { return planificationId; }
    public HistoriqueCertificationFne setPlanificationId(Integer planificationId) {
        this.planificationId = planificationId; return this;
    }

    public LocalDateTime getExecutionDebut() { return executionDebut; }
    public HistoriqueCertificationFne setExecutionDebut(LocalDateTime executionDebut) {
        this.executionDebut = executionDebut; return this;
    }

    public LocalDateTime getExecutionFin() { return executionFin; }
    public HistoriqueCertificationFne setExecutionFin(LocalDateTime executionFin) {
        this.executionFin = executionFin; return this;
    }

    public ExecutionStatut getStatut() { return statut; }
    public HistoriqueCertificationFne setStatut(ExecutionStatut statut) { this.statut = statut; return this; }

    public int getNbCertifiees() { return nbCertifiees; }
    public HistoriqueCertificationFne setNbCertifiees(int nbCertifiees) { this.nbCertifiees = nbCertifiees; return this; }

    public int getNbEchecs() { return nbEchecs; }
    public HistoriqueCertificationFne setNbEchecs(int nbEchecs) { this.nbEchecs = nbEchecs; return this; }

    public String getMessage() { return message; }
    public HistoriqueCertificationFne setMessage(String message) { this.message = message; return this; }
}
