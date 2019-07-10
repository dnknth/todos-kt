package com.example.todo.http;

import javax.ws.rs.NameBinding;
import javax.ws.rs.core.Response.Status;

/**
 * Add support for HTTP status codes other than 200.
 * @author dk
 */
@NameBinding
@Retention( AnnotationRetention.RUNTIME)
annotation class ResponseStatus( val value : Status)
