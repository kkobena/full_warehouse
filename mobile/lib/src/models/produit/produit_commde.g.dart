// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'produit_commde.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ProduitCommde _$ProduitCommdeFromJson(Map<String, dynamic> json) =>
    ProduitCommde(
      date: json['date'] as String,
      quantity: json['quantity'] as String,
      fournisseur: json['fournisseur'] as String?,
      numero: json['numero'] as String?,
      unitPrice: json['unitPrice'] as String?,
      totalAmount: json['totalAmount'] as String?,
      grossAmount: json['grossAmount'] as String?,
      totaGrossAmount: json['totaGrossAmount'] as String?,
      userName: json['userName'] as String?,
    );

Map<String, dynamic> _$ProduitCommdeToJson(ProduitCommde instance) =>
    <String, dynamic>{
      'date': instance.date,
      'quantity': instance.quantity,
      'fournisseur': instance.fournisseur,
      'numero': instance.numero,
      'unitPrice': instance.unitPrice,
      'totalAmount': instance.totalAmount,
      'grossAmount': instance.grossAmount,
      'totaGrossAmount': instance.totaGrossAmount,
      'userName': instance.userName,
    };
