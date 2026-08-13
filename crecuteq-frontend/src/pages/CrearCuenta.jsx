import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { registrarUsuario } from "../services/usuarioService";
import { obtenerCargos } from "../services/cargoService";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import { googleScholarValido, orcidValido } from "../utils/perfilAcademicoValidation";
import investigacionImagen from "../assets/DEP INVESTIGACION.png";
// FIN - Mejoras UI/UX reutilizables

function CrearCuenta() {

    const navigate = useNavigate();

    const [nombreCompleto, setNombreCompleto] = useState("");
    const [correoInstitucional, setCorreoInstitucional] = useState("");
    const [cargoId, setCargoId] = useState("");
    const [cargos, setCargos] = useState([]);
    const [orcid, setOrcid] = useState("");
    const [googleScholar, setGoogleScholar] = useState("");
    // INICIO - Botones con estado de carga
    const [cargando, setCargando] = useState(false);
    // FIN - Botones con estado de carga
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    useEffect(() => {
        const cargarCargos = async () => {
            try {
                setCargos(await obtenerCargos());
            } catch (error) {
                console.error(error);
                notify("No fue posible cargar los cargos disponibles.", "danger");
            }
        };

        cargarCargos();
    }, [notify]);

    const guardarUsuario = async (e) => {

        e.preventDefault();

        if (!orcidValido(orcid)) {
            notify("Ingrese un ORCID válido, por ejemplo https://orcid.org/0000-0002-1825-0097.", "warning");
            return;
        }

        if (!googleScholarValido(googleScholar)) {
            notify("Ingrese una URL válida de Google Scholar que comience con https://scholar.google.com/.", "warning");
            return;
        }

        setCargando(true);

        try {

            await registrarUsuario({

                nombreCompleto,
                correoInstitucional,
                cargoId: Number(cargoId),
                orcid: orcid.trim(),
                googleScholar: googleScholar.trim()

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

        <div className="login-container auth-split">

            <aside className="auth-photo d-none d-lg-flex">
                <img src={investigacionImagen} alt="Edificio del Departamento de Investigación de la UTEQ" />
                <div className="auth-photo__overlay">
                    <span>Comunidad académica UTEQ</span>
                    <h2>Únete a la investigación que transforma.</h2>
                    <p>Crea tu cuenta institucional y accede a información científica para fortalecer tus proyectos.</p>
                </div>
            </aside>

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

                        <div className="mb-3">
                            <label className="form-label" htmlFor="registro-orcid">
                                ORCID <span className="form-text">(opcional)</span>
                            </label>
                            <input
                                id="registro-orcid"
                                type="url"
                                className={`form-control ${orcid && !orcidValido(orcid) ? "is-invalid" : ""}`}
                                placeholder="https://orcid.org/0000-0002-1825-0097"
                                value={orcid}
                                onChange={(e) => setOrcid(e.target.value)}
                            />
                            {orcid && !orcidValido(orcid) && (
                                <div className="invalid-feedback">Ingrese una URL ORCID válida.</div>
                            )}
                        </div>

                        <div className="mb-3">
                            <label className="form-label" htmlFor="registro-google-scholar">
                                Google Scholar <span className="form-text">(opcional)</span>
                            </label>
                            <input
                                id="registro-google-scholar"
                                type="url"
                                className={`form-control ${googleScholar && !googleScholarValido(googleScholar) ? "is-invalid" : ""}`}
                                placeholder="https://scholar.google.com/citations?user=..."
                                value={googleScholar}
                                onChange={(e) => setGoogleScholar(e.target.value)}
                            />
                            {googleScholar && !googleScholarValido(googleScholar) && (
                                <div className="invalid-feedback">Ingrese una URL válida de Google Scholar.</div>
                            )}
                        </div>

                        <div className="mb-4">

                            <label className="form-label">
                                Cargo
                            </label>

                            <select
                                className="form-select"
                                value={cargoId}
                                onChange={(e) =>
                                    setCargoId(e.target.value)
                                }
                                required
                            >

                                <option value="">
                                    Seleccione...
                                </option>

                                {cargos.map((cargo) => (
                                    <option key={cargo.id} value={cargo.id}>
                                        {cargo.nombreCargo}
                                    </option>
                                ))}

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
