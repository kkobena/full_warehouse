// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'produit_vendu.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ProduitVendu _$ProduitVenduFromJson(Map<String, dynamic> json) => ProduitVendu(
      date: json['date'] as String,
      quantity: json['quantity'] as String,
      numero: json['numero'] as String?,
      unitPrice: json['unitPrice'] as String?,
      totalAmount: json['totalAmount'] as String?,
      grossAmount: json['grossAmount'] as String?,
      totaGrossAmount: json['totaGrossAmount'] as String?,
      userName: json['userName'] as String?,
    );

Map<String, dynamic> _$ProduitVenduToJson(ProduitVendu instance) =>
    <String, dynamic>{
      'date': instance.date,
      'quantity': instance.quantity,
      'numero': instance.numero,
      'unitPrice': instance.unitPrice,
      'totalAmount': instance.totalAmount,
      'grossAmount': instance.grossAmount,
      'totaGrossAmount': instance.totaGrossAmount,
      'userName': instance.userName,
    };
