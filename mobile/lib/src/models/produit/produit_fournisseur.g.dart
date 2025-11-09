// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'produit_fournisseur.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ProduitFournisseur _$ProduitFournisseurFromJson(Map<String, dynamic> json) =>
    ProduitFournisseur(
      codeCip: json['codeCip'] as String,
      prixUni: (json['prixUni'] as num).toInt(),
      prixAchat: (json['prixAchat'] as num).toInt(),
      fournisseurLibelle: json['fournisseurLibelle'] as String,
      principal: json['principal'] as bool,
    );

Map<String, dynamic> _$ProduitFournisseurToJson(ProduitFournisseur instance) =>
    <String, dynamic>{
      'codeCip': instance.codeCip,
      'prixUni': instance.prixUni,
      'prixAchat': instance.prixAchat,
      'fournisseurLibelle': instance.fournisseurLibelle,
      'principal': instance.principal,
    };
