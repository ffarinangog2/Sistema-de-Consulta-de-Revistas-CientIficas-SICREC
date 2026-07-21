import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { registrarUsuario } from "../services/usuarioService";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
// FIN - Mejoras UI/UX reutilizables

function CrearCuenta() {

    const navigate = useNavigate();

    const [nombreCompleto, setNombreCompleto] = useState("");
    const [correoInstitucional, setCorreoInstitucional] = useState("");
    const [tipoUsuario, setTipoUsuario] = useState("");
    // INICIO - Botones con estado de carga
    const [cargando, setCargando] = useState(false);
    // FIN - Botones con estado de carga
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const guardarUsuario = async (e) => {

        e.preventDefault();

        setCargando(true);

        try {

            await registrarUsuario({

                nombreCompleto,
                correoInstitucional,
                tipoUsuario

            });

            notify(
                "Cuenta creada correctamente. Revise su correo institucional para conocer su contraseña temporal.",
                "success"
            );

            navigate("/login");

        } catch (error) {

            console.error(error);

            notify(
                error.response?.data?.message ||
                "No fue posible crear la cuenta.",
                "danger"
            );

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
                            Crear Cuenta
                        </h1>

                        <p className="subtitle">
                            Sistema de Consulta de Revistas Científicas
                        </p>

                    </div>

                    <form onSubmit={guardarUsuario}>

                        <div className="mb-3">

                            <label className="form-label">
                                Nombre Completo
                            </label>

                            <input
                                type="text"
                                className="form-control"
                                placeholder="Ingrese su nombre completo"
                                value={nombreCompleto}
                                onChange={(e) =>
                                    setNombreCompleto(e.target.value)
                                }
                                required
                            />

                        </div>

                        <div className="mb-3">

                            <label className="form-label">
                                Correo Institucional
                            </label>

                            <input
                                type="email"
                                className="form-control"
                                placeholder="usuario@uteq.edu.ec"
                                value={correoInstitucional}
                                onChange={(e) =>
                                    setCorreoInstitucional(e.target.value)
                                }
                                required
                            />

                        </div>

                        <div className="mb-4">

                            <label className="form-label">
                                Tipo de Usuario
                            </label>

                            <select
                                className="form-select"
                                value={tipoUsuario}
                                onChange={(e) =>
                                    setTipoUsuario(e.target.value)
                                }
                                required
                            >

                                <option value="">
                                    Seleccione...
                                </option>

                                <option value="DOCENTE">
                                    Docente
                                </option>

                                <option value="ESTUDIANTE">
                                    Estudiante
                                </option>

                                <option value="PERSONAL_DE_INVESTIGACION">
                                    Personal de Investigación
                                </option>

                            </select>

                        </div>

                        {/* INICIO - Botones con estado de carga */}
                        <LoadingButton
                            type="submit"
                            className="btn btn-primary w-100"
                            loading={cargando}
                            loadingText="Creando cuenta..."
                        >
                            Crear Cuenta
                        </LoadingButton>
                        {/* FIN - Botones con estado de carga */}

                        <div className="login-links">

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

export default CrearCuenta;
