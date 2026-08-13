import { useCallback, useEffect, useState } from "react";
import LoadingButton from "./LoadingButton";
import { useNotification } from "../hooks/useNotification";
import {
    actualizarPerfilAcademico,
    crearPerfilAcademico,
    obtenerPerfilAcademico
} from "../services/perfilAcademicoService";
import { GOOGLE_SCHOLAR_PATTERN, ORCID_PATTERN } from "../utils/perfilAcademicoValidation";

// INICIO - Perfil Académico
const perfilInicial = {
    id: null,
    googleScholar: "",
    orcid: "",
    tieneGoogleScholar: false,
    tieneOrcid: false
};

const obtenerMensajeError = (error, mensajePredeterminado) => {
    const data = error.response?.data;
    if (typeof data === "string") return data;
    if (data?.detail) return data.detail;
    if (data?.message) return data.message;
    if (data && typeof data === "object") {
        const mensajes = Object.values(data).filter((valor) => typeof valor === "string");
        if (mensajes.length > 0) return mensajes.join(" ");
    }
    return mensajePredeterminado;
};

function PerfilAcademicoSection({ onPerfilCargado }) {
    const { notify } = useNotification();
    const [perfilAcademico, setPerfilAcademico] = useState(perfilInicial);
    const [googleScholar, setGoogleScholar] = useState("");
    const [orcid, setOrcid] = useState("");
    const [cargando, setCargando] = useState(true);
    const [guardando, setGuardando] = useState(false);
    const [expandido, setExpandido] = useState(false);

    const aplicarPerfil = useCallback((perfil) => {
        const datos = { ...perfilInicial, ...perfil };
        setPerfilAcademico(datos);
        setGoogleScholar(datos.googleScholar || "");
        setOrcid(datos.orcid || "");
        onPerfilCargado?.(datos);
    }, [onPerfilCargado]);

    useEffect(() => {
        const cargarPerfilAcademico = async () => {
            try {
                aplicarPerfil(await obtenerPerfilAcademico());
            } catch (error) {
                console.error(error);
                notify(obtenerMensajeError(
                    error,
                    "No fue posible cargar el Perfil Académico."
                ), "danger");
            } finally {
                setCargando(false);
            }
        };

        cargarPerfilAcademico();
    }, [aplicarPerfil, notify]);

    const guardar = async (evento) => {
        evento.preventDefault();
        setGuardando(true);

        try {
            const datos = {
                googleScholar: googleScholar.trim(),
                orcid: orcid.trim()
            };
            const resultado = perfilAcademico.id
                ? await actualizarPerfilAcademico(datos)
                : await crearPerfilAcademico(datos);

            aplicarPerfil(resultado);
            notify(
                perfilAcademico.id
                    ? "Perfil Académico actualizado correctamente."
                    : "Perfil Académico guardado correctamente.",
                "success"
            );
        } catch (error) {
            console.error(error);
            notify(obtenerMensajeError(
                error,
                "No fue posible guardar el Perfil Académico."
            ), "danger");
        } finally {
            setGuardando(false);
        }
    };

    return (
        <section className="card shadow-sm border-0 mt-4" aria-labelledby="titulo-perfil-academico">
            <button
                type="button"
                className="perfil-academico__toggle"
                aria-expanded={expandido}
                aria-controls="contenido-perfil-academico"
                onClick={() => setExpandido((valor) => !valor)}
            >
                <h5 id="titulo-perfil-academico" className="mb-0">Perfil Académico</h5>
                <span className={`perfil-academico__flecha ${expandido ? "perfil-academico__flecha--abierta" : ""}`} aria-hidden="true">⌄</span>
            </button>

            {expandido && <div id="contenido-perfil-academico" className="card-body p-3 p-md-4 pt-0">
                <p className="text-muted small mb-3">Administre sus identificadores y perfiles científicos.</p>

                {cargando ? (
                    <div className="text-center py-4" role="status">
                        <div className="spinner-border text-primary" aria-hidden="true" />
                        <p className="text-muted small mt-2 mb-0">Cargando Perfil Académico...</p>
                    </div>
                ) : (
                    <form onSubmit={guardar}>
                        <div className="row g-3">
                            <div className="col-12">
                                <label className="form-label" htmlFor="google-scholar">Google Scholar</label>
                                <input
                                    id="google-scholar"
                                    type="url"
                                    className="form-control"
                                    placeholder="https://scholar.google.com/..."
                                    pattern={GOOGLE_SCHOLAR_PATTERN.source}
                                    title="Ingrese una URL con el formato https://scholar.google.com/..."
                                    value={googleScholar}
                                    onChange={(evento) => setGoogleScholar(evento.target.value)}
                                    disabled={guardando}
                                />
                            </div>

                            <div className="col-12">
                                <label className="form-label" htmlFor="orcid">ORCID</label>
                                <input
                                    id="orcid"
                                    type="url"
                                    className="form-control"
                                    placeholder="https://orcid.org/0000-0000-0000-0000"
                                    pattern={ORCID_PATTERN.source}
                                    title="Ingrese una URL con el formato https://orcid.org/0000-0000-0000-0000"
                                    value={orcid}
                                    onChange={(evento) => setOrcid(evento.target.value)}
                                    disabled={guardando}
                                />
                            </div>
                        </div>

                        <div className="d-flex flex-column flex-sm-row flex-wrap gap-2 mt-3">
                            <LoadingButton
                                type="submit"
                                className="btn btn-primary"
                                loading={guardando}
                                loadingText="Guardando..."
                            >
                                {perfilAcademico.id
                                    ? "Actualizar Perfil Académico"
                                    : "Guardar Perfil Académico"}
                            </LoadingButton>

                            {perfilAcademico.tieneGoogleScholar && perfilAcademico.googleScholar && (
                                <a
                                    className="btn btn-outline-primary"
                                    href={perfilAcademico.googleScholar}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    🎓 Ver Google Scholar
                                </a>
                            )}

                            {perfilAcademico.tieneOrcid && perfilAcademico.orcid && (
                                <a
                                    className="btn btn-outline-success"
                                    href={perfilAcademico.orcid}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    🟢 Ver ORCID
                                </a>
                            )}
                        </div>
                    </form>
                )}
            </div>}
        </section>
    );
}

export default PerfilAcademicoSection;
// FIN - Perfil Académico
