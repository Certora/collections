package com.certora.collect

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal class ArrayHashSetSerializer<E>(private val elementSerializer: KSerializer<E>) :
        KSerializer<ArrayHashSet<E>> {
    private val setSerializer = SetSerializer(elementSerializer)
    override val descriptor: SerialDescriptor =
            SerialDescriptor("ArrayHashSet", setSerializer.descriptor)
    override fun serialize(encoder: Encoder, value: ArrayHashSet<E>) {
        encoder.encodeSerializableValue(setSerializer, value)
    }
    override fun deserialize(decoder: Decoder): ArrayHashSet<E> {
        return ArrayHashSet<E>(decoder.decodeSerializableValue(setSerializer))
    }
}

