import axios from "axios";

export const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

// Interceptor: inject JWT + log outgoing requests
api.interceptors.request.use(
  (config) => {
    const jwtToken = localStorage.getItem("accessToken");

    if (jwtToken) {
      config.headers = config.headers ?? {};
      config.headers.Authorization = `Bearer ${jwtToken}`;
    }
    
    console.log("Sending request:", {
      method: config.method?.toUpperCase(),
      url: `${config.baseURL}${config.url}`,
      headers: config.headers,
      data: config.data,
    });

    return config;
  },
  (error) => {
    console.error("Request setup error:", error);
    return Promise.reject(error);
  }
);