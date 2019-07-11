FROM openjdk:8-jre-alpine

ARG http_port=8080
ARG admin_port=8081
ARG INSTALL_DIR=/opt/todos
ARG user=todos

RUN mkdir -p $INSTALL_DIR \
	&& apk add --no-cache tini \
	&& adduser -h $INSTALL_DIR -D ${user}
	
COPY h2-config.yml target/todo-kt-0.0.1-SNAPSHOT.jar $INSTALL_DIR/

EXPOSE ${http_port}
EXPOSE ${admin_port}

USER ${user}
ENTRYPOINT ["/sbin/tini", "--", "/usr/bin/java", "-jar", "/opt/todos/todo-kt-0.0.1-SNAPSHOT.jar", "server", "/opt/todos/h2-config.yml"]
