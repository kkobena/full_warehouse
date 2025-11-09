// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'rayon_produit.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RayonProduit _$RayonProduitFromJson(Map<String, dynamic> json) => RayonProduit(
      codeRayon: json['codeRayon'] as String,
      libelleRayon: json['libelleRayon'] as String,
      libelleStorage: json['libelleStorage'] as String,
      storageType: json['storageType'] as String,
    );

Map<String, dynamic> _$RayonProduitToJson(RayonProduit instance) =>
    <String, dynamic>{
      'codeRayon': instance.codeRayon,
      'libelleRayon': instance.libelleRayon,
      'libelleStorage': instance.libelleStorage,
      'storageType': instance.storageType,
    };
