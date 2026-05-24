package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.FournisseurProduit}
 **/
@StaticMetamodel(FournisseurProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class FournisseurProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #codeCip
	 **/
	public static final String CODE_CIP = "codeCip";
	
	/**
	 * @see #codeEan
	 **/
	public static final String CODE_EAN = "codeEan";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #prixUni
	 **/
	public static final String PRIX_UNI = "prixUni";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #orderLines
	 **/
	public static final String ORDER_LINES = "orderLines";
	
	/**
	 * @see #createdDate
	 **/
	public static final String CREATED_DATE = "createdDate";
	
	/**
	 * @see #lastModifiedDate
	 **/
	public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
	
	/**
	 * @see #qteColis
	 **/
	public static final String QTE_COLIS = "qteColis";
	
	/**
	 * @see #qteMinimaleCommande
	 **/
	public static final String QTE_MINIMALE_COMMANDE = "qteMinimaleCommande";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.FournisseurProduit}
	 **/
	public static volatile EntityType<FournisseurProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#id}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#codeCip}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, String> codeCip;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#codeEan}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, String> codeEan;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#prixAchat}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Integer> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#prixUni}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Integer> prixUni;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#produit}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#fournisseur}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#orderLines}
	 **/
	public static volatile SetAttribute<FournisseurProduit, OrderLine> orderLines;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#createdDate}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, LocalDateTime> createdDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#lastModifiedDate}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, LocalDateTime> lastModifiedDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#qteColis}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Integer> qteColis;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduit#qteMinimaleCommande}
	 **/
	public static volatile SingularAttribute<FournisseurProduit, Integer> qteMinimaleCommande;

}

