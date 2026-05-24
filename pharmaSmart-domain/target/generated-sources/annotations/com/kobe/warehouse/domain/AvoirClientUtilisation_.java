package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirClientUtilisation}
 **/
@StaticMetamodel(AvoirClientUtilisation.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirClientUtilisation_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #avoirClient
	 **/
	public static final String AVOIR_CLIENT = "avoirClient";
	
	/**
	 * @see #montantUtilise
	 **/
	public static final String MONTANT_UTILISE = "montantUtilise";
	
	/**
	 * @see #utiliseLe
	 **/
	public static final String UTILISE_LE = "utiliseLe";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #utilisePar
	 **/
	public static final String UTILISE_PAR = "utilisePar";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirClientUtilisation}
	 **/
	public static volatile EntityType<AvoirClientUtilisation> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#id}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#avoirClient}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, AvoirClient> avoirClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#montantUtilise}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, Integer> montantUtilise;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#utiliseLe}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, LocalDateTime> utiliseLe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#commentaire}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirClientUtilisation#utilisePar}
	 **/
	public static volatile SingularAttribute<AvoirClientUtilisation, AppUser> utilisePar;

}

