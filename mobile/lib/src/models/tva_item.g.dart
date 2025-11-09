// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tva_item.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TvaItem _$TvaItemFromJson(Map<String, dynamic> json) => TvaItem(
      code: (json['code'] as num).toInt(),
      ttc: json['ttc'] as String,
      tva: json['tva'] as String,
      ht: json['ht'] as String,
      pourcentage: (json['pourcentage'] as num).toDouble(),
      date: json['date'] as String?,
    );

Map<String, dynamic> _$TvaItemToJson(TvaItem instance) => <String, dynamic>{
      'code': instance.code,
      'ttc': instance.ttc,
      'tva': instance.tva,
      'ht': instance.ht,
      'pourcentage': instance.pourcentage,
      'date': instance.date,
    };
