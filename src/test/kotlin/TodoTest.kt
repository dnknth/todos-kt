package com.example.todo.api

/**
 * Test API object attributes.
 */
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

public class TodoTest {

	@Test
	fun testEquality() {
		val uuid = UUID.randomUUID()
		val todo1 = Todo( uuid, "foo", "bar", listOf())
		assertEquals( uuid, todo1.id)
		assertEquals( "foo", todo1.name)
		assertEquals( "bar", todo1.description)
		assertEquals( 0, todo1.tasks.size)
	}
}
