import 'package:json_annotation/json_annotation.dart';
import 'package:mobile/src/models/tva_item.dart';

part 'tva.g.dart';

@JsonSerializable()
class Tva {
  final List<TvaItem> items;

  Tva({required this.items});

  // Factory constructor for deserialization
  factory Tva.fromJson(Map<String, dynamic> json) => _$TvaFromJson(json);

  // Method for serialization
  Map<String, dynamic> toJson() => _$TvaToJson(this);
}
