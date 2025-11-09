import 'package:json_annotation/json_annotation.dart';

part 'tva_item.g.dart';

@JsonSerializable()
class TvaItem {
  final int code;
  final String ttc;
  final String tva;
  final String ht;
  final double pourcentage;
  final String? date; // Optional field for date

  TvaItem({
    required this.code,
    required this.ttc,
    required this.tva,
    required this.ht,
    required this.pourcentage,
    this.date,
  });

  factory TvaItem.fromJson(Map<String, dynamic> json) =>
      _$TvaItemFromJson(json);


  Map<String, dynamic> toJson() => _$TvaItemToJson(this);
}
