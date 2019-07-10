package com.example.todo.api

import java.util.UUID

/**
 * JSON-serializable task.
 * @author dk
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
