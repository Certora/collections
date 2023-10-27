package benchmarks.size


val scenarioSizes = (1..256).asSequence() + sequenceOf(512, 1024, 2048, 4096, 8192, 16384, 32768)

data class ComparableSizeKey(val value: Int) : Comparable<ComparableSizeKey> {
    override fun compareTo(other: ComparableSizeKey): Int = value.compareTo(other.value)
}

data class HashableSizeKey(val value: Int)

object DummyValue
