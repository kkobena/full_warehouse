// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tva.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Tva _$TvaFromJson(Map<String, dynamic> json) => Tva(
      items: (json['items'] as List<dynamic>)
          .map((e) => TvaItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$TvaToJson(Tva instance) => <String, dynamic>{
      'items': instance.items,
    };
