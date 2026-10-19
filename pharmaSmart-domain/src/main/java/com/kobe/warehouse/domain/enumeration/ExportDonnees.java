package com.kobe.warehouse.domain.enumeration;

import static com.kobe.warehouse.domain.enumeration.TypeColonneExport.*;

import java.util.List;

/**
 * Exports de données brutes : des lignes lues en flux, que le pharmacien retravaille dans un tableur. Les colonnes suivent, dans l'ordre, ce que lit la requête de l'export
 * ({@code RequeteExport}) ; leur type décide du format des cellules. Les filtres annoncés sont ceux des écrans d'où les données
 * viennent : le pharmacien exporte d'ici ce qu'il aurait filtré là-bas.
 */
public enum ExportDonnees implements SujetExport {
    VENTES(
        RubriqueExport.DONNEES,
        "Ventes",
        "Une ligne par vente clôturée du CA de l'officine (annulées exclues).",
        true,
        List.of(
            new ColonneExport("Date", DATE),
            new ColonneExport("Horodatage", DATE_HEURE),
            new ColonneExport("Numéro", TEXTE),
            new ColonneExport("Type de vente", TEXTE),
            new ColonneExport("Ordonnance / conseil", TEXTE),
            new ColonneExport("Vendeur", TEXTE),
            new ColonneExport("Caissier", TEXTE),
            new ColonneExport("Montant TTC", MONTANT),
            new ColonneExport("Montant HT", MONTANT),
            new ColonneExport("Remise", MONTANT),
            new ColonneExport("Net à payer", MONTANT)
        ),
        List.of(
            FiltreExport.NATURE_VENTE,
            FiltreExport.TYPE_PRESCRIPTION,
            FiltreExport.VENDEUR,
            FiltreExport.CAISSIER,
            FiltreExport.MAGASIN
        )
    ),
    LIGNES_VENTE(
        RubriqueExport.DONNEES,
        "Lignes de vente",
        "Une ligne par produit vendu : quantités, prix, remise, coût d'achat.",
        true,
        List.of(
            new ColonneExport("Date", DATE),
            new ColonneExport("Vente", TEXTE),
            new ColonneExport("CIP", TEXTE),
            new ColonneExport("Produit", TEXTE),
            new ColonneExport("Famille", TEXTE),
            new ColonneExport("Quantité demandée", ENTIER),
            new ColonneExport("Quantité servie", ENTIER),
            new ColonneExport("Prix unitaire", MONTANT),
            new ColonneExport("Montant TTC", MONTANT),
            new ColonneExport("Remise", MONTANT),
            new ColonneExport("Prix d'achat unitaire", MONTANT)
        ),
        List.of(
            FiltreExport.FAMILLE,
            FiltreExport.LABORATOIRE,
            FiltreExport.FOURNISSEUR,
            FiltreExport.NATURE_VENTE,
            FiltreExport.TYPE_PRESCRIPTION,
            FiltreExport.VENDEUR,
            FiltreExport.MAGASIN
        )
    ),
    ENCAISSEMENTS(
        RubriqueExport.DONNEES,
        "Encaissements et mouvements de caisse",
        "Une ligne par règlement ou mouvement : type, mode de paiement, montant, caissier.",
        true,
        List.of(
            new ColonneExport("Date", DATE),
            new ColonneExport("Horodatage", DATE_HEURE),
            new ColonneExport("Nature", TEXTE),
            new ColonneExport("Type de transaction", TEXTE),
            new ColonneExport("Mode de paiement", TEXTE),
            new ColonneExport("Montant", MONTANT),
            new ColonneExport("Caissier", TEXTE),
            new ColonneExport("Référence", TEXTE)
        ),
        List.of(FiltreExport.MODE_PAIEMENT, FiltreExport.TYPE_TRANSACTION, FiltreExport.CAISSIER)
    ),
    ACHATS(
        RubriqueExport.DONNEES,
        "Achats reçus",
        "Une ligne par produit reçu, datée à la réception : quantités, unités gratuites, prix d'achat.",
        true,
        List.of(
            new ColonneExport("Date de réception", DATE),
            new ColonneExport("Bon de livraison", TEXTE),
            new ColonneExport("Fournisseur", TEXTE),
            new ColonneExport("CIP", TEXTE),
            new ColonneExport("Produit", TEXTE),
            new ColonneExport("Quantité reçue", ENTIER),
            new ColonneExport("Unités gratuites", ENTIER),
            new ColonneExport("Prix d'achat unitaire", MONTANT),
            new ColonneExport("Montant", MONTANT)
        ),
        List.of(FiltreExport.FOURNISSEUR, FiltreExport.FAMILLE, FiltreExport.LABORATOIRE)
    ),
    MOUVEMENTS_STOCK(
        RubriqueExport.DONNEES,
        "Mouvements de stock",
        "Tous les mouvements tracés (ventes, entrées, ajustements, inventaires…), avec le stock avant et après.",
        true,
        List.of(
            new ColonneExport("Date", DATE),
            new ColonneExport("Horodatage", DATE_HEURE),
            new ColonneExport("Mouvement", TEXTE),
            new ColonneExport("Produit", TEXTE),
            new ColonneExport("Quantité", ENTIER),
            new ColonneExport("Stock avant", ENTIER),
            new ColonneExport("Stock après", ENTIER),
            new ColonneExport("Prix d'achat", MONTANT),
            new ColonneExport("Prix de vente", MONTANT),
            new ColonneExport("Utilisateur", TEXTE)
        ),
        List.of(
            FiltreExport.TYPE_MOUVEMENT,
            FiltreExport.UTILISATEUR,
            FiltreExport.FAMILLE,
            FiltreExport.LABORATOIRE,
            FiltreExport.MAGASIN
        )
    ),
    PRODUITS(
        RubriqueExport.DONNEES,
        "Produits et prix",
        "Le référentiel tel qu'il est aujourd'hui : classement, TVA, prix, stock.",
        false,
        List.of(
            new ColonneExport("CIP", TEXTE),
            new ColonneExport("Produit", TEXTE),
            new ColonneExport("Famille", TEXTE),
            new ColonneExport("Laboratoire", TEXTE),
            new ColonneExport("TVA (%)", ENTIER),
            new ColonneExport("Prix de vente", MONTANT),
            new ColonneExport("Prix d'achat", MONTANT),
            new ColonneExport("Stock", ENTIER)
        ),
        List.of(FiltreExport.FAMILLE, FiltreExport.LABORATOIRE, FiltreExport.FOURNISSEUR, FiltreExport.FORME, FiltreExport.GAMME)
    ),
    CLIENTS(
        RubriqueExport.NOMINATIF,
        "Clients",
        "Coordonnées des clients : données personnelles, chaque export est tracé.",
        false,
        List.of(
            new ColonneExport("Code", TEXTE),
            new ColonneExport("Nom", TEXTE),
            new ColonneExport("Prénom", TEXTE),
            new ColonneExport("Téléphone", TEXTE),
            new ColonneExport("E-mail", TEXTE),
            new ColonneExport("Date de naissance", DATE),
            new ColonneExport("Sexe", TEXTE),
            new ColonneExport("Créé le", DATE_HEURE)
        ),
        List.of(FiltreExport.SEXE, FiltreExport.STATUT_CLIENT)
    ),
    VENTES_JOUR_PRODUIT(
        RubriqueExport.BI,
        "Ventes jour × produit",
        "Fichier plat pour un tableau croisé (Excel, Power BI) : toutes les ventilations du pilotage, une ligne par jour et produit.",
        true,
        List.of(
            new ColonneExport("Jour", DATE),
            new ColonneExport("CIP", TEXTE),
            new ColonneExport("Produit", TEXTE),
            new ColonneExport("Famille", TEXTE),
            new ColonneExport("Laboratoire", TEXTE),
            new ColonneExport("Fournisseur principal", TEXTE),
            new ColonneExport("Forme", TEXTE),
            new ColonneExport("Gamme", TEXTE),
            new ColonneExport("TVA (%)", ENTIER),
            new ColonneExport("Type de vente", TEXTE),
            new ColonneExport("Ordonnance / conseil", TEXTE),
            new ColonneExport("Vendeur", TEXTE),
            new ColonneExport("Magasin", TEXTE),
            new ColonneExport("Taux de remise (%)", ENTIER),
            new ColonneExport("Octroi de la remise", TEXTE),
            new ColonneExport("Quantité servie", ENTIER),
            new ColonneExport("CA TTC", MONTANT),
            new ColonneExport("CA HT", MONTANT),
            new ColonneExport("Remise", MONTANT),
            new ColonneExport("Coût HT", MONTANT),
            new ColonneExport("Marge HT", MONTANT)
        ),
        List.of(
            FiltreExport.FAMILLE,
            FiltreExport.LABORATOIRE,
            FiltreExport.FOURNISSEUR,
            FiltreExport.FORME,
            FiltreExport.GAMME,
            FiltreExport.NATURE_VENTE,
            FiltreExport.TYPE_PRESCRIPTION,
            FiltreExport.VENDEUR,
            FiltreExport.MAGASIN
        )
    );

    /** Une ligne de données s'écrit dans n'importe lequel des trois formats. */
    private static final List<FormatExport> TOUS_FORMATS = List.of(FormatExport.XLSX, FormatExport.CSV, FormatExport.PDF);

    private final RubriqueExport rubrique;
    private final String libelle;
    private final String description;
    private final boolean periodique;
    private final List<ColonneExport> colonnes;
    private final List<FiltreExport> filtres;

    ExportDonnees(
        RubriqueExport rubrique,
        String libelle,
        String description,
        boolean periodique,
        List<ColonneExport> colonnes,
        List<FiltreExport> filtres
    ) {
        this.rubrique = rubrique;
        this.libelle = libelle;
        this.description = description;
        this.periodique = periodique;
        this.colonnes = colonnes;
        this.filtres = filtres;
    }

    public RubriqueExport getRubrique() {
        return rubrique;
    }

    @Override
    public String getLibelleRubrique() {
        return rubrique.getLibelle();
    }

    /** Le droit de la rubrique : c'est lui qui ouvre tous les exports de données qu'elle contient. */
    @Override
    public List<String> getDroits() {
        return List.of(rubrique.getDroit());
    }

    @Override
    public List<FormatExport> getFormats() {
        return TOUS_FORMATS;
    }

    @Override
    public boolean estProgrammable() {
        return true;
    }

    @Override
    public String getLibelle() {
        return libelle;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /** Lu sur une période (du / au) ; sinon, l'état du jour. */
    @Override
    public boolean isPeriodique() {
        return periodique;
    }

    public List<ColonneExport> getColonnes() {
        return colonnes;
    }

    /** Filtres que l'export accepte ; tout autre filtre reçu est ignoré. */
    @Override
    public List<FiltreExport> getFiltres() {
        return filtres;
    }

    public record ColonneExport(String libelle, TypeColonneExport type) {}
}
