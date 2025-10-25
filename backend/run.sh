#!/bin/bash

Set environment variables for Spring Boot application
export JWT_SECRET="kAQMAOIq35Q4pwquvfXtKp9uQ5RexPxWqeCj+I918KQ="
export SPRING_DATASOURCE_URL="jdbc:postgresql://csd-asssignment-g3-1.postgres.database.azure.com:5432/tariff?sslmode=require"
export SPRING_DATASOURCE_USERNAME="dev_sonia"
export SPRING_DATASOURCE_PASSWORD="Gjk0a4*u"

# Google OAuth credentials
export GOOGLE_CLIENT_ID="1093310684513-qe8bcloi8b025cvi403gu8mmcf3ugm0j.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="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"

# GitHub OAuth credentials
export GITHUB_CLIENT_ID="Ov23liIUUctoBLOxDqX5"
export GITHUB_CLIENT_SECRET="4cf28d8cff279cca1919c7dd4bb08a09c3b428b0"

echo "✅ Environment variables exported successfully!"


# Run the Spring Boot application
kill -9 $(sudo lsof -t -i:8080)
./mvnw spring-boot:run
