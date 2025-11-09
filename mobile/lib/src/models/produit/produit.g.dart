// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'produit.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Produit _$ProduitFromJson(Map<String, dynamic> json) => Produit(
      id: (json['id'] as num).toInt(),
      libelle: json['libelle'] as String,
      etatProduit:
          EtatProduit.fromJson(json['etatProduit'] as Map<String, dynamic>),
      codeCip: json['codeCip'] as String?,
      rayonLibelle: json['rayonLibelle'] as String?,
      totalQuantity: (json['totalQuantity'] as num?)?.toInt(),
      qtyReserve: (json['qtyReserve'] as num?)?.toInt(),
      tvaTaux: (json['tvaTaux'] as num?)?.toInt(),
      regularUnitPrice: (json['regularUnitPrice'] as num?)?.toInt(),
      costAmount: (json['costAmount'] as num?)?.toInt(),
      qtyAppro: (json['qtyAppro'] as num?)?.toInt(),
      qtySeuilMini: (json['qtySeuilMini'] as num?)?.toInt(),
      fournisseurProduits: (json['fournisseurProduits'] as List<dynamic>?)
          ?.map((e) => ProduitFournisseur.fromJson(e as Map<String, dynamic>))
          .toList(),
      stockProduits: (json['stockProduits'] as List<dynamic>?)
          ?.map((e) => ProduitStock.fromJson(e as Map<String, dynamic>))
          .toList(),
      lastDateOfSale: json['lastDateOfSale'] == null
          ? null
          : DateTime.parse(json['lastDateOfSale'] as String),
      lastOrderDate: json['lastOrderDate'] == null
          ? null
          : DateTime.parse(json['lastOrderDate'] as String),
      lastInventoryDate: json['lastInventoryDate'] == null
          ? null
          : DateTime.parse(json['lastInventoryDate'] as String),
      laboratoireLibelle: json['laboratoireLibelle'] as String?,
      formeLibelle: json['formeLibelle'] as String?,
      gammeLibelle: json['gammeLibelle'] as String?,
      dciLibelle: json['dciLibelle'] as String?,
      perimeAt: json['perimeAt'] as String?,
      rayonProduits: (json['rayonProduits'] as List<dynamic>?)
          ?.map((e) => RayonProduit.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$ProduitToJson(Produit instance) => <String, dynamic>{
      'id': instance.id,
      'libelle': instance.libelle,
      'codeCip': instance.codeCip,
      'rayonLibelle': instance.rayonLibelle,
      'etatProduit': instance.etatProduit,
      'totalQuantity': instance.totalQuantity,
      'qtyReserve': instance.qtyReserve,
      'tvaTaux': instance.tvaTaux,
      'regularUnitPrice': instance.regularUnitPrice,
      'costAmount': instance.costAmount,
      'qtyAppro': instance.qtyAppro,
      'qtySeuilMini': instance.qtySeuilMini,
      'fournisseurProduits': instance.fournisseurProduits,
      'stockProduits': instance.stockProduits,
      'rayonProduits': instance.rayonProduits,
      'lastDateOfSale': instance.lastDateOfSale?.toIso8601String(),
      'lastOrderDate': instance.lastOrderDate?.toIso8601String(),
      'lastInventoryDate': instance.lastInventoryDate?.toIso8601String(),
      'laboratoireLibelle': instance.laboratoireLibelle,
      'formeLibelle': instance.formeLibelle,
      'gammeLibelle': instance.gammeLibelle,
      'dciLibelle': instance.dciLibelle,
      'perimeAt': instance.perimeAt,
    };
