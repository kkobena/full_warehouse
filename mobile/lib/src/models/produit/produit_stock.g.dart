// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'produit_stock.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ProduitStock _$ProduitStockFromJson(Map<String, dynamic> json) => ProduitStock(
      qtyStock: (json['qtyStock'] as num).toInt(),
      qtyVirtual: (json['qtyVirtual'] as num).toInt(),
      qtyUG: (json['qtyUG'] as num).toInt(),
      storageName: json['storageName'] as String,
      storageType: json['storageType'] as String,
    );

Map<String, dynamic> _$ProduitStockToJson(ProduitStock instance) =>
    <String, dynamic>{
      'qtyStock': instance.qtyStock,
      'qtyVirtual': instance.qtyVirtual,
      'qtyUG': instance.qtyUG,
      'storageName': instance.storageName,
      'storageType': instance.storageType,
    };
