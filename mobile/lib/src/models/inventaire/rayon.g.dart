// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'rayon.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class RayonAdapter extends TypeAdapter<Rayon> {
  @override
  final int typeId = 0;

  @override
  Rayon read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Rayon(
      id: fields[0] as int,
      code: fields[1] as String,
      libelle: fields[2] as String,
      inventoryId: fields[5] as int,
      storageId: fields[3] as int?,
      storageLibelle: fields[4] as String?,
    );
  }

  @override
  void write(BinaryWriter writer, Rayon obj) {
    writer
      ..writeByte(6)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.code)
      ..writeByte(2)
      ..write(obj.libelle)
      ..writeByte(3)
      ..write(obj.storageId)
      ..writeByte(4)
      ..write(obj.storageLibelle)
      ..writeByte(5)
      ..write(obj.inventoryId);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is RayonAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Rayon _$RayonFromJson(Map<String, dynamic> json) => Rayon(
      id: (json['id'] as num).toInt(),
      code: json['code'] as String,
      libelle: json['libelle'] as String,
      inventoryId: (json['inventoryId'] as num).toInt(),
      storageId: (json['storageId'] as num?)?.toInt(),
      storageLibelle: json['storageLibelle'] as String?,
    );

Map<String, dynamic> _$RayonToJson(Rayon instance) => <String, dynamic>{
      'id': instance.id,
      'code': instance.code,
      'libelle': instance.libelle,
      'storageId': instance.storageId,
      'storageLibelle': instance.storageLibelle,
      'inventoryId': instance.inventoryId,
    };
