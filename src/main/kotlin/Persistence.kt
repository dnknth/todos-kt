package com.example.todo.db

import java.util.UUID
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.SqlObject


data class ResultRow private constructor( val id: UUID, val name: String?, val description: String?)


/**
 * Task-related SQL queries.
 */
interface TaskDao {

	@SqlUpdate( "INSERT INTO task (id, name, description, todo_id, position) VALUES (:id, :name, :description, :todo_id, :position)")
	fun insert( @Bind("id") id: UUID, @Bind("name") name: String?, @Bind("description") description: String?, @Bind("todo_id") todo_id: UUID, @Bind("position") position: Int)

	@SqlQuery( "SELECT id, name, description FROM task WHERE todo_id = :todo_id ORDER BY position")
	fun findByTodoId( @Bind("todo_id") todo_id: UUID ) : List<ResultRow>

	@SqlUpdate( "DELETE FROM task WHERE id = :id")
	fun deleteById( @Bind("id") id: UUID, @Bind("todo_id") todo_id: UUID)

	@SqlUpdate( "UPDATE task SET name = :name, description = :description, position = :position WHERE id = :id AND todo_id = :todo_id")
	fun updateById( @Bind("id") id: UUID, @Bind("todo_id") todo_id: UUID, @Bind("name") name: String?, @Bind("description") description: String?, @Bind("position") position: Int)
}


/**
 * Todo-related SQL queries.
 */
interface TodoDao {

	@SqlUpdate( "INSERT INTO todo (username, id, name, description, created) VALUES (:user, :id, :name, :description,  current_timestamp)")
	fun insert( @Bind("user") user: String, @Bind("id") id: UUID, @Bind("name") name: String?, @Bind("description") description: String?)

	@SqlQuery( "SELECT id, name, description FROM todo WHERE username = :user ORDER BY created")
	fun findAll( @Bind("user") user: String) : List<ResultRow>
	
	@SqlQuery( "SELECT id, name, description FROM todo WHERE username = :user AND id = :id")
	fun findById( @Bind("user") user: String, @Bind("id") id: UUID) : ResultRow?

	@SqlUpdate( "DELETE FROM todo WHERE username = :user AND id = :id")
	fun deleteById( @Bind("user") user: String, @Bind("id") id: UUID)

	@SqlUpdate( "UPDATE todo SET name = :name, description = :description WHERE username = :user AND id = :id")
	fun updateById( @Bind("user") user: String, @Bind("id") id: UUID, @Bind("name") name: String?, @Bind("description") description: String?)
}
