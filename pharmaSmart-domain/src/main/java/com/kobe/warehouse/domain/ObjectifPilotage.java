package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Objectif mensuel d'un indicateur du pilotage (migration V2.1.38). */
@Entity
@Table(name = "pilotage_objectif")
public class ObjectifPilotage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicateur", nullable = false, length = 30)
    private IndicateurPilotage indicateur;

    @Column(name = "annee", nullable = false)
    private int annee;

    @Column(name = "mois", nullable = false)
    private int mois;

    @Column(name = "valeur", nullable = false)
    private double valeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par")
    private AppUser modifiePar;

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe;

    public Long getId() {
        return id;
    }

    public IndicateurPilotage getIndicateur() {
        return indicateur;
    }

    public ObjectifPilotage setIndicateur(IndicateurPilotage indicateur) {
        this.indicateur = indicateur;
        return this;
    }

    public int getAnnee() {
        return annee;
    }

    public ObjectifPilotage setAnnee(int annee) {
        this.annee = annee;
        return this;
    }

    public int getMois() {
        return mois;
    }

    public ObjectifPilotage setMois(int mois) {
        this.mois = mois;
        return this;
    }

    public double getValeur() {
        return valeur;
    }

    public ObjectifPilotage setValeur(double valeur) {
        this.valeur = valeur;
        return this;
    }

    public AppUser getModifiePar() {
        return modifiePar;
    }

    public ObjectifPilotage setModifiePar(AppUser modifiePar) {
        this.modifiePar = modifiePar;
        return this;
    }

    public LocalDateTime getModifieLe() {
        return modifieLe;
    }

    public ObjectifPilotage setModifieLe(LocalDateTime modifieLe) {
        this.modifieLe = modifieLe;
        return this;
    }
}
