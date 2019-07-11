package com.example.todo.http

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext
import javax.ws.rs.core.Response.Status
import javax.ws.rs.ext.Provider
import java.io.IOException
import javax.ws.rs.NameBinding;


val OK = Status.OK.getStatusCode()


/**
 * Add support for HTTP status codes other than 200.
 */
@NameBinding
@Retention( AnnotationRetention.RUNTIME)
annotation class ResponseStatus( val value : Status)


/**
 * Check whether a JAX-RS endpoint is annotated with @HttpStatus
 * and change the HTTP status code from OK to the provided value
 */
@Provider
class ResponseStatusFilter : ContainerResponseFilter {
	
	override fun filter( containerRequestContext: ContainerRequestContext,
						 containerResponseContext: ContainerResponseContext) {
		if (containerResponseContext.getStatus() == OK) {
			for (annotation in containerResponseContext.entityAnnotations) {
				if (annotation is ResponseStatus) {
					containerResponseContext.setStatus( annotation.value.statusCode)
					break
				}
			}
		}
	}

	/**
	 * Helper to register the ResponseStatusFilter with Jersey
	 */
	@Provider
	class Feature : DynamicFeature {
	
	    override fun configure( resourceInfo: ResourceInfo, context: FeatureContext) {
	        if (resourceInfo.resourceMethod.getAnnotation( ResponseStatus::class.java) != null) {
	            context.register( ResponseStatusFilter::class.java)
	        }
	    }
	}
}
