package com.example.todo.db

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.Jdbi

/**
 * JDBI persistence tests against an in-memory H2 database.
 * @author dk
 */
@TestInstance( Lifecycle.PER_CLASS)
class DaoTest {
	
		val jdbi = Jdbi.create( "jdbc:h2:mem:test")
		.installPlugin( SqlObjectPlugin())
        .installPlugin( KotlinPlugin())
        .installPlugin( KotlinSqlObjectPlugin())
	
    val todoDao = jdbi.onDemand( TodoDao::class.java)
	
	@Before
	fun setUp() {
        Liquibase( "migrations.xml",
        		ClassLoaderResourceAccessor(),
        		JdbcConnection( jdbi.open().connection)).update("")
	}
	
	@Test
	fun testAddTodo() {
		val con = jdbi.open().connection
		val stmt = con.createStatement()
		stmt.execute( "DELETE FROM todo")

		val uuid = UUID.randomUUID()
		todoDao.insert( "test", uuid, "foo", "bar")
		
		stmt.execute( "SELECT * FROM todo")
		val rs = stmt.resultSet
		while (rs.next()) {
			assertEquals( "test", rs.getString( "username"))
			assertEquals( "foo",  rs.getString( "name"))
			assertEquals( "bar",  rs.getString( "description"))
			assertEquals( uuid,   rs.getObject( "id"))
		}
		
		for (row in todoDao.findAll("test")) {
			println( row)
		}

		val dao = todoDao.findById( "test", uuid)!!
		assertEquals( "foo", dao.name)
		assertEquals( "bar", dao.description)
		assertEquals( uuid, dao.id)
	}
}
