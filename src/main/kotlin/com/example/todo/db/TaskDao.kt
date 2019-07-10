package com.example.todo.db;

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.util.UUID
import org.jdbi.v3.sqlobject.SqlObject

/**
 * Task-related SQL queries.
 * @author dk
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
