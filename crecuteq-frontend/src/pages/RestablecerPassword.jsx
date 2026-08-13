// INICIO - Restablecer contraseña
import "../styles/Login.css";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import axios from "axios";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import PasswordField from "../components/PasswordField";
// FIN - Mejoras UI/UX reutilizables

function RestablecerPassword() {

    const [searchParams] = useSearchParams();
    const token = searchParams.get("token")?.trim() || "";

    const [nuevaPassword, setNuevaPassword] = useState("");
    const [confirmarPassword, setConfirmarPassword] = useState("");
    const [enviando, setEnviando] = useState(false);
    const [restablecida, setRestablecida] = useState(false);
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    // Obtiene el mensaje del backend independientemente de su formato de respuesta.
    const obtenerMensaje = (data, mensajePredeterminado) => {

        if (typeof data === "string") {
            return data;
        }

        return data?.message || data?.mensaje || mensajePredeterminado;

    };

    // Valida los campos y envía el token junto con las nuevas contraseñas.
    const restablecerPassword = async (e) => {

        e.preventDefault();
        if (!token) {
            notify("El enlace de recuperación no es válido o ha expirado.", "danger");
            return;
        }

        if (!nuevaPassword || !confirmarPassword) {
            notify("Complete ambos campos de contraseña.", "warning");
            return;
        }

        if (nuevaPassword !== confirmarPassword) {
            notify("Las contraseñas no coinciden.", "warning");
            return;
        }

        try {

            setEnviando(true);

            const respuesta = await axios.post(
                "/api/auth/restablecer-password",
                {
                    token,
                    nuevaPassword,
                    confirmarPassword
                }
            );

            notify(
                obtenerMensaje(
                    respuesta.data,
                    "La contraseña fue restablecida correctamente."
                ),
                "success"
            );
            setRestablecida(true);

        } catch (error) {

            console.error(error);
            notify(
                obtenerMensaje(
                    error.response?.data,
                    "El enlace de recuperación no es válido o ha expirado."
                ),
                "danger"
            );

        } finally {

            setEnviando(false);

        }

    };

    return (

        <div className="login-container">

            <div className="card login-card">

                {/* INICIO - Ajuste Responsive */}
                <div className="card-body p-3 p-sm-5">

                    <div className="text-center mb-4">

                        <h1 className="logo-title">
                            Restablecer contraseña
                        </h1>

                        <p className="subtitle">
                            Ingrese y confirme su nueva contraseña.
                        </p>

                    </div>

                    {!token && (
                        <div className="alert alert-danger" role="alert">
                            El enlace de recuperación no es válido o ha expirado.
                        </div>
                    )}

                    <form onSubmit={restablecerPassword} noValidate>

                        {/* INICIO - Botón mostrar contraseña y validaciones visuales */}
                        <PasswordField
                            id="nueva-password-restablecer"
                            label="Nueva contraseña"
                            value={nuevaPassword}
                            onChange={(e) => setNuevaPassword(e.target.value)}
                            autoComplete="new-password"
                            disabled={!token || restablecida}
                            required
                        />

                        <PasswordField
                            id="confirmar-password-restablecer"
                            label="Confirmar contraseña"
                            className="mb-4"
                            value={confirmarPassword}
                            onChange={(e) => setConfirmarPassword(e.target.value)}
                            autoComplete="new-password"
                            disabled={!token || restablecida}
                            required
                        />
                        {/* FIN - Botón mostrar contraseña y validaciones visuales */}

                        {!restablecida ? (
                            <LoadingButton
                                type="submit"
                                className="btn btn-primary w-100"
                                disabled={!token || enviando}
                                loading={enviando}
                                loadingText="Restableciendo contraseña..."
                            >
                                Restablecer contraseña
                            </LoadingButton>
                        ) : (
                            <Link className="btn btn-primary w-100" to="/login">
                                Ir al Login
                            </Link>
                        )}

                        {!restablecida && (
                            <div className="login-links justify-content-center">

                                <Link to="/login">
                                    Volver al Login
                                </Link>

                            </div>
                        )}

                    </form>

                </div>
                {/* FIN - Ajuste Responsive */}

            </div>

        </div>

    );

}

export default RestablecerPassword;
// FIN - Restablecer contraseña
