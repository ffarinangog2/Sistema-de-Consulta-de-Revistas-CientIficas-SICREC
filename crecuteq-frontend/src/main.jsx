import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "./index.css";
import "./utils/httpErrorInterceptor.js";
import App from "./App.jsx";
// INICIO - Notificaciones globales
import { NotificationProvider } from "./context/NotificationContext.jsx";
// FIN - Notificaciones globales

// INICIO - Tema inicial sin destellos; claro es el valor predeterminado.
const initialTheme = localStorage.getItem("crecuteq-theme") === "dark" ? "dark" : "light";
document.documentElement.dataset.theme = initialTheme;
document.documentElement.setAttribute("data-bs-theme", initialTheme);
// FIN - Tema inicial

createRoot(document.getElementById("root")).render(
    <StrictMode>
        <BrowserRouter>
            {/* INICIO - Notificaciones globales */}
            <NotificationProvider>
                <App />
            </NotificationProvider>
            {/* FIN - Notificaciones globales */}
        </BrowserRouter>
    </StrictMode>
);
