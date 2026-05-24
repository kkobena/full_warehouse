package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PharmaMlEnvoi}
 **/
@StaticMetamodel(PharmaMlEnvoi.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PharmaMlEnvoi_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #refMessage
	 **/
	public static final String REF_MESSAGE = "refMessage";
	
	/**
	 * @see #tentatives
	 **/
	public static final String TENTATIVES = "tentatives";
	
	/**
	 * @see #derniereTentative
	 **/
	public static final String DERNIERE_TENTATIVE = "derniereTentative";
	
	/**
	 * @see #xmlRequetePath
	 **/
	public static final String XML_REQUETE_PATH = "xmlRequetePath";
	
	/**
	 * @see #xmlReponsePath
	 **/
	public static final String XML_REPONSE_PATH = "xmlReponsePath";
	
	/**
	 * @see #totalLignes
	 **/
	public static final String TOTAL_LIGNES = "totalLignes";
	
	/**
	 * @see #lignesAcceptees
	 **/
	public static final String LIGNES_ACCEPTEES = "lignesAcceptees";
	
	/**
	 * @see #lignesRupture
	 **/
	public static final String LIGNES_RUPTURE = "lignesRupture";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PharmaMlEnvoi}
	 **/
	public static volatile EntityType<PharmaMlEnvoi> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#id}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#commande}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Commande> commande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#fournisseur}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#statut}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, PharmaMlStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#refMessage}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, String> refMessage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#tentatives}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Integer> tentatives;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#derniereTentative}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, LocalDateTime> derniereTentative;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#xmlRequetePath}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, String> xmlRequetePath;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#xmlReponsePath}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, String> xmlReponsePath;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#totalLignes}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Integer> totalLignes;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#lignesAcceptees}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Integer> lignesAcceptees;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#lignesRupture}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, Integer> lignesRupture;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PharmaMlEnvoi#createdAt}
	 **/
	public static volatile SingularAttribute<PharmaMlEnvoi, LocalDateTime> createdAt;

}

