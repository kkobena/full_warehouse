// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_caisse_recap.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UserCaisseRecap _$UserCaisseRecapFromJson(Map<String, dynamic> json) =>
    UserCaisseRecap(
      title: json['title'] as String,
      items: (json['items'] as List<dynamic>)
          .map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      resume: (json['resume'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$UserCaisseRecapToJson(UserCaisseRecap instance) =>
    <String, dynamic>{
      'title': instance.title,
      'items': instance.items,
      'resume': instance.resume,
    };
