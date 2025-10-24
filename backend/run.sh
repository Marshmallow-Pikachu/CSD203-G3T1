#!/bin/bash

# Set environment variables for Spring Boot application
export JWT_SECRET="kAQMAOIq35Q4pwquvfXtKp9uQ5RexPxWqeCj+I918KQ="
export SPRING_DATASOURCE_URL="jdbc:postgresql://csd-asssignment-g3-1.postgres.database.azure.com:5432/tariff?sslmode=require"
export SPRING_DATASOURCE_USERNAME="dev_sonia"
export SPRING_DATASOURCE_PASSWORD="Gjk0a4*u"

# Run the Spring Boot application
kill -9 $(sudo lsof -t -i:8080)
./mvnw spring-boot:run
