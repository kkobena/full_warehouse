// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'list_item.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ListItem _$ListItemFromJson(Map<String, dynamic> json) => ListItem(
      libelle: json['libelle'] as String,
      value: json['value'] as String,
      autre: json['autre'] as String?,
    );

Map<String, dynamic> _$ListItemToJson(ListItem instance) => <String, dynamic>{
      'libelle': instance.libelle,
      'value': instance.value,
      'autre': instance.autre,
    };
