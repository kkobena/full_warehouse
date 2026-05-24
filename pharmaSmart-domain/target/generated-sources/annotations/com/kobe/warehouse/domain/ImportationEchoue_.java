package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ImportationEchoue}
 **/
@StaticMetamodel(ImportationEchoue.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ImportationEchoue_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #objectId
	 **/
	public static final String OBJECT_ID = "objectId";
	
	/**
	 * @see #importationEchoueLignes
	 **/
	public static final String IMPORTATION_ECHOUE_LIGNES = "importationEchoueLignes";
	
	/**
	 * @see #isCommande
	 **/
	public static final String IS_COMMANDE = "isCommande";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ImportationEchoue}
	 **/
	public static volatile EntityType<ImportationEchoue> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoue#id}
	 **/
	public static volatile SingularAttribute<ImportationEchoue, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoue#created}
	 **/
	public static volatile SingularAttribute<ImportationEchoue, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoue#objectId}
	 **/
	public static volatile SingularAttribute<ImportationEchoue, Integer> objectId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoue#importationEchoueLignes}
	 **/
	public static volatile ListAttribute<ImportationEchoue, ImportationEchoueLigne> importationEchoueLignes;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ImportationEchoue#isCommande}
	 **/
	public static volatile SingularAttribute<ImportationEchoue, Boolean> isCommande;

}

