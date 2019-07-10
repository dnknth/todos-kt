package com.example.todo.api

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.Length
import java.util.UUID


const val NAME_LENGTH = 120
const val DESCRIPTION_LENGTH = 2048

/**
 * Common JSON attributes for Tasks and Todos.
 * @author dk
 */
open class DtoBase(
    @JsonProperty var id: UUID? = null,
    @JsonProperty @Length(max = NAME_LENGTH) var name: String? = null,
    @JsonProperty @Length(max = DESCRIPTION_LENGTH) var description: String? = null
)