package io.github.mangi.glyph.hook.reflect

import com.highcapable.kavaref.KavaRef.Companion.resolve
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object Reflection {

    fun loadClassOrNull(
        name: String,
        classLoader: ClassLoader,
        onMissing: (Throwable) -> Unit,
    ): Class<*>? = try {
        Class.forName(name, false, classLoader)
    } catch (exception: ClassNotFoundException) {
        onMissing(exception)
        null
    }

    fun findField(
        clazz: Class<*>,
        name: String,
        expectedType: Class<*>? = null,
    ): Field? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            current.resolve().optional(silent = true).firstFieldOrNull {
                this.name = name
                expectedType?.let { expected -> type { expected.isAssignableFrom(it) } }
            }?.self?.let {
                it.isAccessible = true
                return it
            }
            current = current.superclass
        }
        return null
    }

    fun findMethod(
        clazz: Class<*>,
        name: String,
        vararg params: Class<*>,
    ): Method? = findMethodReturning(clazz, name, null, *params)

    fun findMethodReturning(
        clazz: Class<*>,
        name: String,
        expectedReturnType: Class<*>?,
        vararg params: Class<*>,
    ): Method? {
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            current.resolve().optional(silent = true).firstMethodOrNull {
                this.name = name
                // Keep exact JVM signatures, including primitive versus boxed parameters.
                parameters { it.toTypedArray().contentEquals(params) }
                expectedReturnType?.let { expected -> returnType { expected.isAssignableFrom(it) } }
            }?.self?.let {
                it.isAccessible = true
                return it
            }
            current = current.superclass
        }
        return null
    }
}
