import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import {
    buscarTodasRevistasCampo,
    buscarRevistas,
    eliminarFavorito,
    exportarRevistasExcel,
    guardarFavorito,
    obtenerCamposEstudioFacultad,
    obtenerDetalleComplementarioRevista,
    obtenerFacultades,
    obtenerFavoritos
} from "../services/revistaService";
import MainLayout from "../layouts/MainLayout";
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import { descargarArchivo } from "../utils/descargarArchivo";

const tieneValor = (valor) => (
    valor !== null
    && valor !== undefined
    && (typeof valor !== "string" || valor.trim() !== "")
);

// INICIO - Adaptación compatible al contrato genérico de proveedores
const obtenerProveedores = (revista) => {
    const proveedores = { ...(revista?.proveedores ?? {}) };
    if (revista?.doaj && !proveedores.doaj) proveedores.doaj = revista.doaj;
    if (revista?.springer && !proveedores.springer) proveedores.springer = revista.springer;
    return proveedores;
};

const nombreProveedor = (nombre) => {
    const conocidos = {
        scopus: "Scopus",
        scopus_excel: "Scopus (Excel)",
        doaj: "DOAJ",
        springer: "Springer",
        elsevier: "Elsevier",
        ieee: "IEEE",
        wiley: "Wiley",
        sage: "SAGE",
        cambridge: "Cambridge University Press",
        degruyter: "Walter de Gruyter",
        brill: "Brill",
        acm: "ACM"
    };
    return conocidos[String(nombre).toLowerCase()] ?? nombre;
};

const formatearApc = (info) => {
    const monedas = [
        ["apcUsd", "USD"], ["apcEur", "EUR"], ["apcGbp", "GBP"],
        ["apcAud", "AUD"], ["apcJpy", "JPY"]
    ].map(([clave, moneda]) => {
        const valor = info?.[clave] ?? valorAdicional(info, clave);
        return tieneValor(valor) ? `${valor} ${moneda}` : null;
    }).filter(Boolean);
    if (monedas.length > 0) return monedas.join(" / ");
    return [info?.apc, info?.monedaApc].filter(tieneValor).join(" ");
};

const etiquetaAdicional = (clave) => ({
    modeloPublicacion: "Modelo de publicación",
    tipoAcceso: "Tipo de acceso",
    periodicidad: "Periodicidad",
    periodicidadEstimada: "Periodicidad",
    areaTematica: "Área temática",
    licencias: "Licencias",
    notasApc: "Notas",
    vigencia: "Vigencia",
    cargoSobreextension: "Cargo por sobreextensión",
    tarifaLicenciaRepositorio: "Tarifa de licencia de repositorio",
    descuentoMiembroIEEE: "Descuento miembro IEEE",
    descuentoMiembroSociedadIEEE: "Descuento miembro de sociedad IEEE",
    openAccess: "Acceso abierto"
}[clave] ?? null);

const valorAdicional = (info, ...nombres) => {
    const datos = Object.entries(info?.datosAdicionales ?? {});
    for (const nombre of nombres) {
        const coincidencia = datos.find(([clave]) =>
            clave.trim().toLocaleLowerCase("es") === nombre.toLocaleLowerCase("es")
        );
        if (tieneValor(coincidencia?.[1])) return coincidencia[1];
    }
    return null;
};

const datosProveedor = (nombre, info) => {
    const proveedor = String(nombre).toLowerCase();
    const periodicidad = valorAdicional(
        info, "periodicidad", "periodicidadEstimada"
    ) ?? info?.periodicidadEstimada;

    if (proveedor === "scopus_excel") {
        return [
            ["Estado", valorAdicional(info, "estado")],
            ["Discontinuada", valorAdicional(info, "discontinuada")],
            ["Periodicidad", periodicidad]
        ].filter(([, valor]) => tieneValor(valor));
    }

    if (proveedor === "springer") {
        return [
            ["Periodicidad", periodicidad]
        ].filter(([, valor]) => tieneValor(valor));
    }

    if (proveedor === "doaj") {
        return [
            ["Periodicidad", periodicidad]
        ].filter(([, valor]) => tieneValor(valor));
    }

    if (proveedor === "elsevier") {
        return [
            ["Modelo de publicación", valorAdicional(info, "modeloPublicacion")],
            ["Vigencia", valorAdicional(info, "vigencia")]
        ].filter(([, valor]) => tieneValor(valor));
    }

    if (proveedor === "wiley") {
        return [
            ["Modelo de publicación", valorAdicional(info, "modeloPublicacion")],
            ["Licencias", valorAdicional(info, "licencias")],
            ["Vigencia", valorAdicional(info, "vigencia")]
        ].filter(([, valor]) => tieneValor(valor));
    }

    const clavesInternas = new Set([
        "fuenteApcSeleccionada", "prioridadApc", "apcDescartadoPorPrioridad"
    ]);
    const adicionales = Object.entries(info?.datosAdicionales ?? {})
        .filter(([clave, valor]) => !clavesInternas.has(clave)
            && !clave.toLowerCase().startsWith("apc") && tieneValor(valor))
        .map(([clave, valor]) => [etiquetaAdicional(clave), valor])
        .filter(([etiqueta]) => tieneValor(etiqueta));
    if (tieneValor(periodicidad)
        && !adicionales.some(([etiqueta]) => etiqueta === "Periodicidad")) {
        adicionales.push(["Periodicidad", periodicidad]);
    }
    return adicionales.filter(([etiqueta, valor], indice, lista) =>
        lista.findIndex(([otra]) => otra === etiqueta) === indice && tieneValor(valor));
};
// FIN - Adaptación compatible al contrato genérico de proveedores

// INICIO - Modal de detalle de revista
function ModalRevista({ revista, onClose, onAlternarFavorito, esFavorita, guardando, cargandoDetalle, errorDetalle, colorCuartil }) {
    if (!revista) return null;

    const proveedores = obtenerProveedores(revista);
    const proveedoresGenericos = Object.entries(proveedores)
        .filter(([nombre, info]) => datosProveedor(nombre, info).length > 0);
    const areas = Array.isArray(revista.scopus?.subjectAreas)
        ? revista.scopus.subjectAreas.map((area) => area?.nombre).filter(Boolean).join(", ")
        : revista.scopus?.areas ?? revista.scopus?.categorias ?? revista.camposEstudio ?? revista.campoEstudio;
    const cobertura = revista.scopus?.cobertura
        ?? ([revista.scopus?.coverageStartYear, revista.scopus?.coverageEndYear].filter(Boolean).join(" - ") || revista.cobertura);
    const tipo = revista.scopus?.tipoFuente ?? revista.tipoFuente;
    const tipoTraducido = String(tipo ?? "").toLowerCase() === "journal" ? "Revista científica" : tipo;
    const sourceId = String(revista.sourceId ?? "").trim();
    const enlaceScopus = sourceId
        ? `https://www.scopus.com/sourceid/${encodeURIComponent(sourceId)}`
        : revista.scopus?.enlaceScopus;
    const fuenteApc = Object.entries(proveedores).find(([, info]) =>
        tieneValor(valorAdicional(info, "fuenteApcSeleccionada")))
        ?? Object.entries(proveedores).find(([, info]) => tieneValor(formatearApc(info)));
    const apcGeneral = fuenteApc ? formatearApc(fuenteApc[1]) : null;

    const informacionGeneral = [
        ["Nombre", revista.titulo],
        ["Editorial", revista.scopus?.publisher ?? revista.publisher],
        ["Cobertura", cobertura],
        ["ISSN", revista.issn],
        ["eISSN", revista.eIssn],
        ["País", revista.pais],
        ["Área temática", areas],
        ["Tipo", tipoTraducido],
        [fuenteApc ? `APC (${nombreProveedor(fuenteApc[0])})` : "APC", apcGeneral]
    ].filter(([, valor]) => tieneValor(valor));

    const metricas = [
        ["Cuartil", revista.cuartil],
        ["CiteScore", revista.scopus?.citeScore, revista.scopus?.citeScoreYear],
        ["SJR", revista.scopus?.sjr, revista.scopus?.sjrYear],
        ["SNIP", revista.scopus?.snip, revista.scopus?.snipYear],
        ["Percentil", revista.scopus?.percentile],
        ["Mejor percentil", revista.scopus?.bestPercentile]
    ].filter(([, valor]) => tieneValor(valor));

    return (
        <>
            <div
                className="modal d-block"
                role="dialog"
                aria-modal="true"
                aria-labelledby="detalle-revista-titulo"
                tabIndex="-1"
                onClick={onClose}
            >
                <div className="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" onClick={(evento) => evento.stopPropagation()}>
                    <div className="modal-content border-0 shadow">
                        <div className="modal-header align-items-start gap-3">
                            <div className="min-w-0">
                                <span className={`badge ${colorCuartil(revista.cuartil)} mb-2`}>
                                    {revista.cuartil ?? "Sin cuartil"}
                                </span>
                                <h4 className="modal-title fw-bold text-break" id="detalle-revista-titulo">
                                    {revista.titulo ?? "Revista sin título"}
                                </h4>
                            </div>
                            <LoadingButton
                                type="button"
                                className={`btn ${esFavorita ? "btn-danger shadow" : "btn-outline-secondary"} rounded-circle ms-auto flex-shrink-0`}
                                style={{ width: "44px", height: "44px" }}
                                onClick={() => onAlternarFavorito(revista)}
                                loading={guardando}
                                loadingText=""
                                disabled={guardando}
                                aria-label={esFavorita ? "Quitar revista de favoritos" : "Guardar revista en favoritos"}
                                aria-pressed={esFavorita}
                                title={esFavorita ? "Quitar de favoritos" : "Guardar en favoritos"}
                            >
                                <span aria-hidden="true" className={`${esFavorita ? "fs-4 text-white" : "fs-5"}`}>{esFavorita ? "♥" : "♡"}</span>
                            </LoadingButton>
                        </div>

                        <div className="modal-body p-3 p-md-4">
                            <section aria-labelledby="informacion-general-titulo">
                                <h6 className="text-uppercase text-muted fw-bold mb-3" id="informacion-general-titulo">Información general</h6>
                                <div className="row g-3">
                                    {informacionGeneral.map(([etiqueta, valor]) => (
                                        <div className="col-12 col-sm-6" key={etiqueta}>
                                            <div className="card h-100 shadow-none">
                                                <div className="card-body p-3">
                                                    <span className="d-block small text-muted mb-1">{etiqueta}</span>
                                                    <span className="fw-semibold text-break">{valor}</span>
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </section>

                            {cargandoDetalle && (
                                <div className="d-flex align-items-center gap-2 mt-4" role="status" aria-live="polite">
                                    <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" />
                                    <span className="small text-muted">Cargando métricas y datos complementarios...</span>
                                </div>
                            )}
                            {errorDetalle && !cargandoDetalle && (
                                <div className="alert alert-warning mt-4 mb-0" role="alert">{errorDetalle}</div>
                            )}

                            {/* INICIO - Información acotada por proveedor */}
                            {proveedoresGenericos.map(([nombre, info]) => (
                                <section className="mt-4" aria-labelledby={`informacion-${nombre}-titulo`} key={nombre}>
                                    <h6 className="text-uppercase text-muted fw-bold mb-3" id={`informacion-${nombre}-titulo`}>
                                        Información {nombreProveedor(nombre)}
                                    </h6>
                                    <div className="row g-3">
                                        {datosProveedor(nombre, info).map(([etiqueta, valor]) => (
                                            <div className="col-12 col-sm-6 col-lg-4" key={etiqueta}>
                                                <div className="card h-100 shadow-none">
                                                    <div className="card-body p-3">
                                                        <span className="d-block small text-muted mb-1">{etiqueta}</span>
                                                        <span className="fw-semibold text-break">{valor}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </section>
                            ))}
                            {/* FIN - Información acotada por proveedor */}

                            {metricas.length > 0 && (
                                <section className="mt-4" aria-labelledby="metricas-titulo">
                                    <h6 className="text-uppercase text-muted fw-bold mb-3" id="metricas-titulo">Métricas</h6>
                                    <div className="row row-cols-2 row-cols-md-4 g-3">
                                        {metricas.map(([etiqueta, valor, anio]) => (
                                            <div className="col" key={etiqueta}>
                                                <div className="card h-100 text-center shadow-none">
                                                    <div className="card-body p-3">
                                                        <span className="d-block small text-muted">{etiqueta}</span>
                                                        <strong className="d-block fs-5 mt-1">{valor}</strong>
                                                        {tieneValor(anio) && <span className="small text-muted">{anio}</span>}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </section>
                            )}
                        </div>

                        <div className="modal-footer gap-2">
                            {enlaceScopus
                                ? <a href={enlaceScopus} target="_blank" rel="noopener noreferrer" className="btn btn-primary">Abrir en Scopus</a>
                                : <button type="button" className="btn btn-primary" disabled title="Scopus no proporcionó un enlace">Abrir en Scopus</button>}
                            <button type="button" className="btn btn-outline-secondary ms-sm-auto" onClick={onClose}>Cerrar</button>
                        </div>
                    </div>
                </div>
            </div>
            <div className="modal-backdrop show" />
        </>
    );
}
// FIN - Modal de detalle de revista

function BuscarRevistas() {
    // Estados y lógica original de búsqueda.
    const [termino, setTermino] = useState("");
    const [cantidad, setCantidad] = useState(25);
    const [revistas, setRevistas] = useState([]);
    const [favoritos, setFavoritos] = useState([]);
    const [cargando, setCargando] = useState(false);
    const [guardandoId, setGuardandoId] = useState(null);
    const [exportandoExcel, setExportandoExcel] = useState(false);
    const [busquedaRealizada, setBusquedaRealizada] = useState(false);
    const [revistaSeleccionada, setRevistaSeleccionada] = useState(null);
    const [cargandoDetalle, setCargandoDetalle] = useState(false);
    const [errorDetalle, setErrorDetalle] = useState("");
    // INICIO - Paginación
    const [paginaActual, setPaginaActual] = useState(1);
    const resultadosPorPagina = 5;
    const resultadosPorPaginaCampo = 25;
    // FIN - Paginación
    const location = useLocation();
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    const { notify } = useNotification();

    // INICIO - Filtros de búsqueda
    const [filtroCuartil, setFiltroCuartil] = useState("Todos");
    const [filtroOpenAccess, setFiltroOpenAccess] = useState("Todos");
    const [filtroEstado, setFiltroEstado] = useState("Todas");
    const [filtroDiscontinuada, setFiltroDiscontinuada] = useState("Todas");
    const [filtroPais, setFiltroPais] = useState("Todos");
    const [facultad, setFacultad] = useState("");
    const [facultades, setFacultades] = useState([]);
    const [campoEstudio, setCampoEstudio] = useState("");
    const [camposFacultad, setCamposFacultad] = useState([]);
    const [cargandoFacultades, setCargandoFacultades] = useState(true);
    const [cargandoCampos, setCargandoCampos] = useState(false);

    const textoNormalizado = (valor) => {
        if (Array.isArray(valor)) return valor.join(" ").toLowerCase();
        return String(valor ?? "").toLowerCase();
    };

    // INICIO - Datos independientes por fuente
    const tieneScopus = (revista) => revista.scopus != null;
    const fuentesDeRevista = (revista) => [
        ...(tieneScopus(revista) ? ["scopus"] : []),
        ...Object.keys(obtenerProveedores(revista))
    ];
    // INICIO - Campo de estudio
    const camposScopus = (revista) => {
        const subjectAreas = revista.scopus?.subjectAreas;
        if (Array.isArray(subjectAreas) && subjectAreas.length > 0) {
            return subjectAreas.map((area) => area?.nombre).filter(Boolean);
        }
        return revista.scopus?.areas ?? revista.scopus?.categorias ?? revista.camposEstudio ?? revista.campoEstudio;
    };
    // FIN - Campo de estudio
    const publisherScopus = (revista) => revista.scopus?.publisher ?? revista.publisher;
    const accesoAbiertoScopus = (revista) => revista.scopus?.accesoAbierto ?? revista.openAccess;

    const fuentesVisibles = (revista) => {
        return fuentesDeRevista(revista);
    };

    const valorAccesoAbierto = (revista, fuente) => {
        if (fuente === "scopus") return accesoAbiertoScopus(revista);
        if (fuente === "doaj") return true;
        const info = obtenerProveedores(revista)[fuente];
        return info?.datosAdicionales?.openAccess ?? info?.datosAdicionales?.accesoAbierto;
    };

    const esAccesoAbierto = (valor) => valor === true || ["sí", "si", "yes", "open access", "open"].includes(textoNormalizado(valor));
    // FIN - Datos independientes por fuente

    const coincideOpenAccess = (revista) => {
        if (filtroOpenAccess === "Todos") return true;
        const coincidencias = fuentesVisibles(revista).map((fuente) => esAccesoAbierto(valorAccesoAbierto(revista, fuente)));
        return filtroOpenAccess === "Con acceso"
            ? coincidencias.some(Boolean)
            : coincidencias.some((coincide) => !coincide);
    };

    const datosScopusExcel = (revista) => obtenerProveedores(revista).scopus_excel;

    const coincideEstado = (revista) => {
        if (filtroEstado === "Todas") return true;
        const estado = textoNormalizado(valorAdicional(datosScopusExcel(revista), "estado"));
        return filtroEstado === "Activa"
            ? ["active", "activo", "activa"].includes(estado)
            : ["inactive", "inactivo", "inactiva"].includes(estado);
    };

    const coincideDiscontinuada = (revista) => {
        if (filtroDiscontinuada === "Todas") return true;
        const valor = textoNormalizado(valorAdicional(
            datosScopusExcel(revista), "discontinuada"
        ));
        const esDiscontinuada = ["sí", "si", "yes", "true", "1"].includes(valor);
        return filtroDiscontinuada === "Sí" ? esDiscontinuada : valor !== "" && !esDiscontinuada;
    };

    const paisRevista = (revista) => String(
        revista.pais
        ?? revista.scimago?.pais
        ?? revista.doaj?.pais
        ?? obtenerProveedores(revista).doaj?.pais
        ?? ""
    ).trim();

    const paisesDisponibles = [...new Set(
        revistas.map(paisRevista).filter(Boolean)
    )].sort((a, b) => a.localeCompare(b, "es", { sensitivity: "base" }));

    const coincidePais = (revista) => filtroPais === "Todos"
        || paisRevista(revista).localeCompare(
            filtroPais, "es", { sensitivity: "base" }
        ) === 0;
    // FIN - Filtros de búsqueda

    const normalizarIdentificador = (valor) => String(valor ?? "").replace(/[^a-zA-Z0-9]/g, "").toLowerCase();

    const obtenerFavoritoDeRevista = (revista) => favoritos.find((favorito) => {
        const sourceIdRevista = normalizarIdentificador(revista.sourceId ?? revista.scopus?.sourceId);
        const sourceIdFavorito = normalizarIdentificador(favorito.sourceId);
        if (sourceIdRevista && sourceIdFavorito && sourceIdRevista === sourceIdFavorito) return true;

        const issnRevista = normalizarIdentificador(revista.issn ?? revista.eIssn);
        const issnFavorito = normalizarIdentificador(favorito.issn);
        if (issnRevista && issnFavorito && issnRevista === issnFavorito) return true;

        return Boolean(revista.titulo && favorito.titulo)
            && revista.titulo.trim().toLowerCase() === favorito.titulo.trim().toLowerCase();
    });

    const alternarFavorito = async (revista) => {
        const favoritoExistente = obtenerFavoritoDeRevista(revista);
        const identificadorRevista = revista.sourceId || revista.issn || revista.titulo;

        try {
            setGuardandoId(identificadorRevista);

            if (!usuario || !usuario.id) {
                console.error("alternarFavorito(): no se pudo obtener un usuario válido desde localStorage.", usuario);
                notify("No se pudo identificar tu usuario. Vuelve a iniciar sesión e intenta de nuevo.", "danger");
                return;
            }

            if (favoritoExistente) {
                await eliminarFavorito(favoritoExistente.id);
                setFavoritos((actuales) => actuales.filter((favorito) => favorito.id !== favoritoExistente.id));
                notify("Revista eliminada de favoritos.", "success");
                return;
            }

            const favorito = {
                usuarioId: usuario.id,
                sourceId: revista.sourceId,
                issn: revista.issn,
                titulo: revista.titulo,
                revista: revista.revista,
                cuartil: revista.cuartil,
                anio: revista.fecha ? parseInt(revista.fecha.substring(0, 4)) : null
            };

            const resultado = await guardarFavorito(favorito);
            if (!resultado || !resultado.id) {
                console.error("alternarFavorito(): la respuesta del backend no parece válida.", { favorito, resultado });
                notify("No se pudo confirmar que la revista se guardó correctamente. Intenta nuevamente.", "danger");
                return;
            }
            setFavoritos((actuales) => [...actuales, resultado]);
            notify("Revista agregada a favoritos.", "success");
        } catch (error) {
            console.error("alternarFavorito(): error al actualizar favorito.", error);
            notify("No se pudo actualizar el favorito.", "danger");
        } finally {
            setGuardandoId(null);
        }
    };

    const exportarResultadosExcel = async () => {
        if (revistasFiltradas.length === 0) return;
        setExportandoExcel(true);
        try {
            const archivo = await exportarRevistasExcel(revistasFiltradas);
            descargarArchivo(archivo, "Revistas_filtradas.xlsx");
            notify(`Se exportaron ${revistasFiltradas.length} revistas.`, "success");
        } catch (error) {
            console.error("No se pudo exportar el Excel de revistas.", error);
            notify("No se pudo exportar el Excel de revistas.", "danger");
        } finally {
            setExportandoExcel(false);
        }
    };

    const colorCuartil = (cuartil) => {
        switch (cuartil) {
            case "Q1": return "bg-success";
            case "Q2": return "bg-primary";
            case "Q3": return "bg-warning text-dark";
            case "Q4": return "bg-danger";
            default: return "bg-secondary";
        }
    };

    const buscar = async (terminoBusqueda = termino) => {
        if (!terminoBusqueda.trim()) return;
        setCargando(true);
        setBusquedaRealizada(true);

        try {
            // INICIO - Corrección cantidad de resultados
            const cantidadSolicitada = Number(cantidad);
            const data = await buscarRevistas(
                terminoBusqueda,
                cantidadSolicitada,
                usuario.id,
                facultad,
                campoEstudio
            );
            setRevistas(Array.isArray(data) ? data.slice(0, cantidadSolicitada) : []);
            // FIN - Corrección cantidad de resultados
            // INICIO - Paginación
            setPaginaActual(1);
            // FIN - Paginación
        } catch (error) {
            console.error(error);
            notify("Error al consultar Scopus.", "danger");
        } finally {
            setCargando(false);
        }
    };

    useEffect(() => {
        if (location.state?.termino) setTermino(location.state.termino);
    }, [location.state]);

    useEffect(() => {
        if (!usuario?.id) return;

        obtenerFavoritos(usuario.id)
            .then((data) => setFavoritos(Array.isArray(data) ? data : []))
            .catch((error) => console.error("No se pudieron cargar los favoritos.", error));
    }, [usuario?.id]);

    useEffect(() => {
        let activo = true;
        setCargandoFacultades(true);
        obtenerFacultades()
            .then((data) => {
                if (activo) setFacultades(Array.isArray(data) ? data : []);
            })
            .catch((error) => {
                if (activo) {
                    console.error("No se pudieron cargar las facultades.", error);
                    notify("No se pudieron cargar las facultades.", "danger");
                }
            })
            .finally(() => {
                if (activo) setCargandoFacultades(false);
            });
        return () => { activo = false; };
    }, [notify]);

    useEffect(() => {
        let activo = true;
        setCampoEstudio("");
        setCamposFacultad([]);
        setRevistas([]);
        setBusquedaRealizada(false);
        setPaginaActual(1);

        if (!facultad) {
            setCargandoCampos(false);
            return () => { activo = false; };
        }

        setCargandoCampos(true);
        obtenerCamposEstudioFacultad(facultad)
            .then((data) => {
                if (activo) setCamposFacultad(Array.isArray(data) ? data : []);
            })
            .catch((error) => {
                if (activo) {
                    console.error("No se pudieron cargar los campos ASJC.", error);
                    notify("No se pudieron cargar los campos de estudio de la facultad.", "danger");
                }
            })
            .finally(() => {
                if (activo) setCargandoCampos(false);
            });

        return () => { activo = false; };
    }, [facultad, notify]);

    useEffect(() => {
        const controller = new AbortController();

        if (!facultad || !campoEstudio || !usuario?.id) {
            setCargando(false);
            return () => controller.abort();
        }

        setRevistas([]);
        setCargando(true);
        setBusquedaRealizada(true);
        buscarTodasRevistasCampo(
            facultad,
            campoEstudio,
            usuario.id,
            controller.signal
        )
            .then((data) => {
                if (controller.signal.aborted) return;
                setRevistas(Array.isArray(data) ? data : []);
                setPaginaActual(1);
            })
            .catch((error) => {
                if (controller.signal.aborted || error?.code === "ERR_CANCELED") return;
                console.error("No se pudieron cargar las revistas del campo ASJC.", error);
                notify("No se pudieron cargar las revistas del campo de estudio.", "danger");
            })
            .finally(() => {
                if (!controller.signal.aborted) setCargando(false);
            });

        return () => controller.abort();
    }, [campoEstudio, facultad, usuario?.id, notify]);

    useEffect(() => {
        const terminoNavegacion = location.state?.termino;
        if (!terminoNavegacion?.trim() || !usuario?.id) return;

        let activo = true;
        setCargando(true);
        setBusquedaRealizada(true);
        buscarRevistas(
            terminoNavegacion,
            Number(cantidad),
            usuario.id,
            facultad,
            campoEstudio
        )
            .then((data) => {
                if (!activo) return;
                setRevistas(Array.isArray(data) ? data.slice(0, Number(cantidad)) : []);
                setPaginaActual(1);
            })
            .catch((error) => {
                if (!activo) return;
                console.error(error);
                notify("Error al consultar Scopus.", "danger");
            })
            .finally(() => {
                if (activo) setCargando(false);
            });

        return () => { activo = false; };
    }, [location.state, usuario?.id, cantidad, facultad, campoEstudio, notify]);

    useEffect(() => {
        if (!revistaSeleccionada) return undefined;

        const cerrarConEscape = (evento) => {
            if (evento.key === "Escape") setRevistaSeleccionada(null);
        };
        document.addEventListener("keydown", cerrarConEscape);
        return () => document.removeEventListener("keydown", cerrarConEscape);
    }, [revistaSeleccionada]);

    const detalleSourceId = revistaSeleccionada?.sourceId;
    const detalleIssn = revistaSeleccionada?.issn;
    const detalleEIssn = revistaSeleccionada?.eIssn;
    const detalleEditorial = revistaSeleccionada?.scopus?.publisher;

    useEffect(() => {
        const controller = new AbortController();
        const claveSolicitada = detalleSourceId || detalleIssn || detalleEIssn;
        if (!claveSolicitada || !facultad || !campoEstudio || !usuario?.id) {
            setCargandoDetalle(false);
            setErrorDetalle("");
            return () => controller.abort();
        }

        setCargandoDetalle(true);
        setErrorDetalle("");
        obtenerDetalleComplementarioRevista(
            {
                sourceId: detalleSourceId,
                issn: detalleIssn,
                eIssn: detalleEIssn,
                scopus: { publisher: detalleEditorial }
            },
            usuario.id,
            controller.signal
        ).then((detalle) => {
            if (controller.signal.aborted) return;
            setRevistaSeleccionada((actual) => {
                const claveActual = actual?.sourceId || actual?.issn || actual?.eIssn;
                if (!actual || claveActual !== claveSolicitada) return actual;
                return {
                    ...actual,
                    cuartil: detalle?.cuartil ?? actual.cuartil,
                    pais: detalle?.pais ?? actual.pais,
                    origenCuartil: detalle?.origenCuartil ?? actual.origenCuartil,
                    encontradaEnScimago: detalle?.encontradaEnScimago
                        ?? actual.encontradaEnScimago,
                    scopus: {
                        ...(actual.scopus ?? {}),
                        ...(detalle?.scopus ?? {}),
                        enlaceScopus: detalle?.scopus?.enlaceScopus
                            ?? actual.scopus?.enlaceScopus
                    },
                    scimago: detalle?.scimago ?? actual.scimago,
                    proveedores: detalle?.proveedores ?? actual.proveedores,
                    doaj: detalle?.doaj ?? actual.doaj,
                    springer: detalle?.springer ?? actual.springer
                };
            });
        }).catch((error) => {
            if (controller.signal.aborted || error?.code === "ERR_CANCELED") return;
            console.error("No se pudo cargar el detalle complementario.", error);
            setErrorDetalle("No se pudieron cargar las métricas y datos complementarios.");
        }).finally(() => {
            if (!controller.signal.aborted) setCargandoDetalle(false);
        });

        return () => controller.abort();
    }, [campoEstudio, detalleEIssn, detalleEditorial, detalleIssn,
        detalleSourceId, facultad, usuario?.id]);

    // INICIO - Filtros de búsqueda
    const revistasFiltradas = revistas.filter((revista) => {
        // INICIO - Filtros respetando la fuente seleccionada
        const fuentes = fuentesVisibles(revista);
        const pasaCuartil = filtroCuartil === "Todos"
            || (filtroCuartil === "Sin cuartil"
                ? !revista.cuartil
                : revista.cuartil === filtroCuartil);
        const pasaOpenAccess = coincideOpenAccess(revista);
        const pasaEstado = coincideEstado(revista);
        const pasaDiscontinuada = coincideDiscontinuada(revista);
        const pasaPais = coincidePais(revista);
        const pasaFuente = fuentes.length > 0;
        const identificadorLocal = normalizarIdentificador(termino);
        const esConsultaIssn = /^\d{4}-?\d{3}[\dXx]$/.test(termino.trim());
        const pasaTermino = !esConsultaIssn
            || normalizarIdentificador(revista.issn) === identificadorLocal
            || normalizarIdentificador(revista.eIssn) === identificadorLocal;
        return pasaCuartil && pasaOpenAccess && pasaEstado
            && pasaDiscontinuada && pasaPais && pasaFuente && pasaTermino;
        // FIN - Filtros respetando la fuente seleccionada
    });

    // INICIO - Paginación
    const esPaginacionCampo = Boolean(facultad && campoEstudio);
    const resultadosPorPaginaActual = esPaginacionCampo
        ? resultadosPorPaginaCampo
        : resultadosPorPagina;
    const indicePrimerResultado = (paginaActual - 1) * resultadosPorPaginaActual;
    const indiceUltimoResultado = paginaActual * resultadosPorPaginaActual;
    const revistasPaginadas = revistasFiltradas.slice(
        indicePrimerResultado, indiceUltimoResultado
    );
    const totalPaginas = Math.ceil(
        revistasFiltradas.length / resultadosPorPaginaActual
    );

    useEffect(() => {
        setPaginaActual(1);
    }, [termino, filtroCuartil, filtroOpenAccess, filtroEstado,
        filtroDiscontinuada, filtroPais]);
    // FIN - Paginación
    // FIN - Filtros de búsqueda

    // INICIO - Mejora visual Buscar Revistas
    return (
        <MainLayout admin={window.location.pathname.startsWith("/admin/")}>
            <div className="container-fluid px-3 px-md-4 py-4" style={{ maxWidth: "1320px" }}>
                <header className="mb-4">
                    <span className="text-primary text-uppercase fw-semibold small">Explorar fuentes científicas</span>
                    <h2 className="fw-bold mt-1 mb-2">Búsqueda de revistas</h2>
                    <p className="text-muted">Consulta y compara revistas en Scopus, SCImago y catálogos editoriales especializados.</p>
                </header>

                <section className="card border-0 shadow-sm mb-4" aria-label="Formulario de búsqueda">
                    <div className="card-body p-3 p-md-4">
                        {/* INICIO - Filtros de búsqueda */}
                        <div className="mb-3">
                            <h5 className="fw-bold mb-0">Encuentra una revista</h5>
                            <p className="text-muted small mt-1">Busca por el título de la revista o por su código ISSN.</p>
                        </div>

                        {/* INICIO - Barra de búsqueda principal */}
                        <div className="row g-2 align-items-end">
                            <div className="col-12 col-lg-3">
                                <label htmlFor="facultad-uteq" className="form-label fw-semibold">Facultad UTEQ</label>
                                <select id="facultad-uteq" className="form-select form-select-lg" value={facultad} onChange={(e) => {
                                    setCampoEstudio("");
                                    setPaginaActual(1);
                                    setFacultad(e.target.value);
                                }} disabled={cargando || cargandoFacultades}>
                                    <option value="">{cargandoFacultades ? "Cargando facultades..." : "Todas las facultades"}</option>
                                    {facultades.map((item) => <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
                                </select>
                                {cargandoFacultades && <span className="spinner-border spinner-border-sm text-primary mt-2" role="status"><span className="visually-hidden">Cargando facultades...</span></span>}
                            </div>
                            <div className="col-12 col-lg-3">
                                <label htmlFor="campo-estudio-asjc" className="form-label fw-semibold">Campo de estudio</label>
                                <select id="campo-estudio-asjc" className="form-select form-select-lg" value={campoEstudio} onChange={(e) => {
                                    setPaginaActual(1);
                                    setCampoEstudio(e.target.value);
                                }} disabled={!facultad || cargando || cargandoCampos}>
                                    <option value="">{cargandoCampos ? "Cargando campos..." : "Todos los campos"}</option>
                                    {camposFacultad.map((campo) => <option key={campo.codigo} value={campo.codigo}>{campo.nombre} ({campo.codigo})</option>)}
                                </select>
                                {cargandoCampos && <span className="spinner-border spinner-border-sm text-primary mt-2" role="status"><span className="visually-hidden">Cargando campos de estudio...</span></span>}
                            </div>
                            <div className="col-12 col-lg-4">
                                <label htmlFor="termino-revista" className="form-label fw-semibold">Título, ISSN o eISSN</label>
                                <input id="termino-revista" className="form-control form-control-lg" placeholder="Ej.: inteligencia artificial o 0000-0000" value={termino} onChange={(e) => setTermino(e.target.value)} onKeyDown={(e) => e.key === "Enter" && !facultad && !cargando && buscar()} disabled={cargando} />
                            </div>
                            {!facultad && <div className="col-12 col-lg-2 d-flex align-items-end">
                                <LoadingButton className="btn btn-primary btn-lg w-100" onClick={() => buscar()} loading={cargando} loadingText="Buscando..." disabled={cargando || !termino.trim()}>
                                    Buscar
                                </LoadingButton>
                            </div>}
                        </div>
                        {/* FIN - Barra de búsqueda principal */}

                        {/* INICIO - Filtros compactos */}
                        <div className="border-top mt-3 pt-3">
                            <p className="small text-muted fw-semibold mb-2">Filtros de resultados</p>
                            <div className="row g-2 align-items-end">
                                <div className="col-6 col-md-3"><label htmlFor="cuartil" className="form-label small fw-semibold">Cuartil</label><select id="cuartil" className="form-select" value={filtroCuartil} onChange={(e) => setFiltroCuartil(e.target.value)}>{["Todos", "Q1", "Q2", "Q3", "Q4", "Sin cuartil"].map((q) => <option key={q}>{q}</option>)}</select></div>
                                <div className="col-6 col-md-3"><label htmlFor="acceso-abierto" className="form-label small fw-semibold">Acceso abierto</label><select id="acceso-abierto" className="form-select" value={filtroOpenAccess} onChange={(e) => setFiltroOpenAccess(e.target.value)}><option>Todos</option><option>Con acceso</option><option>Sin acceso</option></select></div>
                                <div className="col-6 col-md-3"><label htmlFor="estado-scopus" className="form-label small fw-semibold">Estado</label><select id="estado-scopus" className="form-select" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)}><option>Todas</option><option>Activa</option><option>Inactiva</option></select></div>
                                <div className="col-6 col-md-3"><label htmlFor="discontinuada-scopus" className="form-label small fw-semibold">Discontinuada</label><select id="discontinuada-scopus" className="form-select" value={filtroDiscontinuada} onChange={(e) => setFiltroDiscontinuada(e.target.value)}><option>Todas</option><option>Sí</option><option>No</option></select></div>
                                <div className="col-6 col-md-3">
                                    <label htmlFor="pais-revista" className="form-label small fw-semibold">País</label>
                                    <select id="pais-revista" className="form-select" value={filtroPais} onChange={(e) => setFiltroPais(e.target.value)}>
                                        <option value="Todos">Todos los países</option>
                                        {paisesDisponibles.map((pais) => <option key={pais} value={pais}>{pais}</option>)}
                                    </select>
                                </div>
                                {!facultad && <div className="col-12 col-md-3">
                                    <label htmlFor="cantidad-resultados" className="form-label small fw-semibold">Cantidad de resultados</label>
                                    <select id="cantidad-resultados" className="form-select" value={cantidad} onChange={(e) => {
                                        setCantidad(Number(e.target.value));
                                        setPaginaActual(1);
                                    }} disabled={cargando}>
                                        <option value={10}>10</option><option value={25}>25</option><option value={50}>50</option>
                                    </select>
                                </div>}
                            </div>
                        </div>
                        {/* FIN - Filtros compactos */}
                        {/* FIN - Filtros de búsqueda */}
                    </div>
                </section>

                {cargando && (
                    /* INICIO - Consulta de ambas fuentes */
                    <section className="card border-0 shadow-sm text-center py-5" aria-live="polite">
                        <div className="card-body">
                            <div className="spinner-grow text-primary" role="status"><span className="visually-hidden">Consultando fuentes de revistas...</span></div>
                            <h5 className="fw-semibold mt-3 mb-1">Consultando revistas</h5>
                            <p className="text-muted">Estamos obteniendo la información desde las fuentes disponibles.</p>
                        </div>
                    </section>
                    /* FIN - Consulta de ambas fuentes */
                )}

                {!cargando && busquedaRealizada && revistas.length === 0 && (
                    <section className="card border-0 shadow-sm text-center py-5" role="status">
                        <div className="card-body"><div className="display-6 mb-3" aria-hidden="true">⌕</div><h5 className="fw-bold">No encontramos revistas</h5><p className="text-muted">Prueba con otro título, ISSN o una búsqueda más general.</p></div>
                    </section>
                )}

                {!cargando && revistas.length > 0 && (
                    <>
                        <div className="d-flex flex-column flex-sm-row justify-content-between gap-2 align-items-sm-center mb-3">
                            <div>
                                <h5 className="fw-bold mb-0">Resultados</h5>
                                <span className="text-muted small">
                                    <strong>{revistasFiltradas.length}</strong> {revistasFiltradas.length === 1 ? "revista coincide" : "revistas coinciden"} con los filtros entre los <strong>{revistas.length}</strong> resultados encontrados
                                </span>
                            </div>
                            <LoadingButton
                                className="btn btn-success btn-sm"
                                onClick={exportarResultadosExcel}
                                loading={exportandoExcel}
                                loadingText="Exportando..."
                                disabled={revistasFiltradas.length === 0}
                            >
                                Exportar Excel ({revistasFiltradas.length})
                            </LoadingButton>
                        </div>

                        {revistasFiltradas.length === 0 ? (
                            <div className="card border-0 shadow-sm text-center py-4"><div className="card-body"><h6 className="fw-bold">No hay coincidencias con estos filtros</h6><p className="text-muted small">Ajusta o elimina alguno de los filtros para ver más resultados.</p></div></div>
                        ) : (
                            <>
                            {/* INICIO - Resultados comparativos por fuente */}
                            <div className="row row-cols-1 g-4">
                                {revistasPaginadas.map((revista, index) => {
                                    const clave = revista.sourceId || revista.issn || `${revista.titulo}-${index}`;
                                    const areasScopus = Array.isArray(camposScopus(revista))
                                        ? camposScopus(revista)
                                        : String(camposScopus(revista) ?? "").split(/[,;]/).filter(Boolean);
                                    const coberturaScopus = revista.scopus?.cobertura
                                        ?? ([revista.scopus?.coverageStartYear, revista.scopus?.coverageEndYear].filter(Boolean).join(" - ") || revista.cobertura);
                                    const areasVisibles = areasScopus;
                                    const fuentesResultado = fuentesDeRevista(revista).map(nombreProveedor);
                                    const favorita = Boolean(obtenerFavoritoDeRevista(revista));
                                    const procesandoFavorito = guardandoId === (revista.sourceId || revista.issn || revista.titulo);

                                    return (
                                        <div className="col" key={clave}>
                                            <article
                                                className="card h-100 border-0 shadow-sm"
                                                role="button"
                                                tabIndex={0}
                                                aria-label={`Ver detalles de ${revista.titulo ?? "la revista"}`}
                                                style={{ cursor: "pointer" }}
                                                onClick={() => setRevistaSeleccionada(revista)}
                                                onKeyDown={(evento) => {
                                                    if (evento.key === "Enter" || evento.key === " ") {
                                                        evento.preventDefault();
                                                        setRevistaSeleccionada(revista);
                                                    }
                                                }}
                                            >
                                                <div className="card-body p-3 p-md-4 position-relative">
                                                    <LoadingButton
                                                        type="button"
                                                        className={`btn ${favorita ? "btn-danger shadow" : "btn-outline-secondary"} rounded-circle position-absolute top-0 end-0 m-3`}
                                                        style={{ width: "44px", height: "44px" }}
                                                        onClick={(evento) => {
                                                            evento.stopPropagation();
                                                            alternarFavorito(revista);
                                                        }}
                                                        onKeyDown={(evento) => evento.stopPropagation()}
                                                        loading={procesandoFavorito}
                                                        loadingText=""
                                                        disabled={guardandoId !== null}
                                                        aria-label={favorita ? "Quitar revista de favoritos" : "Guardar revista en favoritos"}
                                                        aria-pressed={favorita}
                                                        title={favorita ? "Quitar de favoritos" : "Guardar en favoritos"}
                                                    >
                                                        <span aria-hidden="true" className={`${favorita ? "fs-4 text-white" : "fs-5"}`}>{favorita ? "♥" : "♡"}</span>
                                                    </LoadingButton>
                                                    <div className="d-flex flex-wrap align-items-start gap-2 mb-3 pe-5">
                                                        <span className={`badge ${colorCuartil(revista.cuartil)}`}>
                                                            {revista.cuartil ?? "Sin cuartil"}
                                                        </span>
                                                        {fuentesResultado.map((fuente) => (
                                                            <span className="badge bg-primary" key={fuente}>Fuente {fuente}</span>
                                                        ))}
                                                    </div>
                                                    <h5 className="fw-bold mb-3 text-break">{revista.titulo ?? "Revista sin título"}</h5>
                                                    <div className="row g-3 small">
                                                        <div className="col-12 col-md-6">
                                                            <span className="d-block text-muted">Editorial</span>
                                                            <span className="fw-semibold text-break">{publisherScopus(revista) ?? "No disponible"}</span>
                                                        </div>
                                                        <div className="col-12 col-md-6">
                                                            <span className="d-block text-muted">Cobertura</span>
                                                            <span className="fw-semibold text-break">{coberturaScopus || "No disponible"}</span>
                                                        </div>
                                                        {revista.pais && (
                                                            <div className="col-12 col-md-6">
                                                                <span className="d-block text-muted">País</span>
                                                                <span className="fw-semibold text-break">{revista.pais}</span>
                                                            </div>
                                                        )}
                                                        <div className="col-12">
                                                            <span className="d-block text-muted">Área temática</span>
                                                            <div className="d-flex flex-wrap gap-1 mt-1">
                                                                {areasVisibles.length > 0
                                                                    ? areasVisibles.slice(0, 3).map((area) => <span className="badge rounded-pill bg-primary-subtle text-primary-emphasis text-wrap" key={area}>{String(area).trim()}</span>)
                                                                    : <span className="fw-semibold">No disponible</span>}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </article>
                                        </div>
                                    );
                                })}
                            </div>
                            {/* FIN - Resultados comparativos por fuente */}

                            {/* INICIO - Paginación */}
                            {totalPaginas > 1 && (
                                <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mt-3">
                                    <span>
                                        Mostrando {indicePrimerResultado + 1} - {Math.min(indiceUltimoResultado, revistasFiltradas.length)} de {revistasFiltradas.length} revistas
                                    </span>
                                    <div className="d-flex flex-wrap align-items-center gap-2">
                                        <button className="btn btn-outline-primary btn-sm" disabled={paginaActual === 1} onClick={() => setPaginaActual(paginaActual - 1)}>
                                            Anterior
                                        </button>
                                        <span>Página {paginaActual} de {totalPaginas || 1}</span>
                                        <button className="btn btn-outline-primary btn-sm" disabled={paginaActual === totalPaginas || totalPaginas === 0} onClick={() => setPaginaActual(paginaActual + 1)}>
                                            Siguiente
                                        </button>
                                    </div>
                                </div>
                            )}
                            {/* FIN - Paginación */}
                            </>
                        )}
                    </>
                )}

                <ModalRevista
                    revista={revistaSeleccionada}
                    onClose={() => setRevistaSeleccionada(null)}
                    onAlternarFavorito={alternarFavorito}
                    esFavorita={Boolean(revistaSeleccionada && obtenerFavoritoDeRevista(revistaSeleccionada))}
                    guardando={guardandoId === (revistaSeleccionada?.sourceId || revistaSeleccionada?.issn || revistaSeleccionada?.titulo)}
                    cargandoDetalle={cargandoDetalle}
                    errorDetalle={errorDetalle}
                    colorCuartil={colorCuartil}
                />
            </div>
        </MainLayout>
    );
    // FIN - Mejora visual Buscar Revistas
}

export default BuscarRevistas;
