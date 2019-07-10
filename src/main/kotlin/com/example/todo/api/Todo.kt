package com.example.todo.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import javax.validation.Valid

/**
 * JSON-serializable todo item.
 * @author dk
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
