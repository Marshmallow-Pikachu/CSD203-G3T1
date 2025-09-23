import axios from "axios";

export const api = axios.create({
  baseURL: "http://localhost:8080",  // points to Spring Boot (8080) in .env
  withCredentials: true,                  // keep true if using cookies/sessions
});