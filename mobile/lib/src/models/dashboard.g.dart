// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dashboard.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Dashboard _$DashboardFromJson(Map<String, dynamic> json) => Dashboard(
      sales: (json['sales'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      netAmounts: (json['netAmounts'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      salesTypes: (json['salesTypes'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      counts: (json['counts'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      paymentModes: (json['paymentModes'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      commandes: (json['commandes'] as List<dynamic>?)
          ?.map((e) => ListItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$DashboardToJson(Dashboard instance) => <String, dynamic>{
      'sales': instance.sales,
      'netAmounts': instance.netAmounts,
      'salesTypes': instance.salesTypes,
      'counts': instance.counts,
      'paymentModes': instance.paymentModes,
      'commandes': instance.commandes,
    };
