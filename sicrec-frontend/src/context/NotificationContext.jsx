// INICIO - Notificaciones globales
import { useCallback, useMemo, useState } from "react";
import NotificationContext from "./notificationContextBase";

export function NotificationProvider({ children }) {

    const [notifications, setNotifications] = useState([]);

    const removeNotification = useCallback((id) => {
        setNotifications((current) => current.filter((item) => item.id !== id));
    }, []);

    const notify = useCallback((message, type = "info") => {

        if (!message) return;

        const id = `${Date.now()}-${Math.random()}`;
        setNotifications((current) => [...current, { id, message, type }]);

        window.setTimeout(() => removeNotification(id), 5000);

    }, [removeNotification]);

    const value = useMemo(() => ({ notify }), [notify]);

    return (
        <NotificationContext.Provider value={value}>
            {children}

            <div
                className="toast-container position-fixed top-0 end-0 p-3"
                style={{ zIndex: 1090 }}
                aria-live="polite"
                aria-atomic="true"
            >
                {notifications.map(({ id, message, type }) => (
                    <div
                        key={id}
                        className={`toast show align-items-center text-bg-${type} border-0 mb-2`}
                        role="alert"
                    >
                        <div className="d-flex">
                            <div className="toast-body">{message}</div>
                            <button
                                type="button"
                                className="btn-close btn-close-white me-2 m-auto"
                                aria-label="Cerrar"
                                onClick={() => removeNotification(id)}
                            />
                        </div>
                    </div>
                ))}
            </div>
        </NotificationContext.Provider>
    );

}

// FIN - Notificaciones globales
