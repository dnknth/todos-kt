package com.example.todo.auth

import java.security.Principal
import java.util.Optional

import io.dropwizard.auth.AuthenticationException
import io.dropwizard.auth.basic.BasicCredentials

/**
 * Dummy user implementation with NO real authentication.
 */
open class User protected constructor( val username : String): Principal {
	override fun getName() = username

	/**
	 * Dummy authentication.
	 * The user name must have at least 2 characters and the
	 * password must be the reversed user name.
	 */
	class Authenticator : io.dropwizard.auth.Authenticator<BasicCredentials, User> {
	
	    override fun authenticate( credentials: BasicCredentials) : Optional<User> {
	    	val name = credentials.username
	    	val password = StringBuilder( name).reverse().toString()
	    	
	    	if ( name.length > 1 && password.equals( credentials.password)) {
	    		return Optional.of( User( credentials.username))
	    	}
	        return Optional.empty()
	    }
	}
}
