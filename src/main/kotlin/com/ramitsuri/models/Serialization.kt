package com.ramitsuri.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

object InstantSerializer: KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(decoder.decodeString()))
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }
}

object ErrorCodeSerializer: KSerializer<ErrorCode> {
    override val descriptor = PrimitiveSerialDescriptor("ErrorCode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ErrorCode {
        val errorCode = try {
            ErrorCode.fromKey(decoder.decodeString().toInt())
        } catch (e: Exception) {
            ErrorCode.UNKNOWN
        }
        return errorCode
    }

    override fun serialize(encoder: Encoder, value: ErrorCode) {
        encoder.encodeInt(value.key)
    }
}

object ActiveStatusSerializer: KSerializer<ActiveStatus> {
    override val descriptor = PrimitiveSerialDescriptor("ActiveStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ActiveStatus {
        val activeStatus = try {
            ActiveStatus.fromKey(decoder.decodeString().toInt())
        } catch (e: Exception) {
            ActiveStatus.UNKNOWN
        }
        return activeStatus
    }

    override fun serialize(encoder: Encoder, value: ActiveStatus) {
        encoder.encodeInt(value.key)
    }
}

object RepeatUnitSerializer: KSerializer<RepeatUnit> {
    override val descriptor = PrimitiveSerialDescriptor("RepeatUnit", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RepeatUnit {
        val repeatUnit = try {
            RepeatUnit.fromKey(decoder.decodeString().toInt())
        } catch (e: Exception) {
            RepeatUnit.NONE
        }
        return repeatUnit
    }

    override fun serialize(encoder: Encoder, value: RepeatUnit) {
        encoder.encodeInt(value.key)
    }
}

object ProgressStatusSerializer: KSerializer<ProgressStatus> {
    override val descriptor = PrimitiveSerialDescriptor("ProgressStatus", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ProgressStatus {
        val progressStatus = try {
            ProgressStatus.fromKey(decoder.decodeString().toInt())
        } catch (e: Exception) {
            ProgressStatus.UNKNOWN
        }
        return progressStatus
    }

    override fun serialize(encoder: Encoder, value: ProgressStatus) {
        encoder.encodeInt(value.key)
    }
}

object CreateTypeSerializer: KSerializer<CreateType> {
    override val descriptor = PrimitiveSerialDescriptor("CreateType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CreateType {
        val createType = try {
            CreateType.fromKey(decoder.decodeString().toInt())
        } catch (e: Exception) {
            CreateType.UNKNOWN
        }
        return createType
    }

    override fun serialize(encoder: Encoder, value: CreateType) {
        encoder.encodeInt(value.key)
    }
}