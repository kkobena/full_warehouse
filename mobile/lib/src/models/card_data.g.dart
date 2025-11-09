// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'card_data.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CardData _$CardDataFromJson(Map<String, dynamic> json) => CardData(
      title: json['title'] as String,
      sum: (json['sum'] as num?)?.toInt(),
      items: (json['items'] as List<dynamic>)
          .map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      summary: (json['summary'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$CardDataToJson(CardData instance) => <String, dynamic>{
      'title': instance.title,
      'sum': instance.sum,
      'items': instance.items,
      'summary': instance.summary,
    };
