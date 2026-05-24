package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Fournisseur}
 **/
@StaticMetamodel(Fournisseur.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Fournisseur_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #numFaxe
	 **/
	public static final String NUM_FAXE = "numFaxe";
	
	/**
	 * @see #addressePostal
	 **/
	public static final String ADDRESSE_POSTAL = "addressePostal";
	
	/**
	 * @see #phone
	 **/
	public static final String PHONE = "phone";
	
	/**
	 * @see #mobile
	 **/
	public static final String MOBILE = "mobile";
	
	/**
	 * @see #site
	 **/
	public static final String SITE = "site";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #odre
	 **/
	public static final String ODRE = "odre";
	
	/**
	 * @see #parent
	 **/
	public static final String PARENT = "parent";
	
	/**
	 * @see #agences
	 **/
	public static final String AGENCES = "agences";
	
	/**
	 * @see #delaiLivraisonJours
	 **/
	public static final String DELAI_LIVRAISON_JOURS = "delaiLivraisonJours";
	
	/**
	 * @see #frequenceCommandeJours
	 **/
	public static final String FREQUENCE_COMMANDE_JOURS = "frequenceCommandeJours";
	
	/**
	 * @see #joursCredit
	 **/
	public static final String JOURS_CREDIT = "joursCredit";
	
	/**
	 * @see #joursCritique
	 **/
	public static final String JOURS_CRITIQUE = "joursCritique";
	
	/**
	 * @see #palierRfa
	 **/
	public static final String PALIER_RFA = "palierRfa";
	
	/**
	 * @see #tauxRfa
	 **/
	public static final String TAUX_RFA = "tauxRfa";
	
	/**
	 * @see #identifiantRepartiteur
	 **/
	public static final String IDENTIFIANT_REPARTITEUR = "identifiantRepartiteur";
	
	/**
	 * @see #urlPharmaMl
	 **/
	public static final String URL_PHARMA_ML = "urlPharmaMl";
	
	/**
	 * @see #codeOfficePharmaMl
	 **/
	public static final String CODE_OFFICE_PHARMA_ML = "codeOfficePharmaMl";
	
	/**
	 * @see #codeRecepteurPharmaMl
	 **/
	public static final String CODE_RECEPTEUR_PHARMA_ML = "codeRecepteurPharmaMl";
	
	/**
	 * @see #idRecepteurPharmaMl
	 **/
	public static final String ID_RECEPTEUR_PHARMA_ML = "idRecepteurPharmaMl";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Fournisseur}
	 **/
	public static volatile EntityType<Fournisseur> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#id}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#libelle}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#numFaxe}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> numFaxe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#addressePostal}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> addressePostal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#phone}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> phone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#mobile}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> mobile;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#site}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> site;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#code}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#email}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#odre}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> odre;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#parent}
	 **/
	public static volatile SingularAttribute<Fournisseur, Fournisseur> parent;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#agences}
	 **/
	public static volatile ListAttribute<Fournisseur, Fournisseur> agences;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#delaiLivraisonJours}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> delaiLivraisonJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#frequenceCommandeJours}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> frequenceCommandeJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#joursCredit}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> joursCredit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#joursCritique}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> joursCritique;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#palierRfa}
	 **/
	public static volatile SingularAttribute<Fournisseur, Long> palierRfa;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#tauxRfa}
	 **/
	public static volatile SingularAttribute<Fournisseur, Integer> tauxRfa;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#identifiantRepartiteur}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> identifiantRepartiteur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#urlPharmaMl}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> urlPharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#codeOfficePharmaMl}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> codeOfficePharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#codeRecepteurPharmaMl}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> codeRecepteurPharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Fournisseur#idRecepteurPharmaMl}
	 **/
	public static volatile SingularAttribute<Fournisseur, String> idRecepteurPharmaMl;

}

