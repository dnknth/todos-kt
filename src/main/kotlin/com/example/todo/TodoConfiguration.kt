package com.example.todo

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import javax.validation.Valid

/**
 * YAML configuration for the Todo service
 * @author dk
 */
class TodoConfiguration : Configuration() {

	@Valid
	@JsonProperty("database")
    var dataSourceFactory = DataSourceFactory()
}
