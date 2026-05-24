package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirFournisseurLine}
 **/
@StaticMetamodel(AvoirFournisseurLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirFournisseurLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #avoirFournisseur
	 **/
	public static final String AVOIR_FOURNISSEUR = "avoirFournisseur";
	
	/**
	 * @see #retourBonItem
	 **/
	public static final String RETOUR_BON_ITEM = "retourBonItem";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirFournisseurLine}
	 **/
	public static volatile EntityType<AvoirFournisseurLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#id}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#avoirFournisseur}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, AvoirFournisseur> avoirFournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#retourBonItem}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, RetourBonItem> retourBonItem;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#qtyMvt}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#prixAchat}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, Long> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirFournisseurLine#commentaire}
	 **/
	public static volatile SingularAttribute<AvoirFournisseurLine, String> commentaire;

}

