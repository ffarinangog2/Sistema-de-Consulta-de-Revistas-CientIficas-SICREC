// INICIO - Recuperación de contraseña
import "../styles/Login.css";
import { useState } from "react";
import { Link } from "react-router-dom";
import axios from "axios";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import investigacionImagen from "../assets/DEP INVESTIGACION.png";
// FIN - Mejoras UI/UX reutilizables

function RecuperarPassword() {

    const [correo, setCorreo] = useState("");
    const [enviando, setEnviando] = useState(false);
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    // Obtiene el texto enviado por el backend, tanto si responde con una cadena
    // como si responde con un objeto que contiene la propiedad "message" o "mensaje".
    const obtenerMensaje = (data, mensajePredeterminado) => {

        if (typeof data === "string") {
            return data;
        }

        return data?.message || data?.mensaje || mensajePredeterminado;

    };

    // Valida el correo institucional antes de consumir el endpoint existente.
    const enviarEnlaceRecuperacion = async (e) => {

        e.preventDefault();
        const correoNormalizado = correo.trim().toLowerCase();

        if (!correoNormalizado) {
            notify("Ingrese su correo institucional.", "warning");
            return;
        }

        if (!/^[^\s@]+@uteq\.edu\.ec$/i.test(correoNormalizado)) {
            notify("Ingrese un correo institucional válido (@uteq.edu.ec).", "warning");
            return;
        }

        try {

            setEnviando(true);

            const respuesta = await axios.post(
                "/api/auth/recuperar-password",
                { correo: correoNormalizado }
            );

            notify(
                obtenerMensaje(
                    respuesta.data,
                    "El enlace de recuperación fue enviado correctamente."
                ),
                "success"
            );

        } catch (error) {

            console.error(error);
            notify(
                obtenerMensaje(
                    error.response?.data,
                    "No fue posible enviar el enlace de recuperación."
                ),
                "danger"
            );

        } finally {

            setEnviando(false);

        }

    };

    return (

        <div className="login-container auth-split auth-split--compact">

            <aside className="auth-photo d-none d-lg-flex">
                <img src={investigacionImagen} alt="Edificio del Departamento de Investigación de la UTEQ" />
                <div className="auth-photo__overlay">
                    <span>Acceso seguro</span>
                    <h2>Recupera tu acceso y continúa investigando.</h2>
                    <p>Te enviaremos las instrucciones únicamente a tu correo institucional.</p>
                </div>
            </aside>

            <div className="card login-card">

                {/* INICIO - Ajuste Responsive */}
                <div className="card-body p-3 p-sm-5">

                    <div className="text-center mb-4">

                        <h1 className="logo-title">
                            Recuperar contraseña
                        </h1>

                        <p className="subtitle">
                            Ingrese su correo institucional para recibir el enlace de recuperación.
                        </p>

                    </div>

                    <form onSubmit={enviarEnlaceRecuperacion} noValidate>

                        <div className="mb-4">

                            <label className="form-label" htmlFor="correo-recuperacion">
                                Correo institucional
                            </label>

                            <input
                                id="correo-recuperacion"
                                type="email"
                                className="form-control"
                                placeholder="correo@uteq.edu.ec"
                                value={correo}
                                onChange={(e) => setCorreo(e.target.value)}
                                autoComplete="email"
                                required
                            />

                        </div>

                        {/* INICIO - Botones con estado de carga */}
                        <LoadingButton
                            type="submit"
                            className="btn btn-primary w-100"
                            loading={enviando}
                            loadingText="Enviando correo..."
                        >
                            Enviar enlace de recuperación
                        </LoadingButton>
                        {/* FIN - Botones con estado de carga */}

                        <div className="login-links justify-content-center">

                            <Link to="/login">
                                Volver al Login
                            </Link>

                        </div>

                    </form>

                </div>
                {/* FIN - Ajuste Responsive */}

            </div>

        </div>

    );

}

export default RecuperarPassword;
// FIN - Recuperación de contraseña
