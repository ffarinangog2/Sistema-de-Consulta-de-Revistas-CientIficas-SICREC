import axios from "axios";

// Adjunta el JWT de la sesion a todas las llamadas de la API. Los servicios
// que ya lo envian explicitamente conservan su cabecera sin cambios.
axios.interceptors.request.use((config) => {
    try {
        const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
        if (usuario?.token && !config.headers?.Authorization) {
            config.headers = config.headers || {};
            config.headers.Authorization = `Bearer ${usuario.token}`;
        }
    } catch {
        // Una sesion local malformada no debe impedir login o recuperacion.
    }
    return config;
});

const MENSAJE_SERVICIO =
    "El servicio está temporalmente no disponible. Intente nuevamente en unos momentos.";
const MENSAJE_RED =
    "No fue posible conectar con el servidor. Verifique su conexión e intente nuevamente.";

const pareceHtml = (data, contentType) => {
    if (String(contentType || "").toLowerCase().includes("text/html")) return true;
    return typeof data === "string" && /^\s*(?:<!doctype\s+html|<html)/i.test(data);
};

// Normaliza fallos de infraestructura antes de que lleguen a las pantallas.
// Así ninguna vista muestra como mensaje una página HTML generada por Nginx.
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        const response = error?.response;
        if (response) {
            const contentType = response.headers?.["content-type"];
            if ([502, 503, 504].includes(response.status) || pareceHtml(response.data, contentType)) {
                response.data = { message: MENSAJE_SERVICIO };
            }
        } else if (error && error.code !== "ERR_CANCELED") {
            error.userMessage = MENSAJE_RED;
        }
        return Promise.reject(error);
    }
);
