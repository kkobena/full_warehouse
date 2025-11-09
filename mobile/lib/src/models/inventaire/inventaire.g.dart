// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'inventaire.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class InventaireAdapter extends TypeAdapter<Inventaire> {
  @override
  final int typeId = 3;

  @override
  Inventaire read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Inventaire(
      id: fields[0] as int?,
      description: fields[1] as String,
      inventoryValueCostBegin: fields[2] as int?,
      inventoryAmountBegin: fields[3] as int?,
      inventoryValueCostAfter: fields[4] as int?,
      inventoryAmountAfter: fields[5] as int?,
      gapCost: fields[6] as int?,
      gapAmount: fields[7] as int?,
      statut: fields[8] as String?,
      inventoryType: fields[9] as String?,
      inventoryCategory: fields[10] as String?,
    );
  }

  @override
  void write(BinaryWriter writer, Inventaire obj) {
    writer
      ..writeByte(11)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.description)
      ..writeByte(2)
      ..write(obj.inventoryValueCostBegin)
      ..writeByte(3)
      ..write(obj.inventoryAmountBegin)
      ..writeByte(4)
      ..write(obj.inventoryValueCostAfter)
      ..writeByte(5)
      ..write(obj.inventoryAmountAfter)
      ..writeByte(6)
      ..write(obj.gapCost)
      ..writeByte(7)
      ..write(obj.gapAmount)
      ..writeByte(8)
      ..write(obj.statut)
      ..writeByte(9)
      ..write(obj.inventoryType)
      ..writeByte(10)
      ..write(obj.inventoryCategory);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is InventaireAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Inventaire _$InventaireFromJson(Map<String, dynamic> json) => Inventaire(
      id: (json['id'] as num?)?.toInt(),
      description: json['description'] as String,
      inventoryValueCostBegin:
          (json['inventoryValueCostBegin'] as num?)?.toInt(),
      inventoryAmountBegin: (json['inventoryAmountBegin'] as num?)?.toInt(),
      inventoryValueCostAfter:
          (json['inventoryValueCostAfter'] as num?)?.toInt(),
      inventoryAmountAfter: (json['inventoryAmountAfter'] as num?)?.toInt(),
      gapCost: (json['gapCost'] as num?)?.toInt(),
      gapAmount: (json['gapAmount'] as num?)?.toInt(),
      statut: json['statut'] as String?,
      inventoryType: json['inventoryType'] as String?,
      inventoryCategory: json['inventoryCategory'] as String?,
    );

Map<String, dynamic> _$InventaireToJson(Inventaire instance) =>
    <String, dynamic>{
      'id': instance.id,
      'description': instance.description,
      'inventoryValueCostBegin': instance.inventoryValueCostBegin,
      'inventoryAmountBegin': instance.inventoryAmountBegin,
      'inventoryValueCostAfter': instance.inventoryValueCostAfter,
      'inventoryAmountAfter': instance.inventoryAmountAfter,
      'gapCost': instance.gapCost,
      'gapAmount': instance.gapAmount,
      'statut': instance.statut,
      'inventoryType': instance.inventoryType,
      'inventoryCategory': instance.inventoryCategory,
    };
