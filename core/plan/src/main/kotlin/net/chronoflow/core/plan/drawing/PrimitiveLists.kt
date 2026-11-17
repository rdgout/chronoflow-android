package net.chronoflow.core.plan.drawing

internal class FloatList(
    capacity: Int = 64,
) {
    private var data = FloatArray(capacity)
    var size = 0
        private set

    fun add(value: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun addAll(other: FloatList) {
        for (i in 0 until other.size) add(other.data[i])
    }

    fun at(index: Int): Float = data[index]

    fun clear() {
        size = 0
    }

    fun toArray(): FloatArray = data.copyOf(size)
}

internal class IntList(
    capacity: Int = 32,
) {
    private var data = IntArray(capacity)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun addAll(other: IntList) {
        for (i in 0 until other.size) add(other.data[i])
    }

    fun clear() {
        size = 0
    }

    fun toArray(): IntArray = data.copyOf(size)
}

internal class ByteList(
    capacity: Int = 32,
) {
    private var data = ByteArray(capacity)
    var size = 0
        private set

    fun add(value: Byte) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = value
    }

    fun addAll(other: ByteList) {
        for (i in 0 until other.size) add(other.data[i])
    }

    fun clear() {
        size = 0
    }

    fun toArray(): ByteArray = data.copyOf(size)
}
