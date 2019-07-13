JAR = target/todo-kt-0.0.1-SNAPSHOT.jar
TAG = dnknth/todos:kt

run: $(JAR)
	java -jar $< server h2-config.yml

$(JAR): build

build:
	mvn clean verify -D maven.test.skip

docker: build h2-config.yml
	docker build -t $(TAG) .

push: docker
	docker push $(TAG)
