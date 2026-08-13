import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import MainLayout from "../layouts/MainLayout";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import PasswordField from "../components/PasswordField";
// FIN - Mejoras UI/UX reutilizables

function CambiarPassword() {

    const navigate = useNavigate();
    // INICIO - Layout según el rol autenticado
    const usuarioAutenticado = JSON.parse(localStorage.getItem("usuario") || "null");
    const esAdministrador = usuarioAutenticado?.rol === "ADMIN";
    // FIN - Layout según el rol autenticado

    const [passwordActual, setPasswordActual] = useState("");
    const [nuevaPassword, setNuevaPassword] = useState("");
    const [confirmarPassword, setConfirmarPassword] = useState("");
    // INICIO - Botones con estado de carga
    const [cargando, setCargando] = useState(false);
    // FIN - Botones con estado de carga
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const guardarCambios = async () => {

        if (
            !passwordActual ||
            !nuevaPassword ||
            !confirmarPassword
        ) {

            notify("Complete todos los campos.", "warning");
            return;

        }

        if (nuevaPassword !== confirmarPassword) {

            notify("Las contraseñas nuevas no coinciden.", "warning");
            return;

        }

        try {

            setCargando(true);

            await axios.put(
                "/api/auth/cambiar-password",
                {
                    passwordActual,
                    nuevaPassword,
                    confirmarPassword
                }
            );

            notify(
                "Contraseña actualizada correctamente. Inicie sesión nuevamente.",
                "success"
            );

            localStorage.removeItem("usuario");

            navigate("/login");

        } catch (error) {

            console.error(error);

            if (error.response?.data) {

                const datos = error.response.data;
                const mensaje = typeof datos === "string"
                    ? datos
                    : datos.message || datos.error;

                notify(
                    mensaje || "No se pudo cambiar la contraseña.",
                    "danger"
                );

            } else {

                notify("No se pudo cambiar la contraseña.", "danger");

            }

        } finally {

            // INICIO - Botones con estado de carga
            setCargando(false);
            // FIN - Botones con estado de carga

        }

    };

    return (

        <MainLayout admin={esAdministrador}>

            <h2 className="mb-4">
                Cambiar contraseña
            </h2>

            <div className="card shadow border-0">

                <div className="card-body">

                    {/* INICIO - Botón mostrar contraseña y validaciones visuales */}
                    <PasswordField
                        id="password-actual"
                        label="Contraseña actual"
                        value={passwordActual}
                        onChange={(e) => setPasswordActual(e.target.value)}
                        autoComplete="current-password"
                    />

                    <PasswordField
                        id="nueva-password"
                        label="Nueva contraseña"
                        value={nuevaPassword}
                        onChange={(e) => setNuevaPassword(e.target.value)}
                        autoComplete="new-password"
                    />

                    <PasswordField
                        id="confirmar-password"
                        label="Confirmar contraseña"
                        value={confirmarPassword}
                        onChange={(e) => setConfirmarPassword(e.target.value)}
                        autoComplete="new-password"
                    />
                    {/* FIN - Botón mostrar contraseña y validaciones visuales */}

                    {/* INICIO - Botones con estado de carga */}
                    <LoadingButton
                        className="btn btn-primary"
                        onClick={guardarCambios}
                        loading={cargando}
                        loadingText="Cambiando contraseña..."
                    >
                        Guardar cambios
                    </LoadingButton>
                    {/* FIN - Botones con estado de carga */}

                </div>

            </div>

        </MainLayout>

    );

}

export default CambiarPassword;
