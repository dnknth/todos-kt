package com.example.todo.api

import org.junit.Assert.*

import java.io.IOException
import java.util.UUID

import javax.ws.rs.NotFoundException

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.junit.Before
import org.junit.Test

import com.fasterxml.jackson.databind.ObjectMapper

import com.example.todo.api.Task
import com.example.todo.api.Todo
import com.example.todo.auth.User
import com.example.todo.db.ResultRow
import io.dropwizard.jackson.Jackson
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.junit.Ignore
import org.junit.jupiter.api.BeforeAll

/**
 * Programmatic API test via direct method calls (no HTTP).
 * The test fixture sets up JDBI with an in-memory H2 database.
 * @author dk
 */
@TestInstance( Lifecycle.PER_CLASS)
public class TodoResourceTest {
	
	val om = Jackson.newObjectMapper()
    val TEST_USER = object : User( "test"){}

	val jdbi : Jdbi
	val api : TodoResource
	
	init {
	    jdbi = Jdbi.create( "jdbc:h2:mem:test")
			.installPlugin( SqlObjectPlugin())
	        .installPlugin( KotlinPlugin())
	        .installPlugin( KotlinSqlObjectPlugin())
	        .registerRowMapper( KotlinMapper( ResultRow::class.java))

		println( "--- Running migrations ---")
        Liquibase( "migrations.xml",
        		ClassLoaderResourceAccessor(),
        		JdbcConnection( jdbi.open().connection)).update("")
	
		api = TodoResource( jdbi)
	}

		
	/**
	 * Create a Todo, check that it is saved correctly and shows up in the Todo list.
	 * Then delete it and check that it is no longer in the todo list.
	 * @throws IOException bad luck
	 */
	@Test
	fun testAddTodo() {

		val n = api.listAllTodos( TEST_USER).size
		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		assertEquals( null, todo1.id)
		assertEquals( 1, todo1.tasks.size)
		assertEquals( null, todo1.tasks[0].id)
		
		val todo2 = api.createTodo( TEST_USER, todo1)
		assertNotEquals( null, todo2.id)
		assertEquals( todo1.name, todo2.name)
		assertEquals( todo1.description, todo2.description)
		assertEquals( 1, todo2.tasks.size)
		
		assertNotEquals( null, todo2.tasks[0].id)
		assertEquals( todo1.tasks[0].name, todo2.tasks[0].name)
		assertEquals( todo1.tasks[0].description, todo2.tasks[0].description)

		val todo3 = api.getTodo( TEST_USER, todo2.id!!)
		assertEquals( todo2, todo3)
		
		val todos1 = api.listAllTodos( TEST_USER)
		assertEquals( n+1, todos1.size)
		assertTrue( todos1.contains( todo2))
		
		api.deleteTodo( TEST_USER, todo2.id!!)
		val todos2 = api.listAllTodos( TEST_USER)
		assertEquals( n, todos2.size)
		assertFalse( todos2.contains( todo2))
	}

	/**
	 * Create a todo, then change some attributes and tasks and verify that the 
	 * changes are present after re-load.
	 * @throws IOException bad luck
	 */
	@Test
	fun testChangeTodo() {

		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		val todo2 = api.createTodo( TEST_USER, todo1)
		assertNotEquals( 0, todo2.tasks.size)
		
		todo2.name = "New " + todo2.name
		todo2.description = "New " + todo2.description
		todo2.tasks[0].name = "New " + todo2.tasks[0].name
		todo2.tasks[0].description = "New " + todo2.tasks[0].description
		todo2.tasks.add( Task( null, "New Task", "Some description"))
		
		api.updateTodo( TEST_USER, todo2.id!!, todo2)
		
		val todo3 = api.getTodo( TEST_USER, todo2.id!!)
		assertEquals( todo2, todo3)
		
		todo2.tasks.removeAt( 0)
		api.updateTodo( TEST_USER, todo2.id!!, todo2)
		val todo4 = api.getTodo( TEST_USER, todo2.id!!)
		assertEquals( todo2, todo4)
		
		api.deleteTodo( TEST_USER, todo2.id!!)
	}

	/**
	 * Verify that the correct exception is thrown when asking for a nonexistent Todo.
	 * @throws IOException bad luck
	 */
	@Test( expected = NotFoundException::class)
	fun testInvalidIdTodo() {
		api.getTodo( TEST_USER, UUID.randomUUID())
	}

	/**
	 * Verify that Todos of another user are not accessible.
	 * @throws IOException bad luck
	 */
	@Test( expected = NotFoundException::class)
	fun testUserSeparation() {

		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		val todo2 = api.createTodo( TEST_USER, todo1)
		
	    val user2 = object : User( "test2"){}
	    api.getTodo( user2, todo2.id!!)
	}
}
