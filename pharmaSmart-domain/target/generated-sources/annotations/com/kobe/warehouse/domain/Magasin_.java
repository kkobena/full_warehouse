package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Magasin}
 **/
@StaticMetamodel(Magasin.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Magasin_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #fullName
	 **/
	public static final String FULL_NAME = "fullName";
	
	/**
	 * @see #phone
	 **/
	public static final String PHONE = "phone";
	
	/**
	 * @see #address
	 **/
	public static final String ADDRESS = "address";
	
	/**
	 * @see #note
	 **/
	public static final String NOTE = "note";
	
	/**
	 * @see #registre
	 **/
	public static final String REGISTRE = "registre";
	
	/**
	 * @see #compteContribuable
	 **/
	public static final String COMPTE_CONTRIBUABLE = "compteContribuable";
	
	/**
	 * @see #numComptable
	 **/
	public static final String NUM_COMPTABLE = "numComptable";
	
	/**
	 * @see #primaryStorage
	 **/
	public static final String PRIMARY_STORAGE = "primaryStorage";
	
	/**
	 * @see #stockageReserve
	 **/
	public static final String STOCKAGE_RESERVE = "stockageReserve";
	
	/**
	 * @see #welcomeMessage
	 **/
	public static final String WELCOME_MESSAGE = "welcomeMessage";
	
	/**
	 * @see #typeMagasin
	 **/
	public static final String TYPE_MAGASIN = "typeMagasin";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #compteBancaire
	 **/
	public static final String COMPTE_BANCAIRE = "compteBancaire";
	
	/**
	 * @see #registreImposition
	 **/
	public static final String REGISTRE_IMPOSITION = "registreImposition";
	
	/**
	 * @see #managerFirstName
	 **/
	public static final String MANAGER_FIRST_NAME = "managerFirstName";
	
	/**
	 * @see #managerLastName
	 **/
	public static final String MANAGER_LAST_NAME = "managerLastName";
	
	/**
	 * @see #fnePointOfSale
	 **/
	public static final String FNE_POINT_OF_SALE = "fnePointOfSale";
	
	/**
	 * @see #fneSecretKey
	 **/
	public static final String FNE_SECRET_KEY = "fneSecretKey";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Magasin}
	 **/
	public static volatile EntityType<Magasin> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#id}
	 **/
	public static volatile SingularAttribute<Magasin, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#name}
	 **/
	public static volatile SingularAttribute<Magasin, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#fullName}
	 **/
	public static volatile SingularAttribute<Magasin, String> fullName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#phone}
	 **/
	public static volatile SingularAttribute<Magasin, String> phone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#address}
	 **/
	public static volatile SingularAttribute<Magasin, String> address;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#note}
	 **/
	public static volatile SingularAttribute<Magasin, String> note;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#registre}
	 **/
	public static volatile SingularAttribute<Magasin, String> registre;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#compteContribuable}
	 **/
	public static volatile SingularAttribute<Magasin, String> compteContribuable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#numComptable}
	 **/
	public static volatile SingularAttribute<Magasin, String> numComptable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#primaryStorage}
	 **/
	public static volatile SingularAttribute<Magasin, Storage> primaryStorage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#stockageReserve}
	 **/
	public static volatile SingularAttribute<Magasin, Storage> stockageReserve;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#welcomeMessage}
	 **/
	public static volatile SingularAttribute<Magasin, String> welcomeMessage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#typeMagasin}
	 **/
	public static volatile SingularAttribute<Magasin, TypeMagasin> typeMagasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#email}
	 **/
	public static volatile SingularAttribute<Magasin, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#compteBancaire}
	 **/
	public static volatile SingularAttribute<Magasin, String> compteBancaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#registreImposition}
	 **/
	public static volatile SingularAttribute<Magasin, String> registreImposition;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#managerFirstName}
	 **/
	public static volatile SingularAttribute<Magasin, String> managerFirstName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#managerLastName}
	 **/
	public static volatile SingularAttribute<Magasin, String> managerLastName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#fnePointOfSale}
	 **/
	public static volatile SingularAttribute<Magasin, String> fnePointOfSale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Magasin#fneSecretKey}
	 **/
	public static volatile SingularAttribute<Magasin, String> fneSecretKey;

}

