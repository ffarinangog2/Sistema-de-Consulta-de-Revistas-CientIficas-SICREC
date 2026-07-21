import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import "./index.css";
import App from "./App.jsx";
// INICIO - Notificaciones globales
import { NotificationProvider } from "./context/NotificationContext.jsx";
// FIN - Notificaciones globales

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
