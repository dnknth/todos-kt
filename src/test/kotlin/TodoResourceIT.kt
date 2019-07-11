package com.example.todo.api

import org.junit.Assert.assertEquals
import  org.junit.Assert.assertFalse
import  org.junit.Assert.assertNotEquals
import  org.junit.Assert.assertNotNull
import  org.junit.Assert.assertTrue

import java.io.IOException
import java.util.ArrayList
import java.util.UUID

import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.junit.ClassRule
import org.junit.Test
import org.junit.Ignore

import com.example.todo.TodoApplication
import com.example.todo.TodoConfiguration
import com.fasterxml.jackson.databind.ObjectMapper

import com.example.todo.api.Task
import com.example.todo.api.Todo
import io.dropwizard.jackson.Jackson
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule


val OK = Status.OK.getStatusCode()
val CREATED = Status.CREATED.getStatusCode()
val NO_CONTENT = Status.NO_CONTENT.getStatusCode()
val MOVED_PERMANENTLY = Status.MOVED_PERMANENTLY.getStatusCode()
val NOT_FOUND = Status.NOT_FOUND.getStatusCode()
	

/**
 * Compare names and descriptions of a todo and its tasks.
 */
fun assertAlmostEquals( expected: Todo, actual: Todo) {
	assertEquals( expected.name, actual.name)
	assertEquals( expected.description, actual.description)
	assertEquals( expected.tasks.size, actual.tasks.size)
	
	for (i in expected.tasks.indices) {
		assertEquals( expected.tasks[i].name, actual.tasks[i].name)
		assertEquals( expected.tasks[i].description, actual.tasks[i].description)
	}
}


/**
 * Integration test for the Todo HTTP endpoints.
 * It fires ReST calls against an embedded DropWizard application backed by a H2 in-memory database.
 */
public class TodoResourceIT {
	
	val om = Jackson.newObjectMapper()
	val client = JerseyClientBuilder().build()
    
    init {
    	client.register( HttpAuthenticationFeature.basic( "test", "tset"))
    }

	public companion object {
	
	    /**
	     * Start the embedded application with a custom H2 config.
	     */
	    @ClassRule @JvmStatic
	    public val RULE = DropwizardAppRule<TodoConfiguration>(
				TodoApplication::class.java,
	            ResourceHelpers.resourceFilePath( "h2-config.yml"))
	}
		
    /**
     * Create a new Todo.
     * @param todo JSON payload without ID
     * @return saved Todo with ID
     */
    fun create( todo: Todo) : Todo  {

    	val response = client.target(
                String.format( "http://localhost:%d/todos", RULE.localPort))
               .request()
               .post( Entity.json( todo))

		assertEquals( CREATED, response.status)
        assertTrue( response.hasEntity())
        val todo2 = response.readEntity( Todo::class.java)
        assertNotNull( todo2.id)
        return todo2
    }

    /**
     * Get the list of all Todos, in no particular order
     */
    fun getAll() : List<Todo> {

    	val response = client.target(
                 String.format( "http://localhost:%d/todos", RULE.localPort))
                .request()
                .get()

        assertEquals( OK, response.status)
        assertTrue( response.hasEntity())
        return response.readEntity( object: GenericType<List<Todo>>() {})
    }

    /**
     * Get a specific Todo by ID
     * @param id object ID
     */
    fun get( id: UUID) : Todo {

    	val response = client.target(
                 String.format( "http://localhost:%d/todos/%s", RULE.localPort, id))
                .request()
                .get()

        assertEquals( OK, response.status)
        assertTrue( response.hasEntity())
        return response.readEntity( Todo::class.java)
    }
	
    /**
     * Overwrite an existing Todo
     * @param todo message payload
     */
    fun update( todo: Todo) {

    	val response = client.target(
                 String.format( "http://localhost:%d/todos/%s", RULE.localPort, todo.id))
                .request()
                .put( Entity.json( todo))

        assertEquals( NO_CONTENT, response.status)
    }
	
    /**
     * Delete a Todo by ID
     */
    fun delete( id: UUID) {

    	val response = client.target(
                 String.format( "http://localhost:%d/todos/%s", RULE.localPort, id))
                .request()
                .delete()

        assertEquals( NO_CONTENT, response.status)
    }

    /**
     * Create a Todo, make certain it shows up in the list of all tasks,
     * then retrieve it by ID and compare contents.
     */
    @Test
    fun testCreate() {

        val n = getAll().size
        
		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)

        val todo2 = create( todo1)
		assertNotEquals( todo1.id, todo2.id)
		assertAlmostEquals( todo1, todo2)
		
		for (i in todo1.tasks.indices) {
			assertNotEquals( todo1.tasks[i].id, todo2.tasks[i].id)
		}

		val todo3 = get( todo2.id!!)
		assertEquals( todo2, todo3)
		
		val todos1 = getAll()
		assertEquals( n+1, todos1.size)
		assertTrue( todos1.contains( todo2))
		
		delete( todo2.id!!)
		val todos2 = getAll()
		assertEquals( n, todos2.size)
		assertFalse( todos2.contains( todo2))
	}
	
    /**
     * Create a Todo, then change fields and tasks, and compare the results.
     */
	@Test
	fun testChangeTodo() {
		
		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		
		val todo2 = create( todo1)
		assertNotEquals( 0, todo2.tasks.size)
		
		todo2.name = "New " + todo2.name
		todo2.description = "New " + todo2.description
		todo2.tasks[0].name = "New " + todo2.tasks[0].name
		todo2.tasks[0].description = "New " + todo2.tasks[0].description
		todo2.tasks.add( Task( null, "New Task", "Some description"))
		
		update( todo2)
		
		val todo3 = get( todo2.id!!)
		assertEquals( todo2.id, todo3.id)
		assertAlmostEquals( todo3, todo3)
		
		todo2.tasks.removeAt( 0)
		update( todo2)
		val todo4 = get( todo2.id!!)
		assertEquals( todo2.id, todo3.id)
		assertAlmostEquals( todo2, todo4)
		
		delete( todo2.id!!)
	}

	/**
	 * Verify that the server sends a NOT_FOUND for nonexistent IDs.
	 */
	@Test
	fun testGetNonexisting() {

		val response = client.target(
            String.format( "http://localhost:%d/todos/%s", RULE.localPort, UUID.randomUUID()))
           .request()
           .get()

		assertEquals( NOT_FOUND, response.status)
	}

	/**
	 * Verify that the server sends a NOT_FOUND for nonsensical IDs.
	 */
	@Test
	fun testGetInvalidUuid() {

		val response = client.target(
            String.format( "http://localhost:%d/todos/IN-VAL-ID", RULE.localPort))
           .request()
           .get()

		assertEquals( NOT_FOUND, response.status)
	}

	/**
	 * Verify that the server handles empty JSON objects gracefully.
	 */
	@Test
	fun testGetInvalidContent() {

		val response = client.target(
                String.format( "http://localhost:%d/todos", RULE.localPort))
               .request()
               .post( Entity.json( Todo()))

		assertEquals( INVALID_REQUEST_BODY, response.status)
    }

	/**
	 * Verify that the server validates field length limits correctly.
	 * Fill the 120 character name fields with 128 characters.
	 */
	@Test
	fun testFieldValidation() {

		val digits = "0123456789ABCDEF" // 16 characters
		val name = digits + digits + digits + digits +
				   digits + digits + digits + digits // 128 characters
		
		val response = client.target(
                String.format( "http://localhost:%d/todos", RULE.localPort))
               .request()
               .post( Entity.json( Todo( null, name, "", listOf<Task>())))

		assertEquals( INVALID_REQUEST_BODY, response.status)
    }

	/**
	 * Verify that the server handles updates on nonexisting resources gracefully.
	 */
	@Test
	fun testInvalidUpdate() {

		val todo = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		val response = client.target(
                String.format( "http://localhost:%d/todos/%s", RULE.localPort, UUID.randomUUID()))
               .request()
               .put( Entity.json(todo))

		assertEquals( NOT_FOUND, response.status)
    }


    /**
     * Create a Todo, then change its UUID and attempt to update it.
     */
	@Test
	fun testMismatchingUidUpdate() {
		
		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		val todo2 = create( todo1)
		val id = todo2.id

		todo2.id = UUID.randomUUID()
		val response = client.target(
				String.format( "http://localhost:%d/todos/%s", RULE.localPort, id))
				.property( ClientProperties.FOLLOW_REDIRECTS, java.lang.Boolean.FALSE)
				.request()
				.put( Entity.json( todo2))

		assertEquals( MOVED_PERMANENTLY, response.status)
	}

    /**
     * Create a Todo, then attempt to access it as another user.
     */
	@Test
	fun testUserSeparation() {
		
		val todo1 = om.readValue( this::class.java.getResourceAsStream( "/fixtures/todo.json"), Todo::class.java)
		val todo2 = create( todo1)

	    val client2 = JerseyClientBuilder().build()
    	client2.register( HttpAuthenticationFeature.basic( "test2", "2tset"))
	    	
    	val response = client2.target(
                String.format( "http://localhost:%d/todos/%s", RULE.localPort, todo2.id))
               .request()
               .get()

		assertEquals( NOT_FOUND, response.status)
	}
}
