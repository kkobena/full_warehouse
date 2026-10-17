package com.kobe.warehouse.service.dto;

public class EtiquetteDTO {

    private String code;
    private String libelle;

    private String prix;
    private boolean print = true;

    private String magasin;
    private String date;
    private int order;

    private String numLot;
    private String expiryDate;

    /** Trame GS1 de l'étiquette interne, préfixée {@code dm:}. {@code null} si rien à y porter. */
    private String codeDataMatrix;

    // Les champs qui suivent ne servent qu'aux usages du lot 3 — réglette, posologie, préparation
    // magistrale. Une même monnaie d'échange pour tous les gabarits, plutôt qu'un DTO par usage
    // que les gabarits et le rendu ZPL auraient à distinguer.

    /** Patient de l'ordonnance ou de la préparation. */
    private String patient;

    /** Prescripteur, nom et qualité tels qu'ils seront lus sur l'étiquette. */
    private String prescripteur;

    /** Instructions de prise, telles que l'ordonnance les porte. */
    private String posologie;

    /** Durée du traitement, déjà mise en forme (« 7 jours »). */
    private String duree;

    /** Composition d'une préparation magistrale. */
    private String composition;

    /** Mention d'usage : « à usage externe », « tenir hors de portée des enfants »… */
    private String mention;

    public EtiquetteDTO() {}

    public EtiquetteDTO(String code, String libelle, String prix, boolean print, String magasin, String date, int order) {
        this.code = code;
        this.libelle = libelle;
        this.prix = prix;
        this.print = print;
        this.magasin = magasin;
        this.date = date;
        this.order = order;
    }

    public String getCode() {
        return code;
    }

    public EtiquetteDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public EtiquetteDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getPrix() {
        return prix;
    }

    public EtiquetteDTO setPrix(String prix) {
        this.prix = prix;
        return this;
    }

    public boolean isPrint() {
        return print;
    }

    public EtiquetteDTO setPrint(boolean print) {
        this.print = print;
        return this;
    }

    public String getMagasin() {
        return magasin;
    }

    public EtiquetteDTO setMagasin(String magasin) {
        this.magasin = magasin;
        return this;
    }

    public String getDate() {
        return date;
    }

    public EtiquetteDTO setDate(String date) {
        this.date = date;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public EtiquetteDTO setOrder(int order) {
        this.order = order;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public EtiquetteDTO setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public EtiquetteDTO setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public String getCodeDataMatrix() {
        return codeDataMatrix;
    }

    public EtiquetteDTO setCodeDataMatrix(String codeDataMatrix) {
        this.codeDataMatrix = codeDataMatrix;
        return this;
    }

    public String getPatient() {
        return patient;
    }

    public EtiquetteDTO setPatient(String patient) {
        this.patient = patient;
        return this;
    }

    public String getPrescripteur() {
        return prescripteur;
    }

    public EtiquetteDTO setPrescripteur(String prescripteur) {
        this.prescripteur = prescripteur;
        return this;
    }

    public String getPosologie() {
        return posologie;
    }

    public EtiquetteDTO setPosologie(String posologie) {
        this.posologie = posologie;
        return this;
    }

    public String getDuree() {
        return duree;
    }

    public EtiquetteDTO setDuree(String duree) {
        this.duree = duree;
        return this;
    }

    public String getComposition() {
        return composition;
    }

    public EtiquetteDTO setComposition(String composition) {
        this.composition = composition;
        return this;
    }

    public String getMention() {
        return mention;
    }

    public EtiquetteDTO setMention(String mention) {
        this.mention = mention;
        return this;
    }
}
