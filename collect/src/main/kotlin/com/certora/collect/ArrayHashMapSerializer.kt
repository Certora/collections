package com.certora.collect

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal class ArrayHashMapSerializer<K, V>(
        private val keySerializer: KSerializer<K>,
        private val valueSerializer: KSerializer<V>
) : KSerializer<ArrayHashMap<K, V>> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)
    override val descriptor: SerialDescriptor =
            SerialDescriptor("ArrayHashMap", mapSerializer.descriptor)
    override fun serialize(encoder: Encoder, value: ArrayHashMap<K, V>) {
        encoder.encodeSerializableValue(mapSerializer, value)
    }
    override fun deserialize(decoder: Decoder): ArrayHashMap<K, V> {
        return ArrayHashMap<K, V>(decoder.decodeSerializableValue(mapSerializer))
    }
}
