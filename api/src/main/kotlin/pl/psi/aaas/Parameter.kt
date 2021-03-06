package pl.psi.aaas

import pl.psi.aaas.usecase.Symbol
import java.time.ZonedDateTime

/**
 * Parameter class represents all values communicated with the [pl.psi.aaas.Engine] (both ways).
 * Parameter is used by [pl.psi.aaas.EngineValuesSender] and [pl.psi.aaas.EngineValuesReceiver].
 * Parameter has four implementations:
 * * [Primitive]
 * * [Vector]
 * * [Matrix]
 * * [DataFrame]
 *
 * Currently supported types are:
 * * String
 * * Long
 * * Double
 * * Boolean
 * * ZonedDateTime
 */
sealed class Parameter<T : Any>(open var value: T, open val clazz: Class<T>) {
    companion object {
        @JvmStatic
        val supportedClasses: List<Class<*>> = listOf(
                java.lang.String::class.java, String::class.java,
                java.lang.Long::class.java, Long::class.java,
                java.lang.Double::class.java, Double::class.java,
                java.lang.Boolean::class.java, Boolean::class.java,
                ZonedDateTime::class.java,
                Vector::class.java)

        /**
         * Returns new [Primitive] value.
         */
        @JvmStatic
        fun <T : Any> ofPrimitive(value: T): Primitive<T> =
                with(value.javaClass) {
                    when (isSupported(this)) {
                        true -> Primitive(value, this)
                        else -> throw IllegalArgumentException("Unsupported type: ${this}")
                    }
                }

        /**
         * Returns a [Vector] of nullable elements.
         *
         * @param T array element type
         * @param value the array
         * @param elemClazz Class of element of an array
         */
        @JvmStatic
        fun <T : Any?> ofArray(value: Array<T?>, elemClazz: Class<T>): Vector<T> =
                when (isSupported(elemClazz)) {
                    true -> Vector(value, value.javaClass, elemClazz)
                    else -> throw IllegalArgumentException("Unsupported type: ${elemClazz.canonicalName}")
                }


        /**
         * Returns a [Vector] of nullable elements.
         *
         * @param T array element type
         * @param value the array
         * @param elemClazz Class of element of an array
         */
        @JvmStatic
        inline fun <reified T : Any?> ofEmptyArray(elemClazz: Class<T>): Vector<T> =
                ofArray(emptyArray())

        /**
         * Returns a [Vector] of not null elements.
         *
         * @param T array element type
         * @param value the array
         * @param elemClazz Class of element of an array
         */
        @JvmStatic
        fun <T : Any> ofArrayNotNull(value: Array<T>, elemClazz: Class<T>): Vector<T> =
                ofArray(value as Array<T?>, elemClazz)

        /**
         * Returns a [Vector] of not null elements.
         *
         * @param T array element type
         * @param value the array
         */
        @JvmStatic
        inline fun <reified T : Any?> ofArray(value: Array<T> = emptyArray()): Vector<T> =
                ofArray(value as Array<T?>, T::class.java)

        /**
         * Returns a [DataFrame] - array of [Column]s.
         */
        @JvmStatic
        fun ofDataFrame(value: Array<Column>): DataFrame {
            val columnClasses = value.map { it.vector.elemClazz }.toList() as List<Class<Any>>
            val unsupported = columnClasses.filterNot { isSupported(it) }
            return when (unsupported.size) {
                0    -> when (value.map { it.vector.value.size }.distinct().size) {
                    1    -> DataFrame(value, columnClasses.toTypedArray())
                    else -> throw IllegalArgumentException("")
                }
                else -> {
                    val notSupportedClasses = unsupported.joinToString()
                    throw IllegalArgumentException("Unsupported types: $notSupportedClasses")
                }
            }
        }

        @JvmStatic
        private fun isSupported(clazz: Class<*>): Boolean = supportedClasses.contains(clazz)
    }
}

/**
 * Primitive value that can be sent and received to the [pl.psi.aaas.Engine].
 *
 * Currently supported types are:
 * * String
 * * Long
 * * Double
 * * Boolean
 * * ZonedDateTime
 */
data class Primitive<T : Any> internal constructor(override var value: T, override val clazz: Class<T>)
    : Parameter<T>(value, clazz)

/**
 * Vector of primitive values.
 * Supports only types supported by [Primitive].
 */
data class Vector<T : Any?> internal constructor(override var value: Array<T?> = emptyArray<Any>() as Array<T?>,
                                                 override val clazz: Class<Array<T?>>, val elemClazz: Class<*>)
    : Parameter<Array<T?>>(value, clazz)

/**
 * Column of [DataFrame] consists of:
 * @param symbol column name
 * @param vector rows of given column
 */
data class Column(val symbol: Symbol, val vector: Vector<in Any>)

// TODO impl me!!
//data class Matrix<T : Any> internal constructor(override var value: Array<Array<T?>>, override val clazz: Class<Array<Array<T?>>>, val elemClazz: Class<T>)
//    : Parameter<Array<Array<T?>>>(value, clazz)

/**
 * Represents DataFrame - array of names, heterogeneous [Vector]s.
 * Only types supported by [Vector] can be used.
 */
data class DataFrame internal constructor(override var value: Array<Column> = emptyArray(),
                                          override val clazz: Class<Array<Column>>,
                                          val columnClasses: Array<Class<Any>>)
    : Parameter<Array<Column>>(value, clazz) {

    internal constructor(value: Array<Column>, columnClasses: Array<Class<Any>>) : this(value, arrayOf<Column>().javaClass, columnClasses)

}

