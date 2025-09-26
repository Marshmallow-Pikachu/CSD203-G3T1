import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.tsx";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"; 
import "./styles/index.css";

// Create the QueryClient instance
const qc = new QueryClient();

//Create the root first
const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement
);

// Then call render on it
root.render(
  <React.StrictMode>
    <QueryClientProvider client={qc}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>
);


