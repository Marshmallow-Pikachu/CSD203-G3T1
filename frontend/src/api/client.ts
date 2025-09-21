import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,  // points to Spring Boot (8080) in .env
  withCredentials: true,                  // keep true if using cookies/sessions
});