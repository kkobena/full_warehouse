// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

User _$UserFromJson(Map<String, dynamic> json) => User(
      login: json['login'] as String?,
      firstName: json['firstName'] as String?,
      lastName: json['lastName'] as String?,
      roleName: json['roleName'] as String?,
      abbrName: json['abbrName'] as String?,
      password: json['password'] as String?,
      id: (json['id'] as num?)?.toInt(),
    );

Map<String, dynamic> _$UserToJson(User instance) => <String, dynamic>{
      'login': instance.login,
      'firstName': instance.firstName,
      'lastName': instance.lastName,
      'roleName': instance.roleName,
      'abbrName': instance.abbrName,
      'password': instance.password,
      'id': instance.id,
    };
