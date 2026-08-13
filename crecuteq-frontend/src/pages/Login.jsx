import "../styles/Login.css";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import PasswordField from "../components/PasswordField";
import rectoradoImagen from "../assets/IMG_0693.png";
import investigacionImagen from "../assets/DEP INVESTIGACION.png";

function Login() {
    const [usuario, setUsuario] = useState("");
    const [password, setPassword] = useState("");
    const [cargando, setCargando] = useState(false);
    const navigate = useNavigate();
    const { notify } = useNotification();

    const iniciarSesion = async (e) => {
        e.preventDefault();
        setCargando(true);

        try {
            const respuesta = await axios.post("/api/auth/login", {
                usuario,
                password
            });
            const usuarioLogueado = respuesta.data;
            localStorage.setItem("usuario", JSON.stringify(usuarioLogueado));

            if (usuarioLogueado.debeCambiarPassword) {
                navigate(usuarioLogueado.rol === "ADMIN"
                    ? "/admin/cambiar-password"
                    : "/usuario/cambiar-password");
                return;
            }

            if (usuarioLogueado.rol === "ADMIN") {
                navigate("/admin/dashboard");
            } else if (usuarioLogueado.rol === "USUARIO") {
                navigate("/usuario/dashboard");
            } else {
                notify("Rol no reconocido.", "warning");
            }
        } catch (error) {
            console.error(error);
            const respuestaError = error.response?.data;
            const mensajeBackend = typeof respuestaError === "string"
                ? respuestaError
                : respuestaError?.message || respuestaError?.mensaje || respuestaError?.error;
            notify(mensajeBackend || "No fue posible iniciar sesión.", "danger");
        } finally {
            setCargando(false);
        }
    };

    return (
        <div className="login-container">
            <section
                className="login-showcase d-none d-lg-flex"
                aria-label="Presentación de CRECUTEQ"
                style={{ "--login-campus-image": `url(${rectoradoImagen})` }}
            >
                <div className="login-showcase__top">
                    <div className="login-showcase__brand">
                        <span aria-hidden="true">C</span>
                        <div><strong>CRECUTEQ</strong><small>Investigación UTEQ</small></div>
                    </div>
                    <span className="login-showcase__institution">Universidad Técnica Estatal de Quevedo</span>
                </div>

                <div className="login-showcase__content">
                    <span className="login-showcase__eyebrow">Conocimiento que transforma</span>
                    <h2>Explora la ciencia.<br/><em>Decide con evidencia.</em></h2>
                    <p>Consulta revistas científicas, cuartiles y métricas académicas desde una plataforma institucional segura.</p>
                    <div className="login-showcase__features">
                        <span>Búsqueda integrada</span>
                        <span>Fuentes verificadas</span>
                        <span>Información actualizada</span>
                    </div>
                </div>

                <div className="login-showcase__department">
                    <img src={investigacionImagen} alt="Edificio del Departamento de Investigación de la UTEQ" />
                    <div className="login-showcase__department-caption">
                        <small>Impulsado por</small>
                        <strong>Dirección de Investigación</strong>
                        <span>Universidad Técnica Estatal de Quevedo</span>
                    </div>
                </div>
            </section>

            <div className="card login-card">
                <div className="card-body">
                    <div className="login-card__heading mb-4">
                        <span className="login-card__eyebrow">Acceso institucional</span>
                        <h1 className="logo-title">CRECUTEQ</h1>
                        <p className="subtitle">Sistema de Consulta de Revistas Científicas</p>
                    </div>

                    <form onSubmit={iniciarSesion}>
                        <div className="mb-3">
                            <label className="form-label" htmlFor="login-usuario">Usuario</label>
                            <input
                                id="login-usuario"
                                type="text"
                                className="form-control"
                                placeholder="Ingrese su usuario"
                                value={usuario}
                                onChange={(e) => setUsuario(e.target.value)}
                                autoComplete="username"
                                required
                            />
                        </div>

                        <PasswordField
                            id="login-password"
                            label="Contraseña"
                            className="mb-4"
                            placeholder="Ingrese su contraseña"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            showValidation={false}
                            required
                        />

                        <LoadingButton
                            type="submit"
                            className="btn btn-primary w-100"
                            loading={cargando}
                            loadingText="Iniciando sesión..."
                        >
                            Iniciar sesión
                        </LoadingButton>

                        <div className="login-links">
                            <Link to="/recuperar-password">¿Olvidaste tu contraseña?</Link>
                            <Link to="/crear-cuenta">Crear cuenta</Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}

export default Login;
