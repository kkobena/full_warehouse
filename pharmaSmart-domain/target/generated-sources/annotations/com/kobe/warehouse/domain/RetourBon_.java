package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourBon}
 **/
@StaticMetamodel(RetourBon.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourBon_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #reference
	 **/
	public static final String REFERENCE = "reference";
	
	/**
	 * @see #retourBonItems
	 **/
	public static final String RETOUR_BON_ITEMS = "retourBonItems";
	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #horsCommande
	 **/
	public static final String HORS_COMMANDE = "horsCommande";
	
	/**
	 * @see #horsStock
	 **/
	public static final String HORS_STOCK = "horsStock";
	
	/**
	 * @see #pharmamlEnvoi
	 **/
	public static final String PHARMAML_ENVOI = "pharmamlEnvoi";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourBon}
	 **/
	public static volatile EntityType<RetourBon> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#id}
	 **/
	public static volatile SingularAttribute<RetourBon, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#dateMtv}
	 **/
	public static volatile SingularAttribute<RetourBon, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#user}
	 **/
	public static volatile SingularAttribute<RetourBon, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#statut}
	 **/
	public static volatile SingularAttribute<RetourBon, RetourStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#commentaire}
	 **/
	public static volatile SingularAttribute<RetourBon, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#reference}
	 **/
	public static volatile SingularAttribute<RetourBon, String> reference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#retourBonItems}
	 **/
	public static volatile ListAttribute<RetourBon, RetourBonItem> retourBonItems;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#commande}
	 **/
	public static volatile SingularAttribute<RetourBon, Commande> commande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#fournisseur}
	 **/
	public static volatile SingularAttribute<RetourBon, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#horsCommande}
	 **/
	public static volatile SingularAttribute<RetourBon, Boolean> horsCommande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#horsStock}
	 **/
	public static volatile SingularAttribute<RetourBon, Boolean> horsStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBon#pharmamlEnvoi}
	 **/
	public static volatile SingularAttribute<RetourBon, PharmaMlEnvoi> pharmamlEnvoi;

}

