package fi.evident.apina.java.reader

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonUnwrapped
import fi.evident.apina.java.model.type.JavaType
import fi.evident.apina.java.reader.JavaTypeMatchers.basicType
import fi.evident.apina.java.reader.JavaTypeMatchers.singletonSchema
import fi.evident.apina.java.reader.JavaTypeMatchers.typeVariable
import fi.evident.apina.java.reader.JavaTypeMatchers.typeWithRepresentation
import kotlinx.metadata.Flag
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.getterSignature
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNUSED_PARAMETER")
class ClassMetadataReaderTest {

    @Test
    fun loadingFields() {
        val javaClass = loadClass(TestClass::class.java)

        val fields = javaClass.fields

        assertThat(javaClass.schema, `is`(singletonSchema("T", basicType(CharSequence::class.java))))

        assertEquals(3, fields.size)
        assertThat(javaClass.getField("field1").type, `is`(typeWithRepresentation("java.lang.String")))
        assertThat(javaClass.getField("field2").type, `is`(typeWithRepresentation("java.util.List<java.lang.String>")))
        assertThat(javaClass.getField("field3").type, `is`(typeVariable("T")))
    }

    @Test
    fun innerClassWithOuterBounds() {
        loadClass(AnonymousInnerClassWithOuterBounds.createInnerClassInstance<Any>().javaClass)
    }

    @Test
    fun enumClasses() {
        val javaClass = loadClass(TestEnum::class.java)

        assertTrue(javaClass.isEnum)
        assertEquals(listOf("FOO", "BAR", "BAZ"), javaClass.enumConstants)
    }

    @Test
    fun `kotlin metadata`() {
        class MyKotlinClass(@Suppress("unused") @JsonIgnore @JsonUnwrapped val name: String, val baz: String)

        val javaClass = loadClass(MyKotlinClass::class.java)
        val metadata = javaClass.findAnnotation(JavaType.Basic("kotlin.Metadata"))!!

        val header = KotlinClassHeader(
            metadata.getAttribute("k") ?: 1,
            metadata.getAttribute("mv") ?: intArrayOf(0),
            metadata.getAttribute("bv") ?: intArrayOf(0),
            metadata.getAttribute("d1") ?: emptyArray(),
            metadata.getAttribute("d2") ?: emptyArray(),
            metadata.getAttribute("xs") ?: "",
            metadata.getAttribute("pn") ?: "",
            metadata.getAttribute("xi") ?: 0)

        val data = KotlinClassMetadata.read(header)
        if (data is KotlinClassMetadata.Class) {
            val cl = data.toKmClass()
            for (ctor in cl.constructors) {
                println("annotations?: ${Flag.HAS_ANNOTATIONS(ctor.flags)}")
                for (parameter in ctor.valueParameters) {
                    println(parameter.name + "/" + parameter.flags + "/ annotations?: ${Flag.HAS_ANNOTATIONS(parameter.flags)}")
                }
            }
            println(cl.properties.map { "${it.name}: field: ${it.fieldSignature}, getter: ${it.getterSignature}" })
        }

    }

    @Suppress("unused")
    private enum class TestEnum {
        FOO, BAR, BAZ;

        var instanceField: String? = null

        companion object {
            var staticField: String? = null
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private class TestClass<T : CharSequence> {

        var field1: String? = null

        var field2: List<String>? = null

        private val field3: T? = null

        fun method1() {}

        fun method2(): String = throw UnsupportedOperationException()

        fun method3(x: T): T = throw UnsupportedOperationException()
    }

    private object AnonymousInnerClassWithOuterBounds {
        fun <T> createInnerClassInstance(): Comparator<T> = Comparator { _, _ -> 0 }
    }
}
