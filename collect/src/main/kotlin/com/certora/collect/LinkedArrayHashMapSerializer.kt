package com.certora.collect

import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
internal class LinkedArrayHashMapSerializer<K, V>(
        private val keySerializer: KSerializer<K>,
        private val valueSerializer: KSerializer<V>
) : KSerializer<LinkedArrayHashMap<K, V>> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)
    override val descriptor: SerialDescriptor =
            SerialDescriptor("LinkedArrayHashMap", mapSerializer.descriptor)
    override fun serialize(encoder: Encoder, value: LinkedArrayHashMap<K, V>) {
        encoder.encodeSerializableValue(mapSerializer, value)
    }
    override fun deserialize(decoder: Decoder): LinkedArrayHashMap<K, V> {
        return LinkedArrayHashMap<K, V>(decoder.decodeSerializableValue(mapSerializer))
    }
}
