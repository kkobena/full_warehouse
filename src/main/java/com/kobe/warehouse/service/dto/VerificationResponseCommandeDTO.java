package com.kobe.warehouse.service.dto;

import java.util.List;
import java.util.Objects;


public class VerificationResponseCommandeDTO {
private List<Item> items;
private List<Item> extraItems;

    public List<Item> getItems() {
        return items;
    }

    public VerificationResponseCommandeDTO setItems(List<Item> items) {
        this.items = items;
        return this;
    }

    public List<Item> getExtraItems() {
        return extraItems;
    }

    public VerificationResponseCommandeDTO setExtraItems(List<Item> extraItems) {
        this.extraItems = extraItems;
        return this;
    }

    public static class Item {
    private String codeCip;
    private String codeEan;
    private String produitLibelle;
    private int quantitePriseEnCompte;
    private int quantite;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return codeCip.equals(item.codeCip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(codeCip);
        }

        public String getCodeCip() {
            return codeCip;
        }

        public Item setCodeCip(String codeCip) {
            this.codeCip = codeCip;
            return this;
        }

        public String getCodeEan() {
            return codeEan;
        }

        public Item setCodeEan(String codeEan) {
            this.codeEan = codeEan;
            return this;
        }

        public String getProduitLibelle() {
            return produitLibelle;
        }

        public Item setProduitLibelle(String produitLibelle) {
            this.produitLibelle = produitLibelle;
            return this;
        }

        public int getQuantitePriseEnCompte() {
            return quantitePriseEnCompte;
        }

        public Item setQuantitePriseEnCompte(int quantitePriseEnCompte) {
            this.quantitePriseEnCompte = quantitePriseEnCompte;
            return this;
        }

        public int getQuantite() {
            return quantite;
        }

        public Item setQuantite(int quantite) {
            this.quantite = quantite;
            return this;
        }
    }
}
