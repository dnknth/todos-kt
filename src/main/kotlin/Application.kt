package com.example.todo

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature
import org.jdbi.v3.core.Jdbi

import com.example.todo.auth.User
import com.example.todo.db.ResultRow
import com.example.todo.health.TodoResourceHealthCheck
import com.example.todo.http.ResponseStatusFilter
import com.example.todo.api.TodoResource
import com.example.todo.api.API_BASE_URI

import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.migrations.MigrationsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.core.kotlin.KotlinPlugin

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import javax.validation.Valid


/**
 * YAML configuration for the Todo service
 */
class TodoConfiguration : Configuration() {

	@Valid
	@JsonProperty("database")
    var dataSourceFactory = DataSourceFactory()
}

/**
 * Main application for the Todo service.
 */
class TodoApplication : Application<TodoConfiguration>() {

	override fun getName() = "todo"
	
    /**
     * Initialize the application.
     * Register LiquiBase migrations and
     * static assets needed for the test UI.
     */
    override fun initialize( bootstrap: Bootstrap<TodoConfiguration>) {
        bootstrap.addBundle( object: MigrationsBundle<TodoConfiguration>() {
            override fun getDataSourceFactory( configuration: TodoConfiguration) 
                = configuration.dataSourceFactory
        })
        bootstrap.addBundle( AssetsBundle( "/assets", "/", "index.html"))
    }

    /**
     * Run the application.
     * LiquiBase migrations are automatically applied on startup,
     * so it is possible to run on an empty DB. Integration tests
     * rely on this. 
     */
    override fun run( configuration: TodoConfiguration, environment: Environment) {
    	
    	// Initial JDBI setup
        val jdbi = JdbiFactory().build(
        		environment, configuration.dataSourceFactory, "db")
				.installPlugin( SqlObjectPlugin())
		        .installPlugin( KotlinPlugin())
        		.installPlugin( KotlinSqlObjectPlugin())
        	.registerRowMapper( KotlinMapper( ResultRow::class.java))
        
        // Run DB migrations
		try {
			Liquibase( "migrations.xml",
				ClassLoaderResourceAccessor(),
				JdbcConnection( jdbi.open().connection)).update("")
		} catch( e: LiquibaseException) {
			throw IllegalStateException( "Database migration failed")
		}

		// Change API base URL to avoid conflict with static assets
        environment.jersey().setUrlPattern( API_BASE_URI + "/*")
        
        // Set up (dummy) authentication
        environment.jersey().register( AuthDynamicFeature(
                BasicCredentialAuthFilter.Builder<User>()
                    .setAuthenticator( User.Authenticator())
                    .setRealm( "To do list")
                    .buildAuthFilter()))
        environment.jersey().register( RolesAllowedDynamicFeature::class.java)
        environment.jersey().register( AuthValueFactoryProvider.Binder<User>(User::class.java))

        // Set up health check
        val api = TodoResource( jdbi)
        environment.healthChecks().register( "api", TodoResourceHealthCheck( api))
        
        // Set up URL routes and filters
        environment.jersey().register( ResponseStatusFilter.Feature::class.java)
    	environment.jersey().register( api)
    }
}

/**
 * Main entry point
 * @param args command line arguments
 * @throws Exception something went wrong
 */
fun main( args: Array<String>) {
    TodoApplication().run( *args)
}

