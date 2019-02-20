[![Build Status](https://travis-ci.org/xm-online/xm-ms-scheduler.svg?branch=master)](https://travis-ci.org/xm-online/xm-ms-scheduler) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=sqale_index&branch=master&project=xm-online:xm-ms-scheduler)](https://sonarcloud.io/dashboard/index/xm-online:xm-ms-scheduler) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=ncloc&branch=master&project=xm-online:xm-ms-scheduler)](https://sonarcloud.io/dashboard/index/xm-online:xm-ms-scheduler) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?&metric=coverage&branch=master&project=xm-online:xm-ms-scheduler)](https://sonarcloud.io/dashboard/index/xm-online:xm-ms-scheduler)

# scheduler
This application was generated using JHipster 4.13.2, you can find documentation and help at [http://www.jhipster.tech/documentation-archive/v4.13.2](http://www.jhipster.tech/documentation-archive/v4.13.2).

This is a "microservice" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

This application is configured for Service Discovery and Configuration with Consul. On launch, it will refuse to start if it is not able to connect to Consul at [http://localhost:8500](http://localhost:8500). For more information, read our documentation on [Service Discovery and Configuration with Consul][].

## Development

To start your application in the dev profile, simply run:

    ./gradlew


For further instructions on how to develop with JHipster, have a look at [Using JHipster in development][].

### Development with kafka

By default when you start this micro service with dev profile it uses property
`application.stream-binding-enabled = false` and does not connect to messaging system.

In case you need to send scheduled messages to real kafka destination do the following: 

1. Turn on stream binding in application properties: `application.stream-binding-enabled = true`
2. Run kafka. the easies way to do it is: 
   ```
   docker run --name kafka -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=localhost --env ADVERTISED_PORT=9092 spotify/kafka
   ```
3. Start the micro service
4. login to kafka docker and consume events:
    ```
    docker exec -it kafka bash
    /opt/kafka_2.11-0.10.1.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic scheduler_queue
    ```
   useful kafka commands:
   ```
    /opt/kafka_2.11-0.10.1.0/bin/kafka-topics.sh --list --zookeeper localhost:2181
    /opt/kafka_2.11-0.10.1.0/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
   ```

## Building for production

To optimize the scheduler application for production, run:

    ./gradlew -Pprod clean bootRepackage

To ensure everything worked, run:

    java -jar build/libs/*.war


Refer to [Using JHipster in production][] for more details.

## Testing

To launch your application's tests, run:

    ./gradlew test

For more information, refer to the [Running tests page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.

For example, to start a postgresql database in a docker container, run:

    docker-compose -f src/main/docker/postgresql.yml up -d

To stop it and remove the container, run:

    docker-compose -f src/main/docker/postgresql.yml down

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    ./gradlew bootRepackage -Pprod buildDocker

Then run:

    docker-compose -f src/main/docker/app.yml up -d

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[JHipster Homepage and latest documentation]: http://www.jhipster.tech
[JHipster 4.13.2 archive]: http://www.jhipster.tech/documentation-archive/v4.13.2
[Doing microservices with JHipster]: http://www.jhipster.tech/documentation-archive/v4.13.2/microservices-architecture/
[Using JHipster in development]: http://www.jhipster.tech/documentation-archive/v4.13.2/development/
[Service Discovery and Configuration with Consul]: http://www.jhipster.tech/documentation-archive/v4.13.2/microservices-architecture/#consul
[Using Docker and Docker-Compose]: http://www.jhipster.tech/documentation-archive/v4.13.2/docker-compose
[Using JHipster in production]: http://www.jhipster.tech/documentation-archive/v4.13.2/production/
[Running tests page]: http://www.jhipster.tech/documentation-archive/v4.13.2/running-tests/
[Setting up Continuous Integration]: http://www.jhipster.tech/documentation-archive/v4.13.2/setting-up-ci/


