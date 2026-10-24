package io.github.mangi.glyph.hook.reflect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReflectionTest {

    @Test
    fun `method lookup never falls back to parameter arity`() {
        assertNull(
            Reflection.findMethod(
                Fixture::class.java,
                "select",
                Boolean::class.javaPrimitiveType!!,
            )
        )
    }

    @Test
    fun `method lookup matches exact parameters and compatible return type`() {
        val method = Reflection.findMethodReturning(
            Fixture::class.java,
            "select",
            CharSequence::class.java,
            String::class.java,
        )

        assertEquals("string", method?.invoke(Fixture(), "value"))
        assertNull(
            Reflection.findMethodReturning(
                Fixture::class.java,
                "select",
                Number::class.java,
                String::class.java,
            )
        )
    }

    @Test
    fun `field lookup validates its declared type`() {
        val fixture = Fixture()
        val field = Reflection.findField(Fixture::class.java, "payload", CharSequence::class.java)

        assertSame(fixture.payload, field?.get(fixture))
        assertNull(Reflection.findField(Fixture::class.java, "payload", Number::class.java))
    }

    private class Fixture {
        val payload: String = "payload"

        @Suppress("unused")
        fun select(value: String): String = "string"

        @Suppress("unused")
        fun select(value: Int): String = "int"
    }
    private open class Parent {
        private val value: CharSequence = "parent"
        private fun label(number: Int): String = "item:$number"
        private fun label(): String = "empty"
    }

    private class Child : Parent() {
        private val value: Int = 42
    }

    @Test
    fun `field lookup skips incompatible shadow and accesses private parent field`() {
        val field = requireNotNull(Reflection.findField(Child::class.java, "value", CharSequence::class.java))
        assertEquals(Parent::class.java, field.declaringClass)
        assertEquals("parent", field.get(Child()))
        assertEquals(42, Reflection.findField(Child::class.java, "value")?.get(Child()))
    }

    @Test
    fun `method lookup preserves exact parameters and assignable return type`() {
        val method = requireNotNull(
            Reflection.findMethodReturning(
                Child::class.java, "label", CharSequence::class.java, Int::class.javaPrimitiveType!!,
            ),
        )
        assertEquals("item:7", method.invoke(Child(), 7))
        assertEquals("empty", Reflection.findMethod(Child::class.java, "label")?.invoke(Child()))
        assertNull(Reflection.findMethod(Child::class.java, "label", Int::class.javaObjectType))
        assertNull(Reflection.findMethodReturning(Child::class.java, "label", Number::class.java))
    }

    @Test
    fun `optional members remain absent without throwing or searching Object`() {
        assertNull(Reflection.findField(Child::class.java, "missing"))
        assertNull(Reflection.findMethod(Child::class.java, "missing"))
        assertNull(Reflection.findMethod(Child::class.java, "toString"))
    }
}
