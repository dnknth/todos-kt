package com.example.todo.db;

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.util.UUID
import org.jdbi.v3.sqlobject.SqlObject

/**
 * Todo-related SQL queries.
 * @author dk
 */
interface TodoDao {

	@SqlUpdate( "INSERT INTO todo (user, id, name, description, created) VALUES (:user, :id, :name, :description,  current_timestamp)")
	fun insert( @Bind("user") user: String, @Bind("id") id: UUID, @Bind("name") name: String?, @Bind("description") description: String?)

	@SqlQuery( "SELECT id, name, description FROM todo WHERE user = :user ORDER BY created")
	fun findAll( @Bind("user") user: String) : List<ResultRow>
	
	@SqlQuery( "SELECT id, name, description FROM todo WHERE user = :user AND id = :id")
	fun findById( @Bind("user") user: String, @Bind("id") id: UUID) : ResultRow?

	@SqlUpdate( "DELETE FROM todo WHERE user = :user AND id = :id")
	fun deleteById( @Bind("user") user: String, @Bind("id") id: UUID)

	@SqlUpdate( "UPDATE todo SET name = :name, description = :description WHERE user = :user AND id = :id")
	fun updateById( @Bind("user") user: String, @Bind("id") id: UUID, @Bind("name") name: String?, @Bind("description") description: String?)
}