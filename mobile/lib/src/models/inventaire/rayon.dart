import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

part 'rayon.g.dart';

@HiveType(typeId: 0)
@JsonSerializable()
class Rayon extends HiveObject {
  @HiveField(0)
  final int id;
  @HiveField(1)
  final String code;
  @HiveField(2)
  final String libelle;
  @HiveField(3)
  final int? storageId;
  @HiveField(4)
  final String? storageLibelle;
  @HiveField(5)
  final int inventoryId;

  Rayon({
    required this.id,
    required this.code,
    required this.libelle,
    required this.inventoryId,
    this.storageId,
    this.storageLibelle,
  });

  factory Rayon.fromJson(Map<String, dynamic> json) => _$RayonFromJson(json);

  Map<String, dynamic> toJson() => _$RayonToJson(this);
}
