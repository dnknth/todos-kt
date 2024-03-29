package com.example.todo.health

import java.util.Arrays
import java.util.Date

import com.codahale.metrics.health.HealthCheck
import com.example.todo.api.Task
import com.example.todo.api.Todo
import com.example.todo.auth.User
import com.example.todo.api.TodoResource


/**
 * Assert that a condition is satisfied
 * @param message error message
 * @param condition expected to be true
 */
fun assertTrue( message: String?, condition: Boolean) {
	if (!condition) throw IllegalStateException( message);
}

/**
 * Assert that two objects are equal
 * @param expected Expected value
 * @param actual Actual value
 */
fun assertEquals( expected: Any?, actual: Any?) {
	if (expected == null && actual == null) return;
	assertTrue(
		"Objects are different. Expected: <$expected>, Actual: <$actual>",
		expected != null && expected == actual);
}


/**
 * Probe the API for health
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
			assertTrue( "Todo is not in list", todo2 in todos1)
			
			api.deleteTodo( USER, todo2.id!!)
			val todos2 = api.listAllTodos( USER)
			assertEquals( n, todos2.size)
			assertTrue( "Todo is still in list", todo2 !in todos2)
		}
		catch (t: RuntimeException) {
			return Result.unhealthy( t.message)
		}
        return Result.healthy()
    }
}
