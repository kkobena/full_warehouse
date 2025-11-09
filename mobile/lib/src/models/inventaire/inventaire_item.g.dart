// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'inventaire_item.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class InventaireItemAdapter extends TypeAdapter<InventaireItem> {
  @override
  final int typeId = 1;

  @override
  InventaireItem read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return InventaireItem(
      id: fields[0] as int,
      quantityOnHand: fields[1] as int?,
      quantityInit: fields[2] as int?,
      inventoryValueCost: fields[3] as int?,
      storeInventoryId: fields[4] as int,
      produitId: fields[5] as int,
      produitLibelle: fields[6] as int,
      inventoryValueTotalCost: fields[7] as int?,
      inventoryValueAmount: fields[8] as int?,
      updated: fields[9] as bool,
      gap: fields[10] as int?,
      prixAchat: fields[11] as int,
      prixUni: fields[12] as int,
      produitCip: fields[13] as int,
      rayonId: fields[14] as int,
    );
  }

  @override
  void write(BinaryWriter writer, InventaireItem obj) {
    writer
      ..writeByte(15)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.quantityOnHand)
      ..writeByte(2)
      ..write(obj.quantityInit)
      ..writeByte(3)
      ..write(obj.inventoryValueCost)
      ..writeByte(4)
      ..write(obj.storeInventoryId)
      ..writeByte(5)
      ..write(obj.produitId)
      ..writeByte(6)
      ..write(obj.produitLibelle)
      ..writeByte(7)
      ..write(obj.inventoryValueTotalCost)
      ..writeByte(8)
      ..write(obj.inventoryValueAmount)
      ..writeByte(9)
      ..write(obj.updated)
      ..writeByte(10)
      ..write(obj.gap)
      ..writeByte(11)
      ..write(obj.prixAchat)
      ..writeByte(12)
      ..write(obj.prixUni)
      ..writeByte(13)
      ..write(obj.produitCip)
      ..writeByte(14)
      ..write(obj.rayonId);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is InventaireItemAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

InventaireItem _$InventaireItemFromJson(Map<String, dynamic> json) =>
    InventaireItem(
      id: (json['id'] as num).toInt(),
      quantityOnHand: (json['quantityOnHand'] as num?)?.toInt(),
      quantityInit: (json['quantityInit'] as num?)?.toInt(),
      inventoryValueCost: (json['inventoryValueCost'] as num?)?.toInt(),
      storeInventoryId: (json['storeInventoryId'] as num).toInt(),
      produitId: (json['produitId'] as num).toInt(),
      produitLibelle: (json['produitLibelle'] as num).toInt(),
      inventoryValueTotalCost:
          (json['inventoryValueTotalCost'] as num?)?.toInt(),
      inventoryValueAmount: (json['inventoryValueAmount'] as num?)?.toInt(),
      updated: json['updated'] as bool,
      gap: (json['gap'] as num?)?.toInt(),
      prixAchat: (json['prixAchat'] as num).toInt(),
      prixUni: (json['prixUni'] as num).toInt(),
      produitCip: (json['produitCip'] as num).toInt(),
      rayonId: (json['rayonId'] as num).toInt(),
    );

Map<String, dynamic> _$InventaireItemToJson(InventaireItem instance) =>
    <String, dynamic>{
      'id': instance.id,
      'quantityOnHand': instance.quantityOnHand,
      'quantityInit': instance.quantityInit,
      'inventoryValueCost': instance.inventoryValueCost,
      'storeInventoryId': instance.storeInventoryId,
      'produitId': instance.produitId,
      'produitLibelle': instance.produitLibelle,
      'inventoryValueTotalCost': instance.inventoryValueTotalCost,
      'inventoryValueAmount': instance.inventoryValueAmount,
      'updated': instance.updated,
      'gap': instance.gap,
      'prixAchat': instance.prixAchat,
      'prixUni': instance.prixUni,
      'produitCip': instance.produitCip,
      'rayonId': instance.rayonId,
    };
