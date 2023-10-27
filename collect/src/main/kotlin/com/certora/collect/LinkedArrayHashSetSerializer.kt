package com.certora.collect

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal class LinkedArrayHashSetSerializer<E>(private val elementSerializer: KSerializer<E>) :
        KSerializer<LinkedArrayHashSet<E>> {
    private val setSerializer = SetSerializer(elementSerializer)
    override val descriptor: SerialDescriptor =
            SerialDescriptor("LinkedArrayHashSet", setSerializer.descriptor)
    override fun serialize(encoder: Encoder, value: LinkedArrayHashSet<E>) {
        encoder.encodeSerializableValue(setSerializer, value)
    }
    override fun deserialize(decoder: Decoder): LinkedArrayHashSet<E> {
        return LinkedArrayHashSet<E>(decoder.decodeSerializableValue(setSerializer))
    }
}
