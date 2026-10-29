# sampleClient

A Spring Boot 4 service that calls [sampleServer](https://github.com/danieltalik/sampleServer) through an
OpenFeign client generated from `src/main/resources/PetStoreApi.yml`.

## Run

Start sampleServer first (port 8085), then:

```bash
mvn spring-boot:run                       # http://localhost:8080
PETS_URL=http://other-host:8085 mvn spring-boot:run
```

- `GET /pets` and `GET /pets/{id}` proxy to sampleServer
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Errors come back as problem details: 404s from the server pass through, server errors become 502,
and an unreachable server becomes 503.

Requires Java 21.

## Test

```bash
mvn verify
```

The spec is a copy of sampleServer's. Change both together.
