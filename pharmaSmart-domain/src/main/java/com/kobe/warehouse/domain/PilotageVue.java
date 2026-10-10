package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateursPilotageConverter;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;

/** Vue enregistrée de l'onglet Analyser ; sans propriétaire, elle est livrée d'office (migration V2.1.35). */
@Entity
@Table(name = "pilotage_vue")
public class PilotageVue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Convert(converter = IndicateursPilotageConverter.class)
    @Column(name = "indicateurs", nullable = false)
    private List<IndicateurPilotage> indicateurs;

    @Enumerated(EnumType.STRING)
    @Column(name = "axe", nullable = false, length = 30)
    private AxeAnalyse axe;

    @Enumerated(EnumType.STRING)
    @Column(name = "axe_croise", length = 30)
    private AxeAnalyse axeCroise;

    @Column(name = "nombre_elements", nullable = false)
    private int nombreElements;

    @Enumerated(EnumType.STRING)
    @Column(name = "tri", nullable = false, length = 20)
    private TriAnalyse tri;

    @Enumerated(EnumType.STRING)
    @Column(name = "affichage", nullable = false, length = 20)
    private AffichageAnalyse affichage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private AppUser proprietaire;

    @Column(name = "partagee", nullable = false)
    private boolean partagee;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public PilotageVue setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public List<IndicateurPilotage> getIndicateurs() {
        return indicateurs;
    }

    public PilotageVue setIndicateurs(List<IndicateurPilotage> indicateurs) {
        this.indicateurs = indicateurs;
        return this;
    }

    public AxeAnalyse getAxe() {
        return axe;
    }

    public PilotageVue setAxe(AxeAnalyse axe) {
        this.axe = axe;
        return this;
    }

    public AxeAnalyse getAxeCroise() {
        return axeCroise;
    }

    public PilotageVue setAxeCroise(AxeAnalyse axeCroise) {
        this.axeCroise = axeCroise;
        return this;
    }

    public int getNombreElements() {
        return nombreElements;
    }

    public PilotageVue setNombreElements(int nombreElements) {
        this.nombreElements = nombreElements;
        return this;
    }

    public TriAnalyse getTri() {
        return tri;
    }

    public PilotageVue setTri(TriAnalyse tri) {
        this.tri = tri;
        return this;
    }

    public AffichageAnalyse getAffichage() {
        return affichage;
    }

    public PilotageVue setAffichage(AffichageAnalyse affichage) {
        this.affichage = affichage;
        return this;
    }

    public AppUser getProprietaire() {
        return proprietaire;
    }

    public PilotageVue setProprietaire(AppUser proprietaire) {
        this.proprietaire = proprietaire;
        return this;
    }

    public boolean isPartagee() {
        return partagee;
    }

    public PilotageVue setPartagee(boolean partagee) {
        this.partagee = partagee;
        return this;
    }

    public int getOrdre() {
        return ordre;
    }

    public boolean estLivree() {
        return proprietaire == null;
    }
}
