import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/card_data.dart';
part 'balance.g.dart';

@JsonSerializable()
class Balance {
  final List<CardData> items;

  Balance({required this.items});

  // Factory constructor for deserialization
  factory Balance.fromJson(Map<String, dynamic> json) => _$BalanceFromJson(json);

  // Method for serialization
  Map<String, dynamic> toJson() => _$BalanceToJson(this);

}
