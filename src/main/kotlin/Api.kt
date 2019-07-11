package com.example.todo.api

import java.net.URI
import java.net.URISyntaxException
import java.util.HashSet
import java.util.UUID
import java.util.stream.Collectors

import javax.annotation.security.PermitAll
import javax.validation.Valid
import javax.ws.rs.ClientErrorException
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.NotFoundException
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.RedirectionException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response.Status

import org.jdbi.v3.core.Jdbi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.codahale.metrics.annotation.Timed
import com.example.todo.auth.User
import com.example.todo.db.ResultRow
import com.example.todo.db.TaskDao
import com.example.todo.db.TodoDao
import com.example.todo.http.ResponseStatus

import io.dropwizard.auth.Auth
import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.Length


const val NAME_LENGTH = 120
const val DESCRIPTION_LENGTH = 2048

const val INVALID_REQUEST_BODY = 422
const val API_BASE_URI = "/todos"


/**
 * Check if a string is null or empty
 */
private fun isEmpty( s : String?) = (s == null || s.isEmpty())


/**
 * Common JSON attributes for Tasks and Todos.
 */
open class DtoBase(
    @JsonProperty var id: UUID? = null,
    @JsonProperty @Length(max = NAME_LENGTH) var name: String? = null,
    @JsonProperty @Length(max = DESCRIPTION_LENGTH) var description: String? = null
)


/**
 * JSON-serializable task.
 */
class Task() : DtoBase() {
	constructor( id: UUID?, name: String?, description: String?) : this() {
		this.id = id
		this.name = name
		this.description = description
	}

	override fun equals( other: Any?) = other != null && other is Task
			&& id == other.id && name == other.name && description == other.description
}


/**
 * JSON-serializable todo item.
 */
class Todo() : DtoBase() {

	constructor( id: UUID?, name: String?, description: String?, tasks: List<Task>) : this() {
		this.id = id
		this.name = name
		this.description = description
		this.tasks = tasks.toMutableList()
	}

	@Valid
    @JsonProperty
	var tasks = mutableListOf<Task>()
	
	override fun equals( other: Any?) = other != null && other is Todo
			&& id == other.id && name == other.name && description == other.description
			&& tasks == other.tasks
}


/**
 * ReST endpoint provider.
 * <ul>
 * <li> GET /todos → Returns a list of all TODOs </li>
 * <li> POST /todos → Expects a TODO (without id) and returns a TODO with ID </li>
 * <li> GET /todos/{id} → Returns a TODO </li>
 * <li> PUT /todos/{id} → Overwrites an existing TODO </li>
 * <li> DELETE /todos/{id} → Deletes a TODO </li> 
 * </ul>
 */
@PermitAll
@Path( "/")
@Produces( MediaType.APPLICATION_JSON)
public class TodoResource( val jdbi: Jdbi) {

	val log = LoggerFactory.getLogger( TodoResource::class.java)
	val taskDao : TaskDao = jdbi.onDemand( TaskDao::class.java)
	val todoDao : TodoDao = jdbi.onDemand( TodoDao::class.java)

    /**
     * Get the current user's name
     * @param user authenticated user
     * @return User name
     */
    @Timed
    @GET @Path( "whoami")
    fun whoAmI( @Auth user: User) = "\"${user.name}\""

    /**
     * Get the list of all Todos
     * @param user authenticated user
     * @return List of all todo items for the user in reverse chronological order of creation
     */
    @Timed
    @Valid
    @GET
    fun listAllTodos( @Auth user: User) =
		todoDao.findAll( user.name)
			.map{ Todo( it.id, it.name, it.description, getTasks( it.id)) }

    /**
     * Load tasks for a given Todo
     * @param todoId Todo ID
     * @return
     */
    fun getTasks( todoId: UUID) =
    	taskDao.findByTodoId( todoId)
			.map{ Task( it.id, it.name, it.description)}

    /**
     * Get a Todo by ID
     * @param user authenticated user
     * @param id Todo ID
     * @return todo item
     */
    @Timed
    @Valid
    @GET @Path( "{id}")
    fun getTodo( @Auth user: User, @PathParam("id") id: UUID) : Todo {
    	val d = todoDao.findById( user.name, id)
		if (d == null) throw NotFoundException( id.toString());
        return Todo( id, d.name, d.description, getTasks( id))
    }

    /**
     * Create a new Todo.
     * @param user authenticated user
     * @param todo Todo to store, must not have an ID
     * @return copy of the original Todo with new ID and new Task IDs
     */
    @Timed
    @Valid
    @POST @Consumes( MediaType.APPLICATION_JSON)
    @ResponseStatus( Status.CREATED) // See RFC 2616, §9.5
    fun createTodo( @Auth user: User, @Valid todo: Todo) : Todo {
    	
    	// Insist that either name or description is present
    	if (isEmpty( todo.name) && isEmpty( todo.description)) {
    		throw ClientErrorException( "Invalid request body", INVALID_REQUEST_BODY)
    	}
    	
    	// Require that the new Todo has no ID
    	if (todo.id != null) {
    		throw ClientErrorException( "Invalid request body", INVALID_REQUEST_BODY)
    	}
    	
    	val todoId = UUID.randomUUID()
    	log.debug( "Adding Todo {}", todoId)
    	todoDao.insert( user.name, todoId, todo.name, todo.description)
    	
    	for (i in todo.tasks.indices) {
    		val task = todo.tasks[i]

        	// Skip task if neither name nor description are present
    		if (isEmpty( task.name) && isEmpty( task.description)) continue
        	
    		val taskId = UUID.randomUUID()
        	log.debug( "Adding Task {}", taskId)
        	taskDao.insert( taskId, task.name, task.description, todoId, i)
    	}
    	return getTodo( user, todoId)
    }

    /**
     * Delete a Todo
     * @param user authenticated user
     * @param id Todo ID
     */
    @Timed
    @DELETE @Path( "{id}")
    @ResponseStatus( Status.NO_CONTENT) // See RFC 2616, §9.7
    fun deleteTodo( @Auth user: User, @PathParam("id") id: UUID) {
    	log.debug( "Deleting Todo {}", id)
    	todoDao.deleteById( user.name, id)
    }

    /**
     * Update the contents a a Todo by ID. The Todo must already exist.
     * If any contained Tasks do not exist, they are created. Otherwise they are updated.
     * Tasks with empty content are deleted.
     * 
     * @param user authenticated user
     * @param id Todo ID
     * @param todo Todo content
     */
    @Timed
    @PUT @Path( "{id}")
    @Consumes( MediaType.APPLICATION_JSON)
    @ResponseStatus( Status.NO_CONTENT) // See RFC 2616, §9.6
    fun updateTodo( @Auth user: User, @PathParam("id") id: UUID, @Valid todo: Todo) {

    	// Refuse action if neither name nor description are present
    	if (isEmpty( todo.name) && isEmpty( todo.description)) {
    		throw ClientErrorException( "Invalid request body", INVALID_REQUEST_BODY)
    	}
    	
    	// Ensure that the Todo ID in the request body matches the ID given in the URL
		try {
			if (!id.equals( todo.id)) {
				throw RedirectionException( Status.MOVED_PERMANENTLY,
						URI( String.format( "%s/%s", API_BASE_URI, todo.id)))
			}
		} catch (e: URISyntaxException) { // Never reached
    		throw ClientErrorException( "Invalid request body", INVALID_REQUEST_BODY)
		}

    	val d = todoDao.findById( user.name, id)
		if (d == null) throw NotFoundException( id.toString());

    	log.debug( "Updating Todo {}", todo.id)
    	todoDao.updateById( user.name, id, todo.name, todo.description)
    	
    	val uids = mutableSetOf<UUID>()
    	for (i in todo.tasks.indices) {
    		val task = todo.tasks[i]
    		
        	// Skip task if neither name nor description are present
        	if (isEmpty( task.name) && isEmpty( task.description)) continue
        	
    		val taskid = task.id ?: UUID.randomUUID()
    		if (task.id == null) {
            	log.debug( "Adding Task {}", taskid)
				task.id = taskid;
    			taskDao.insert( taskid, task.name, task.description, id, i)
    		}
    		else {
    			// Security: Prevent update of Tasks belonging to other Todos
    			// by also matching against the Todo ID
            	log.debug( "Updating Task {}", task.id)
    			taskDao.updateById( taskid, id, task.name, task.description, i)
    		}
    		uids.add( taskid)
    	}
    	
    	for (task in taskDao.findByTodoId(id)) {
    		if (!uids.contains( task.id)) {
    			// Security: Prevent deletion of Tasks belonging to other Todos
    			// by also matching against the Todo ID
            	log.debug( "Deleting Task {}", task.id)
    			taskDao.deleteById( task.id, id)
    		}
    	}
    }
}
