import "../styles/Login.css";
import { useState } from "react";
// INICIO - Navegación a recuperación de contraseña
import { Link, useNavigate } from "react-router-dom";
// FIN - Navegación a recuperación de contraseña
import axios from "axios";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import PasswordField from "../components/PasswordField";
// FIN - Mejoras UI/UX reutilizables

function Login() {

    const [usuario, setUsuario] = useState("");
    const [password, setPassword] = useState("");
    // INICIO - Botones con estado de carga
    const [cargando, setCargando] = useState(false);
    // FIN - Botones con estado de carga

    const navigate = useNavigate();
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const iniciarSesion = async (e) => {

        e.preventDefault();

        setCargando(true);

        try {

            const respuesta = await axios.post(
                "/api/auth/login",
                {
                    usuario: usuario,
                    password: password
                }
            );

            const usuarioLogueado = respuesta.data;

            localStorage.setItem(
                "usuario",
                JSON.stringify(usuarioLogueado)
            );

            if (usuarioLogueado.debeCambiarPassword) {

                navigate("/usuario/cambiar-password");
                return;

            }

            if (usuarioLogueado.rol === "ADMIN") {

                navigate("/admin/dashboard");

            } else if (
                usuarioLogueado.rol === "DOCENTE" ||
                usuarioLogueado.rol === "ESTUDIANTE" ||
                usuarioLogueado.rol === "PERSONAL_DE_INVESTIGACION"
            ) {

                navigate("/usuario/dashboard");

            } else {

                notify("Rol no reconocido.", "warning");

            }

        } catch (error) {

            console.error(error);
            // INICIO - Mensajes reales del backend
            const respuestaError = error.response?.data;
            const mensajeBackend = typeof respuestaError === "string"
                ? respuestaError
                : respuestaError?.message ||
                  respuestaError?.mensaje ||
                  respuestaError?.error;

            notify(
                mensajeBackend || "No fue posible iniciar sesión.",
                "danger"
            );
            // FIN - Mensajes reales del backend

        } finally {

            // INICIO - Botones con estado de carga
            setCargando(false);
            // FIN - Botones con estado de carga

        }

    };

    return (

        <div className="login-container">

            <div className="card login-card">

                {/* INICIO - Ajuste Responsive */}
                <div className="card-body p-3 p-sm-5">

                    <div className="text-center mb-4">

                        <h1 className="logo-title">
                            SICREC
                        </h1>

                        <p className="subtitle">
                            Sistema de Consulta de Revistas Científicas
                        </p>

                    </div>

                    <form onSubmit={iniciarSesion}>

                        <div className="mb-3">

                            <label className="form-label">
                                Usuario
                            </label>

                            <input
                                type="text"
                                className="form-control"
                                placeholder="Ingrese su usuario"
                                value={usuario}
                                onChange={(e) => setUsuario(e.target.value)}
                                required
                            />

                        </div>

                        {/* INICIO - Botón mostrar contraseña y validaciones visuales */}
                        <PasswordField
                            id="login-password"
                            label="Contraseña"
                            className="mb-4"
                            placeholder="Ingrese su contraseña"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                        {/* FIN - Botón mostrar contraseña y validaciones visuales */}

                        {/* INICIO - Botones con estado de carga */}
                        <LoadingButton
                            type="submit"
                            className="btn btn-primary w-100"
                            loading={cargando}
                            loadingText="Iniciar sesión..."
                        >
                            Iniciar Sesión
                        </LoadingButton>
                        {/* FIN - Botones con estado de carga */}

                        <div className="login-links">

                            {/* INICIO - Enlace a recuperación de contraseña */}
                            <Link to="/recuperar-password">
                                ¿Olvidaste tu contraseña?
                            </Link>
                            {/* FIN - Enlace a recuperación de contraseña */}

                            <a href="/crear-cuenta">
                                Crear cuenta
                            </a>

                        </div>

                    </form>

                </div>
                {/* FIN - Ajuste Responsive */}

            </div>

        </div>

    );

}

export default Login;
