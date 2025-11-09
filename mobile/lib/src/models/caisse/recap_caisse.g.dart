// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'recap_caisse.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RecapCaisse _$RecapCaisseFromJson(Map<String, dynamic> json) => RecapCaisse(
      items: (json['items'] as List<dynamic>)
          .map((e) => UserCaisseRecap.fromJson(e as Map<String, dynamic>))
          .toList(),
      resume: (json['resume'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$RecapCaisseToJson(RecapCaisse instance) =>
    <String, dynamic>{
      'items': instance.items,
      'resume': instance.resume,
    };
