package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.GroupeFournisseur}
 **/
@StaticMetamodel(GroupeFournisseur.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class GroupeFournisseur_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #addresspostale
	 **/
	public static final String ADDRESSPOSTALE = "addresspostale";
	
	/**
	 * @see #numFaxe
	 **/
	public static final String NUM_FAXE = "numFaxe";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #tel
	 **/
	public static final String TEL = "tel";
	
	/**
	 * @see #odre
	 **/
	public static final String ODRE = "odre";
	
	/**
	 * @see #delaiLivraisonJours
	 **/
	public static final String DELAI_LIVRAISON_JOURS = "delaiLivraisonJours";
	
	/**
	 * @see #frequenceCommandeJours
	 **/
	public static final String FREQUENCE_COMMANDE_JOURS = "frequenceCommandeJours";
	
	/**
	 * @see #codeRecepteurPharmaMl
	 **/
	public static final String CODE_RECEPTEUR_PHARMA_ML = "codeRecepteurPharmaMl";
	
	/**
	 * @see #codeOfficePharmaMl
	 **/
	public static final String CODE_OFFICE_PHARMA_ML = "codeOfficePharmaMl";
	
	/**
	 * @see #urlPharmaMl
	 **/
	public static final String URL_PHARMA_ML = "urlPharmaMl";
	
	/**
	 * @see #idRecepteurPharmaMl
	 **/
	public static final String ID_RECEPTEUR_PHARMA_ML = "idRecepteurPharmaMl";
	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.GroupeFournisseur}
	 **/
	public static volatile EntityType<GroupeFournisseur> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#id}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#libelle}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#addresspostale}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> addresspostale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#numFaxe}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> numFaxe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#email}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#tel}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> tel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#odre}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> odre;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#delaiLivraisonJours}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> delaiLivraisonJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#frequenceCommandeJours}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> frequenceCommandeJours;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#codeRecepteurPharmaMl}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> codeRecepteurPharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#codeOfficePharmaMl}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> codeOfficePharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#urlPharmaMl}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> urlPharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#idRecepteurPharmaMl}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, String> idRecepteurPharmaMl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#joursCredit}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> joursCredit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#joursCritique}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> joursCritique;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#palierRfa}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Long> palierRfa;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GroupeFournisseur#tauxRfa}
	 **/
	public static volatile SingularAttribute<GroupeFournisseur, Integer> tauxRfa;

}

