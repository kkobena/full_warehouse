package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirClient}
 **/
@StaticMetamodel(AvoirClient.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirClient_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #reference
	 **/
	public static final String REFERENCE = "reference";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #clotureLe
	 **/
	public static final String CLOTURE_LE = "clotureLe";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #modeCloture
	 **/
	public static final String MODE_CLOTURE = "modeCloture";
	
	/**
	 * @see #quantite
	 **/
	public static final String QUANTITE = "quantite";
	
	/**
	 * @see #montant
	 **/
	public static final String MONTANT = "montant";
	
	/**
	 * @see #montantUtilise
	 **/
	public static final String MONTANT_UTILISE = "montantUtilise";
	
	/**
	 * @see #dateExpiration
	 **/
	public static final String DATE_EXPIRATION = "dateExpiration";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #customer
	 **/
	public static final String CUSTOMER = "customer";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #salesLine
	 **/
	public static final String SALES_LINE = "salesLine";
	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";
	
	/**
	 * @see #createdBy
	 **/
	public static final String CREATED_BY = "createdBy";
	
	/**
	 * @see #closedBy
	 **/
	public static final String CLOSED_BY = "closedBy";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirClient}
	 **/
	public static volatile EntityType<AvoirClient> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#id}
	 **/
	public static volatile SingularAttribute<AvoirClient, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#reference}
	 **/
	public static volatile SingularAttribute<AvoirClient, String> reference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#createdAt}
	 **/
	public static volatile SingularAttribute<AvoirClient, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#clotureLe}
	 **/
	public static volatile SingularAttribute<AvoirClient, LocalDateTime> clotureLe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#statut}
	 **/
	public static volatile SingularAttribute<AvoirClient, AvoirClientStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#modeCloture}
	 **/
	public static volatile SingularAttribute<AvoirClient, ModeClotureAvoir> modeCloture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#quantite}
	 **/
	public static volatile SingularAttribute<AvoirClient, Integer> quantite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#montant}
	 **/
	public static volatile SingularAttribute<AvoirClient, Integer> montant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#montantUtilise}
	 **/
	public static volatile SingularAttribute<AvoirClient, Integer> montantUtilise;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#dateExpiration}
	 **/
	public static volatile SingularAttribute<AvoirClient, LocalDate> dateExpiration;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#commentaire}
	 **/
	public static volatile SingularAttribute<AvoirClient, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#customer}
	 **/
	public static volatile SingularAttribute<AvoirClient, Customer> customer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#produit}
	 **/
	public static volatile SingularAttribute<AvoirClient, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#salesLine}
	 **/
	public static volatile SingularAttribute<AvoirClient, SalesLine> salesLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#commande}
	 **/
	public static volatile SingularAttribute<AvoirClient, Commande> commande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#createdBy}
	 **/
	public static volatile SingularAttribute<AvoirClient, AppUser> createdBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClient#closedBy}
	 **/
	public static volatile SingularAttribute<AvoirClient, AppUser> closedBy;

}

