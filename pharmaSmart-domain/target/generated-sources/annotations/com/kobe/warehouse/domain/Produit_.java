package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Produit}
 **/
@StaticMetamodel(Produit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Produit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #nomCommercial
	 **/
	public static final String NOM_COMMERCIAL = "nomCommercial";
	
	/**
	 * @see #typeProduit
	 **/
	public static final String TYPE_PRODUIT = "typeProduit";
	
	/**
	 * @see #costAmount
	 **/
	public static final String COST_AMOUNT = "costAmount";
	
	/**
	 * @see #regularUnitPrice
	 **/
	public static final String REGULAR_UNIT_PRICE = "regularUnitPrice";
	
	/**
	 * @see #netUnitPrice
	 **/
	public static final String NET_UNIT_PRICE = "netUnitPrice";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #itemQty
	 **/
	public static final String ITEM_QTY = "itemQty";
	
	/**
	 * @see #qtyAppro
	 **/
	public static final String QTY_APPRO = "qtyAppro";
	
	/**
	 * @see #qtySeuilMini
	 **/
	public static final String QTY_SEUIL_MINI = "qtySeuilMini";
	
	/**
	 * @see #checkExpiryDate
	 **/
	public static final String CHECK_EXPIRY_DATE = "checkExpiryDate";
	
	/**
	 * @see #gestionLot
	 **/
	public static final String GESTION_LOT = "gestionLot";
	
	/**
	 * @see #thermosensible
	 **/
	public static final String THERMOSENSIBLE = "thermosensible";
	
	/**
	 * @see #remisable
	 **/
	public static final String REMISABLE = "remisable";
	
	/**
	 * @see #chiffre
	 **/
	public static final String CHIFFRE = "chiffre";
	
	/**
	 * @see #itemCostAmount
	 **/
	public static final String ITEM_COST_AMOUNT = "itemCostAmount";
	
	/**
	 * @see #statutLegal
	 **/
	public static final String STATUT_LEGAL = "statutLegal";
	
	/**
	 * @see #itemRegularUnitPrice
	 **/
	public static final String ITEM_REGULAR_UNIT_PRICE = "itemRegularUnitPrice";
	
	/**
	 * @see #optionPrixProduit
	 **/
	public static final String OPTION_PRIX_PRODUIT = "optionPrixProduit";
	
	/**
	 * @see #prixMnp
	 **/
	public static final String PRIX_MNP = "prixMnp";
	
	/**
	 * @see #deconditionnable
	 **/
	public static final String DECONDITIONNABLE = "deconditionnable";
	
	/**
	 * @see #parent
	 **/
	public static final String PARENT = "parent";
	
	/**
	 * @see #produits
	 **/
	public static final String PRODUITS = "produits";
	
	/**
	 * @see #stockProduits
	 **/
	public static final String STOCK_PRODUITS = "stockProduits";
	
	/**
	 * @see #tva
	 **/
	public static final String TVA = "tva";
	
	/**
	 * @see #laboratoire
	 **/
	public static final String LABORATOIRE = "laboratoire";
	
	/**
	 * @see #forme
	 **/
	public static final String FORME = "forme";
	
	/**
	 * @see #codeEanLaboratoire
	 **/
	public static final String CODE_EAN_LABORATOIRE = "codeEanLaboratoire";
	
	/**
	 * @see #famille
	 **/
	public static final String FAMILLE = "famille";
	
	/**
	 * @see #gamme
	 **/
	public static final String GAMME = "gamme";
	
	/**
	 * @see #dci
	 **/
	public static final String DCI = "dci";
	
	/**
	 * @see #status
	 **/
	public static final String STATUS = "status";
	
	/**
	 * @see #classeCriticite
	 **/
	public static final String CLASSE_CRITICITE = "classeCriticite";
	
	/**
	 * @see #isClassificationOverridden
	 **/
	public static final String IS_CLASSIFICATION_OVERRIDDEN = "isClassificationOverridden";
	
	/**
	 * @see #estMedicamentEssentiel
	 **/
	public static final String EST_MEDICAMENT_ESSENTIEL = "estMedicamentEssentiel";
	
	/**
	 * @see #estProduitGarde
	 **/
	public static final String EST_PRODUIT_GARDE = "estProduitGarde";
	
	/**
	 * @see #fournisseurProduits
	 **/
	public static final String FOURNISSEUR_PRODUITS = "fournisseurProduits";
	
	/**
	 * @see #fournisseurProduitPrincipal
	 **/
	public static final String FOURNISSEUR_PRODUIT_PRINCIPAL = "fournisseurProduitPrincipal";
	
	/**
	 * @see #rayonProduits
	 **/
	public static final String RAYON_PRODUITS = "rayonProduits";
	
	/**
	 * @see #tableau
	 **/
	public static final String TABLEAU = "tableau";
	
	/**
	 * @see #seuilDeconditionnement
	 **/
	public static final String SEUIL_DECONDITIONNEMENT = "seuilDeconditionnement";
	
	/**
	 * @see #codeRemise
	 **/
	public static final String CODE_REMISE = "codeRemise";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Produit}
	 **/
	public static volatile EntityType<Produit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#id}
	 **/
	public static volatile SingularAttribute<Produit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#libelle}
	 **/
	public static volatile SingularAttribute<Produit, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#nomCommercial}
	 **/
	public static volatile SingularAttribute<Produit, String> nomCommercial;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#typeProduit}
	 **/
	public static volatile SingularAttribute<Produit, TypeProduit> typeProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#costAmount}
	 **/
	public static volatile SingularAttribute<Produit, Integer> costAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#regularUnitPrice}
	 **/
	public static volatile SingularAttribute<Produit, Integer> regularUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#netUnitPrice}
	 **/
	public static volatile SingularAttribute<Produit, Integer> netUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#createdAt}
	 **/
	public static volatile SingularAttribute<Produit, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#updatedAt}
	 **/
	public static volatile SingularAttribute<Produit, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#itemQty}
	 **/
	public static volatile SingularAttribute<Produit, Integer> itemQty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#qtyAppro}
	 **/
	public static volatile SingularAttribute<Produit, Integer> qtyAppro;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#qtySeuilMini}
	 **/
	public static volatile SingularAttribute<Produit, Integer> qtySeuilMini;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#checkExpiryDate}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> checkExpiryDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#gestionLot}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> gestionLot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#thermosensible}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> thermosensible;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#remisable}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> remisable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#chiffre}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> chiffre;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#itemCostAmount}
	 **/
	public static volatile SingularAttribute<Produit, Integer> itemCostAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#statutLegal}
	 **/
	public static volatile SingularAttribute<Produit, StatutLegal> statutLegal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#itemRegularUnitPrice}
	 **/
	public static volatile SingularAttribute<Produit, Integer> itemRegularUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#optionPrixProduit}
	 **/
	public static volatile ListAttribute<Produit, OptionPrixProduit> optionPrixProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#prixMnp}
	 **/
	public static volatile SingularAttribute<Produit, Integer> prixMnp;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#deconditionnable}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> deconditionnable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#parent}
	 **/
	public static volatile SingularAttribute<Produit, Produit> parent;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#produits}
	 **/
	public static volatile ListAttribute<Produit, Produit> produits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#stockProduits}
	 **/
	public static volatile SetAttribute<Produit, StockProduit> stockProduits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#tva}
	 **/
	public static volatile SingularAttribute<Produit, Tva> tva;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#laboratoire}
	 **/
	public static volatile SingularAttribute<Produit, Laboratoire> laboratoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#forme}
	 **/
	public static volatile SingularAttribute<Produit, FormProduit> forme;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#codeEanLaboratoire}
	 **/
	public static volatile SingularAttribute<Produit, String> codeEanLaboratoire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#famille}
	 **/
	public static volatile SingularAttribute<Produit, FamilleProduit> famille;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#gamme}
	 **/
	public static volatile SingularAttribute<Produit, GammeProduit> gamme;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#dci}
	 **/
	public static volatile SingularAttribute<Produit, Dci> dci;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#status}
	 **/
	public static volatile SingularAttribute<Produit, Status> status;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#classeCriticite}
	 **/
	public static volatile SingularAttribute<Produit, ClasseCriticite> classeCriticite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#isClassificationOverridden}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> isClassificationOverridden;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#estMedicamentEssentiel}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> estMedicamentEssentiel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#estProduitGarde}
	 **/
	public static volatile SingularAttribute<Produit, Boolean> estProduitGarde;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#fournisseurProduits}
	 **/
	public static volatile SetAttribute<Produit, FournisseurProduit> fournisseurProduits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#fournisseurProduitPrincipal}
	 **/
	public static volatile SingularAttribute<Produit, FournisseurProduit> fournisseurProduitPrincipal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#rayonProduits}
	 **/
	public static volatile SetAttribute<Produit, RayonProduit> rayonProduits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#tableau}
	 **/
	public static volatile SingularAttribute<Produit, Tableau> tableau;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#seuilDeconditionnement}
	 **/
	public static volatile SingularAttribute<Produit, Integer> seuilDeconditionnement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Produit#codeRemise}
	 **/
	public static volatile SingularAttribute<Produit, CodeRemise> codeRemise;

}

