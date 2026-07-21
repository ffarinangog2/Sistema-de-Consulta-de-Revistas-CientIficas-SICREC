// INICIO - Notificaciones globales
import { useContext } from "react";
import NotificationContext from "../context/notificationContextBase";

export function useNotification() {

    const context = useContext(NotificationContext);

    if (!context) {
        throw new Error("useNotification debe utilizarse dentro de NotificationProvider.");
    }

    return context;

}
// FIN - Notificaciones globales
