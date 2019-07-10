package com.example.todo.db

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.UUID
import org.jdbi.v3.core.kotlin.KotlinMapper


data class ResultRow private constructor( val id: UUID, val name: String?, val description: String?)