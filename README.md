# DropWizard demo application

This is an example application of a small ReST service using [DropWizard](https://www.dropwizard.io/1.3.9/docs/).
The user can manage to-do items. A simple test UI is included.

NB: This is only an example. The intent is to demonstrate the technical stack, but not to provide a full-fledged application.
No real authentication is required.

## Features

The following techniques are demonstrated:

- Serving static content (the test UI) via the embedded [Jetty](https://www.eclipse.org/jetty/) web server,
- Validation of [DTOs](src/main/java/com/example/todo/api/DtoBase.java) and [API](src/main/java/com/example/todo/resources/TodoResource.java) endpoints,
- Declarative [data access](src/main/java/com/example/todo/db/TodoDao.java) with JDBI3,
- Custom [row mapper](src/main/java/com/example/todo/db/ResultRow.java) for JDBC results,
- Schema migrations with [LiquiBase](https://www.liquibase.org),
- Annotation-based JAX-RS response [postprocessing](src/main/java/com/example/todo/http/ResponseStatusFilter.java),
- Basic [authentication](src/main/java/com/example/todo/auth/User.java) and access control,
- A simple [health check](src/main/java/com/example/todo/health/TodoResourceHealthCheck.java),
- [Unit](src/test/java/com/example/todo/resources/TodoResourceTest.java) and [integration](src/test/java/com/example/todo/resources/TodoResourceIT.java) testing,
- Docker [packaging](Dockerfile).

## How to start the application

1. Maven and Java SE 8 are required.
1. Run `mvn clean install` to build the application,
1. Start it with `java -jar target/todo-kt-0.0.1-SNAPSHOT.jar server h2-config.yml`,
1. Try the test UI at `http://localhost:8080`, log as any user with the reversed user name as password.
