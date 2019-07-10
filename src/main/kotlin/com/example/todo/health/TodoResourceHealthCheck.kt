package com.example.todo.health

import java.util.Arrays
import java.util.Date

import com.codahale.metrics.health.HealthCheck
import com.example.todo.api.Task
import com.example.todo.api.Todo
import com.example.todo.auth.User
import com.example.todo.resources.TodoResource

/**
 * Probe the API for health
 * @author dk
 */
class TodoResourceHealthCheck( val api: TodoResource) : HealthCheck() {
	
    val USER = object : User( "-HealthCheckUser-"){}

    override fun check() : Result {

    	val timestamp = Date().getTime()
		
		val todo1 = Todo( null, "todo-" + timestamp, "test todo " + timestamp,
				listOf( Task( null, "task-" + timestamp, "test task " + timestamp)))
		
		try {
			val n = api.listAllTodos( USER).size
			val todo2 = api.createTodo( USER, todo1)

			assertTrue( "Todo ID is null", todo2.id != null)
			assertEquals( todo1.name, todo2.name)
			assertEquals( todo1.description, todo2.description)
			assertEquals( todo1.tasks.size, todo2.tasks.size)
			
			for (i in todo1.tasks.indices) {
				assertTrue( "Task ID is null", todo2.tasks[i].id != null)
				assertEquals( todo1.tasks[i].name, todo2.tasks[i].name)
				assertEquals( todo1.tasks[i].description, todo2.tasks[i].description)
			}
	
			val todo3 = api.getTodo( USER, todo2.id!!)
			assertEquals( todo2, todo3)
			
			val todos1 = api.listAllTodos( USER)
			assertEquals( n+1, todos1.size)
			assertTrue( "Todo is not in list", todos1.contains( todo2))
			
			api.deleteTodo( USER, todo2.id!!)
			val todos2 = api.listAllTodos( USER)
			assertEquals( n, todos2.size)
			assertTrue( "Todo is still in list", !todos2.contains( todo2))
		}
		catch (t: RuntimeException) {
			return Result.unhealthy( t.message)
		}
        return Result.healthy()
    }
}