import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { buscarRevistas, guardarFavorito } from "../services/revistaService";
import MainLayout from "../layouts/MainLayout";
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";

// INICIO - Campo de estudio con búsqueda
function SelectorCampoEstudio({ opciones, valor, onChange }) {
    const [consulta, setConsulta] = useState(valor);
    const [abierto, setAbierto] = useState(false);
    const contenedorRef = useRef(null);

    useEffect(() => {
        setConsulta(valor);
    }, [valor]);

    useEffect(() => {
        const cerrarAlHacerClicFuera = (evento) => {
            if (!contenedorRef.current?.contains(evento.target)) setAbierto(false);
        };

        document.addEventListener("mousedown", cerrarAlHacerClicFuera);
        return () => document.removeEventListener("mousedown", cerrarAlHacerClicFuera);
    }, []);

    const opcionesFiltradas = opciones.filter((opcion) =>
        opcion.toLocaleLowerCase("es").includes(consulta.toLocaleLowerCase("es"))
    );

    const seleccionar = (opcion) => {
        onChange(opcion);
        setConsulta(opcion);
        setAbierto(false);
    };

    return (
        <div className="position-relative" ref={contenedorRef}>
            <input
                id="campo-estudio"
                className="form-control"
                type="search"
                role="combobox"
                aria-autocomplete="list"
                aria-controls="opciones-campo-estudio"
                aria-expanded={abierto}
                autoComplete="off"
                value={consulta}
                placeholder="Buscar un campo de estudio"
                onFocus={() => setAbierto(true)}
                onChange={(evento) => {
                    setConsulta(evento.target.value);
                    setAbierto(true);
                    if (!evento.target.value) onChange("");
                }}
                onKeyDown={(evento) => {
                    if (evento.key === "Escape") {
                        setConsulta(valor);
                        setAbierto(false);
                    }
                }}
                onBlur={() => setConsulta(valor)}
            />
            {abierto && (
                <div id="opciones-campo-estudio" className="list-group position-absolute top-100 start-0 end-0 mt-1 shadow-sm overflow-auto z-3" role="listbox" style={{ maxHeight: "240px" }}>
                    <button type="button" className={`list-group-item list-group-item-action ${valor === "" ? "active" : ""}`} onMouseDown={(evento) => evento.preventDefault()} onClick={() => seleccionar("")}>
                        Todos los campos
                    </button>
                    {opcionesFiltradas.map((opcion) => (
                        <button key={opcion} type="button" role="option" aria-selected={valor === opcion} className={`list-group-item list-group-item-action ${valor === opcion ? "active" : ""}`} onMouseDown={(evento) => evento.preventDefault()} onClick={() => seleccionar(opcion)}>
                            {opcion}
                        </button>
                    ))}
                    {opcionesFiltradas.length === 0 && <span className="list-group-item text-muted small">No hay campos coincidentes</span>}
                </div>
            )}
        </div>
    );
}
// FIN - Campo de estudio con búsqueda

// INICIO - Secciones Scopus y SCImago
function SeccionFuente({ nombre, principal = false, disponible, mensajeNoEncontrada, children }) {
    return (
        <section className={`h-100 rounded-3 border p-3 ${principal ? "border-primary-subtle bg-primary-subtle bg-opacity-10" : "bg-light"}`} aria-label={`Datos de ${nombre}`}>
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <span className={`badge ${principal ? "bg-primary" : "bg-dark"}`}>{nombre}</span>
                {principal && <span className="badge bg-primary">Fuente principal</span>}
            </div>

            {disponible ? children : (
                <div className="text-muted small py-3" role="status">
                    <span className="d-block fw-semibold">{mensajeNoEncontrada}</span>
                    <span>No hay información disponible de esta fuente.</span>
                </div>
            )}
        </section>
    );
}
// FIN - Secciones Scopus y SCImago

// INICIO - Información expandible
function DetalleTecnico({ revista, mostrarScopus, mostrarScimago, tieneScopus, tieneScimago }) {
    const Campo = ({ etiqueta, valor, ancho = "col-6 col-md-4" }) => (
        <div className={ancho}>
            <span className="d-block small text-muted">{etiqueta}</span>
            <span className="fw-semibold text-break">{valor ?? "-"}</span>
        </div>
    );

    return (
        <div className="border-top mt-3 pt-3">
            <h6 className="fw-bold mb-3">Métricas e información técnica</h6>

            {mostrarScopus && tieneScopus(revista) && (
                <div className="mb-3">
                    <span className="badge bg-light text-dark border mb-3">Scopus</span>
                    <div className="row g-3">
                        <Campo etiqueta="ISSN" valor={revista.issn} />
                        <Campo etiqueta="eISSN" valor={revista.eIssn} />
                        <Campo etiqueta="ID de fuente" valor={revista.sourceId ?? revista.scopus?.sourceId} />
                        <Campo etiqueta="CiteScore" valor={revista.scopus?.citeScore ? `${revista.scopus.citeScore}${revista.scopus.citeScoreYear ? ` (${revista.scopus.citeScoreYear})` : ""}` : null} />
                        <Campo etiqueta="SJR" valor={revista.scopus?.sjr ? `${revista.scopus.sjr}${revista.scopus.sjrYear ? ` (${revista.scopus.sjrYear})` : ""}` : null} />
                        <Campo etiqueta="SNIP" valor={revista.scopus?.snip ? `${revista.scopus.snip}${revista.scopus.snipYear ? ` (${revista.scopus.snipYear})` : ""}` : null} />
                        <Campo etiqueta="Percentil" valor={revista.scopus?.percentile} />
                        <Campo etiqueta="Mejor percentil" valor={revista.scopus?.bestPercentile} />
                        <Campo etiqueta="Mejor cuartil" valor={revista.scopus?.bestQuartile} />
                        <Campo etiqueta="País" valor={revista.scopus?.pais} />
                        <Campo etiqueta="Tipo de fuente" valor={(revista.scopus?.tipoFuente ?? revista.tipoFuente)?.toLowerCase() === "journal" ? "Revista científica" : revista.scopus?.tipoFuente ?? revista.tipoFuente} />
                        <Campo etiqueta="Tipo de acceso abierto" valor={revista.scopus?.tipoOpenAccess} />
                        <Campo etiqueta="Inicio de cobertura" valor={revista.scopus?.coverageStartYear} />
                        <Campo etiqueta="Fin de cobertura" valor={revista.scopus?.coverageEndYear} />
                    </div>
                </div>
            )}

            {mostrarScimago && tieneScimago(revista) && (
                <div>
                    <span className="badge bg-light text-dark border mb-3">SCImago</span>
                    <div className="row g-3">
                        <Campo etiqueta="ISSN" valor={revista.scimago?.issn} />
                        <Campo etiqueta="País" valor={revista.scimago?.pais} />
                        <Campo etiqueta="Región" valor={revista.scimago?.region} />
                        <Campo etiqueta="SJR" valor={revista.scimago?.sjr} />
                        <Campo etiqueta="Índice H" valor={revista.scimago?.hIndex} />
                        <Campo etiqueta="Ranking" valor={revista.scimago?.rank} />
                        <Campo etiqueta="Categorías" valor={revista.scimago?.categorias} ancho="col-12" />
                        <Campo etiqueta="Áreas" valor={revista.scimago?.areas} ancho="col-12" />
                    </div>
                </div>
            )}
        </div>
    );
}
// FIN - Información expandible

function BuscarRevistas() {
    // Estados y lógica original de búsqueda.
    const [termino, setTermino] = useState("");
    const [cantidad, setCantidad] = useState(25);
    const [revistas, setRevistas] = useState([]);
    const [cargando, setCargando] = useState(false);
    const [guardandoId, setGuardandoId] = useState(null);
    const [busquedaRealizada, setBusquedaRealizada] = useState(false);
    // INICIO - Paginación
    const [paginaActual, setPaginaActual] = useState(1);
    const resultadosPorPagina = 5;
    // FIN - Paginación
    const location = useLocation();
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    const { notify } = useNotification();

    // INICIO - Filtros de búsqueda
    const [filtroCampo, setFiltroCampo] = useState("");
    const [filtroCuartil, setFiltroCuartil] = useState("Todos");
    const [filtroOpenAccess, setFiltroOpenAccess] = useState("Todos");
    const [filtroFuente, setFiltroFuente] = useState("Todas");

    const textoNormalizado = (valor) => {
        if (Array.isArray(valor)) return valor.join(" ").toLowerCase();
        return String(valor ?? "").toLowerCase();
    };

    // INICIO - Datos independientes por fuente
    const tieneScopus = (revista) => revista.scopus != null;
    const tieneScimago = (revista) => revista.scimago != null;
    // INICIO - Campo de estudio
    const camposScopus = (revista) => {
        const subjectAreas = revista.scopus?.subjectAreas;
        if (Array.isArray(subjectAreas) && subjectAreas.length > 0) {
            return subjectAreas.map((area) => area?.nombre).filter(Boolean);
        }
        return revista.scopus?.areas ?? revista.scopus?.categorias ?? revista.camposEstudio ?? revista.campoEstudio;
    };
    // FIN - Campo de estudio
    const camposScimago = (revista) => revista.scimago?.areas ?? revista.scimago?.categorias;
    const publisherScopus = (revista) => revista.scopus?.publisher ?? revista.publisher;
    const publisherScimago = (revista) => revista.scimago?.publisher;
    const tipoFuenteScopus = (revista) => revista.scopus?.tipoFuente ?? revista.tipoFuente;
    const accesoAbiertoScopus = (revista) => revista.scopus?.accesoAbierto ?? revista.openAccess;
    const accesoAbiertoScimago = (revista) => revista.scimago?.openAccess;

    const fuentesVisibles = (revista) => {
        if (filtroFuente === "Scopus") return tieneScopus(revista) ? ["Scopus"] : [];
        if (filtroFuente === "SCImago") return tieneScimago(revista) ? ["SCImago"] : [];
        return [
            ...(tieneScopus(revista) ? ["Scopus"] : []),
            ...(tieneScimago(revista) ? ["SCImago"] : [])
        ];
    };

    const valorAccesoAbierto = (revista, fuente) => fuente === "Scopus"
        ? accesoAbiertoScopus(revista)
        : accesoAbiertoScimago(revista);

    const esAccesoAbierto = (valor) => valor === true || ["sí", "si", "yes", "open access", "open"].includes(textoNormalizado(valor));
    // FIN - Datos independientes por fuente

    const coincideOpenAccess = (revista) => {
        if (filtroOpenAccess === "Todos") return true;
        const coincidencias = fuentesVisibles(revista).map((fuente) => esAccesoAbierto(valorAccesoAbierto(revista, fuente)));
        return filtroOpenAccess === "Con acceso"
            ? coincidencias.some(Boolean)
            : coincidencias.some((coincide) => !coincide);
    };
    // FIN - Filtros de búsqueda

    // INICIO - Información expandible
    const [detallesAbiertos, setDetallesAbiertos] = useState({});

    const alternarDetalle = (clave) => {
        setDetallesAbiertos((actuales) => ({ ...actuales, [clave]: !actuales[clave] }));
    };
    // FIN - Información expandible

    const guardar = async (revista) => {
        try {
            setGuardandoId(revista.sourceId || revista.issn || revista.titulo);

            if (!usuario || !usuario.id) {
                console.error("guardar(): no se pudo obtener un usuario válido desde localStorage.", usuario);
                notify("No se pudo identificar tu usuario. Vuelve a iniciar sesión e intenta de nuevo.", "danger");
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
                console.error("guardar(): la respuesta del backend no parece válida.", { favorito, resultado });
                notify("No se pudo confirmar que la revista se guardó correctamente. Intenta nuevamente.", "danger");
                return;
            }
            notify("Revista agregada a favoritos.", "success");
        } catch (error) {
            console.error("guardar(): error al guardar favorito.", error);
            notify("No se pudo guardar.", "danger");
        } finally {
            setGuardandoId(null);
        }
    };

    const verEnScopus = (e, enlace) => {
        e.preventDefault();
        if (!enlace) return;
        const url = /^https?:\/\//i.test(enlace) ? enlace : `https://${enlace}`;
        window.open(url, "_blank", "noopener,noreferrer");
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
            const data = await buscarRevistas(terminoBusqueda, cantidadSolicitada, usuario.id);
            setRevistas(Array.isArray(data) ? data.slice(0, cantidadSolicitada) : []);
            // FIN - Corrección cantidad de resultados
            setDetallesAbiertos({});
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
        if (location.state?.termino) buscar(location.state.termino);
    }, [location.state]);

    // INICIO - Filtros de búsqueda
    const revistasFiltradas = revistas.filter((revista) => {
        // INICIO - Filtros respetando la fuente seleccionada
        const fuentes = fuentesVisibles(revista);
        const cuartiles = fuentes.map((fuente) => fuente === "Scopus" ? revista.scopus?.cuartil : revista.scimago?.cuartil);
        const campos = fuentes.map((fuente) => fuente === "Scopus" ? camposScopus(revista) : camposScimago(revista));
        const pasaCuartil = filtroCuartil === "Todos"
            || (filtroCuartil === "Sin cuartil" ? cuartiles.every((cuartil) => !cuartil) : cuartiles.includes(filtroCuartil));
        const pasaCampo = !filtroCampo || campos.some((campo) => textoNormalizado(campo).includes(textoNormalizado(filtroCampo)));
        const pasaOpenAccess = coincideOpenAccess(revista);
        const pasaFuente = fuentes.length > 0;
        return pasaCuartil && pasaCampo && pasaOpenAccess && pasaFuente;
        // FIN - Filtros respetando la fuente seleccionada
    });

    // INICIO - Campo de estudio con búsqueda
    const camposEstudioDisponibles = [...new Set(revistas.flatMap((revista) => {
        const campos = [camposScopus(revista), camposScimago(revista)];
        return campos.flatMap((campo) => Array.isArray(campo) ? campo : String(campo ?? "").split(/[,;]/));
    }).map((campo) => String(campo).trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b, "es"));
    // FIN - Campo de estudio con búsqueda

    // INICIO - Paginación
    const indiceUltimoResultado = paginaActual * resultadosPorPagina;
    const indicePrimerResultado = indiceUltimoResultado - resultadosPorPagina;
    const revistasPaginadas = revistasFiltradas.slice(indicePrimerResultado, indiceUltimoResultado);
    const totalPaginas = Math.ceil(revistasFiltradas.length / resultadosPorPagina);

    useEffect(() => {
        setPaginaActual(1);
    }, [filtroCampo, filtroCuartil, filtroOpenAccess, filtroFuente]);
    // FIN - Paginación
    // FIN - Filtros de búsqueda

    // INICIO - Mejora visual Buscar Revistas
    return (
        <MainLayout admin={false}>
            <div className="container-fluid px-3 px-md-4 py-4" style={{ maxWidth: "1320px" }}>
                <header className="mb-4">
                    <span className="text-primary text-uppercase fw-semibold small">Explorar fuentes científicas</span>
                    <h2 className="fw-bold mt-1 mb-2">Búsqueda de revistas</h2>
                    <p className="text-muted">Consulta y compara revistas indexadas en Scopus y SCImago.</p>
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
                            <div className="col-12 col-lg-10">
                                <label htmlFor="termino-revista" className="form-label fw-semibold">Título o ISSN</label>
                                <input id="termino-revista" className="form-control form-control-lg" placeholder="Ej.: inteligencia artificial o 0000-0000" value={termino} onChange={(e) => setTermino(e.target.value)} onKeyDown={(e) => e.key === "Enter" && !cargando && buscar()} disabled={cargando} />
                            </div>
                            <div className="col-12 col-lg-2 d-flex align-items-end">
                                <LoadingButton className="btn btn-primary btn-lg w-100" onClick={() => buscar()} loading={cargando} loadingText="Buscando..." disabled={cargando || !termino.trim()}>
                                    Buscar
                                </LoadingButton>
                            </div>
                        </div>
                        {/* FIN - Barra de búsqueda principal */}

                        {/* INICIO - Filtros compactos */}
                        <div className="border-top mt-3 pt-3">
                            <p className="small text-muted fw-semibold mb-2">Filtros de resultados</p>
                            <div className="row g-2 align-items-end">
                                {/* INICIO - Campo de estudio con búsqueda */}
                                <div className="col-12 col-lg-4"><label htmlFor="campo-estudio" className="form-label small fw-semibold">Campo de estudio</label><SelectorCampoEstudio opciones={camposEstudioDisponibles} valor={filtroCampo} onChange={setFiltroCampo} /></div>
                                {/* FIN - Campo de estudio con búsqueda */}
                                <div className="col-6 col-md-3 col-lg-2"><label htmlFor="cuartil" className="form-label small fw-semibold">Cuartil</label><select id="cuartil" className="form-select" value={filtroCuartil} onChange={(e) => setFiltroCuartil(e.target.value)}>{["Todos", "Q1", "Q2", "Q3", "Q4", "Sin cuartil"].map((q) => <option key={q}>{q}</option>)}</select></div>
                                <div className="col-6 col-md-3 col-lg-2"><label htmlFor="acceso-abierto" className="form-label small fw-semibold">Acceso abierto</label><select id="acceso-abierto" className="form-select" value={filtroOpenAccess} onChange={(e) => setFiltroOpenAccess(e.target.value)}><option>Todos</option><option>Con acceso</option><option>Sin acceso</option></select></div>
                                <div className="col-6 col-md-3 col-lg-2"><label htmlFor="fuente-datos" className="form-label small fw-semibold">Fuente de datos</label><select id="fuente-datos" className="form-select" value={filtroFuente} onChange={(e) => setFiltroFuente(e.target.value)}><option>Todas</option><option>Scopus</option><option>SCImago</option></select></div>
                                <div className="col-6 col-md-3 col-lg-2">
                                    <label htmlFor="cantidad-resultados" className="form-label small fw-semibold">Cantidad de resultados</label>
                                    <select id="cantidad-resultados" className="form-select" value={cantidad} onChange={(e) => {
                                        setCantidad(Number(e.target.value));
                                        setPaginaActual(1);
                                    }} disabled={cargando}>
                                        <option value={10}>10</option><option value={25}>25</option><option value={50}>50</option>
                                    </select>
                                </div>
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
                            <div className="spinner-grow text-primary" role="status"><span className="visually-hidden">Consultando Scopus y SCImago...</span></div>
                            <h5 className="fw-semibold mt-3 mb-1">Consultando revistas</h5>
                            <p className="text-muted">Estamos obteniendo la información desde Scopus y SCImago.</p>
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
                            <h5 className="fw-bold mb-0">Resultados</h5>
                            <span className="text-muted small">Mostrando <strong>{revistasFiltradas.length}</strong> de <strong>{revistas.length}</strong> revistas</span>
                        </div>

                        {revistasFiltradas.length === 0 ? (
                            <div className="card border-0 shadow-sm text-center py-4"><div className="card-body"><h6 className="fw-bold">No hay coincidencias con estos filtros</h6><p className="text-muted small">Ajusta o elimina alguno de los filtros para ver más resultados.</p></div></div>
                        ) : (
                            <>
                            {/* INICIO - Resultados comparativos por fuente */}
                            <div className="row row-cols-1 g-4">
                                {revistasPaginadas.map((revista, index) => {
                                    const clave = revista.sourceId || revista.issn || `${revista.titulo}-${index}`;
                                    const mostrarScopus = filtroFuente !== "SCImago";
                                    const mostrarScimago = filtroFuente !== "Scopus";
                                    const abiertaScopus = esAccesoAbierto(accesoAbiertoScopus(revista));
                                    const abiertaScimago = esAccesoAbierto(accesoAbiertoScimago(revista));
                                    // INICIO - Mejora visual Buscar Revistas
                                    const areasScopus = Array.isArray(camposScopus(revista))
                                        ? camposScopus(revista)
                                        : String(camposScopus(revista) ?? "").split(/[,;]/).filter(Boolean);
                                    const areasScimago = textoNormalizado(camposScimago(revista))
                                        ? String(camposScimago(revista)).split(/[,;]/).filter(Boolean)
                                        : [];
                                    const tipoFuente = mostrarScopus && tieneScopus(revista)
                                        ? tipoFuenteScopus(revista)
                                        : revista.scimago?.tipoFuente;
                                    const tipoFuenteTraducido = textoNormalizado(tipoFuente) === "journal"
                                        ? "Revista científica"
                                        : tipoFuente ?? "Revista científica";
                                    const subtitulo = revista.revista && revista.revista !== revista.titulo
                                        ? revista.revista
                                        : tipoFuenteTraducido;
                                    const coberturaScopus = revista.scopus?.cobertura
                                        ?? ([revista.scopus?.coverageStartYear, revista.scopus?.coverageEndYear].filter(Boolean).join(" - ") || revista.cobertura);
                                    // FIN - Mejora visual Buscar Revistas

                                    return (
                                        <div className="col" key={clave}>
                                            <article className="card h-100 border-0 shadow-sm">
                                                <div className="card-body p-3 p-md-4 d-flex flex-column">
                                                    {/* INICIO - Mejora visual Buscar Revistas */}
                                                    <div className="border-bottom pb-3 mb-3">
                                                        <h5 className="fw-bold mb-1 text-break">{revista.titulo ?? "Revista sin título"}</h5>
                                                        <p className="text-muted small">{subtitulo}</p>
                                                    </div>
                                                    {/* FIN - Mejora visual Buscar Revistas */}

                                                    {/* INICIO - Secciones Scopus y SCImago */}
                                                    <div className="row g-3 mb-3">
                                                        {mostrarScopus && (
                                                            <div className={mostrarScimago ? "col-12 col-lg-6" : "col-12"}>
                                                                <SeccionFuente nombre="Scopus" principal disponible={tieneScopus(revista)} mensajeNoEncontrada="No encontrada en Scopus">
                                                                    <div className="d-flex flex-wrap gap-2 mb-3">
                                                                        <span className={`badge ${colorCuartil(revista.scopus?.cuartil)}`}>{revista.scopus?.cuartil ?? "Sin cuartil"}</span>
                                                                        <span className={`badge rounded-pill ${abiertaScopus ? "bg-success-subtle text-success-emphasis" : "bg-secondary-subtle text-secondary-emphasis"}`}>{abiertaScopus ? "Acceso abierto" : "Acceso por suscripción"}</span>
                                                                    </div>
                                                                    <div className="row g-3 small">
                                                                        <div className="col-12"><span className="d-block text-muted">Campos de estudio</span><div className="d-flex flex-wrap gap-1 mt-1">{areasScopus.length > 0 ? areasScopus.slice(0, 3).map((area) => <span className="badge rounded-pill bg-primary-subtle text-primary-emphasis text-wrap" key={area}>{String(area).trim()}</span>) : <span className="fw-semibold">No disponible</span>}</div></div>
                                                                        <div className="col-12 col-sm-6"><span className="d-block text-muted">Editorial</span><span className="fw-semibold text-break">{publisherScopus(revista) ?? "No disponible"}</span></div>
                                                                        <div className="col-12 col-sm-6"><span className="d-block text-muted">Cobertura</span><span className="fw-semibold text-break">{coberturaScopus || "No disponible"}</span></div>
                                                                    </div>
                                                                </SeccionFuente>
                                                            </div>
                                                        )}

                                                        {mostrarScimago && (
                                                            <div className={mostrarScopus ? "col-12 col-lg-6" : "col-12"}>
                                                                <SeccionFuente nombre="SCImago" disponible={tieneScimago(revista)} mensajeNoEncontrada="No encontrada en SCImago">
                                                                    <div className="d-flex flex-wrap gap-2 mb-3">
                                                                        <span className={`badge ${colorCuartil(revista.scimago?.cuartil)}`}>{revista.scimago?.cuartil ?? "Sin cuartil"}</span>
                                                                        <span className={`badge rounded-pill ${abiertaScimago ? "bg-success-subtle text-success-emphasis" : "bg-secondary-subtle text-secondary-emphasis"}`}>{abiertaScimago ? "Acceso abierto" : "Acceso por suscripción"}</span>
                                                                    </div>
                                                                    <div className="row g-3 small">
                                                                        <div className="col-12"><span className="d-block text-muted">Campos de estudio</span><div className="d-flex flex-wrap gap-1 mt-1">{areasScimago.length > 0 ? areasScimago.slice(0, 3).map((area) => <span className="badge rounded-pill bg-primary-subtle text-primary-emphasis text-wrap" key={area}>{area.trim()}</span>) : <span className="fw-semibold">No disponible</span>}</div></div>
                                                                        <div className="col-12 col-sm-6"><span className="d-block text-muted">Editorial</span><span className="fw-semibold text-break">{publisherScimago(revista) ?? "No disponible"}</span></div>
                                                                        <div className="col-12 col-sm-6"><span className="d-block text-muted">Cobertura</span><span className="fw-semibold text-break">{revista.scimago?.cobertura ?? "No disponible"}</span></div>
                                                                    </div>
                                                                </SeccionFuente>
                                                            </div>
                                                        )}
                                                    </div>
                                                    {/* FIN - Secciones Scopus y SCImago */}

                                                    {/* INICIO - Información expandible por fuente */}
                                                    {detallesAbiertos[clave] && <DetalleTecnico revista={revista} mostrarScopus={mostrarScopus} mostrarScimago={mostrarScimago} tieneScopus={tieneScopus} tieneScimago={tieneScimago} />}
                                                    {/* FIN - Información expandible por fuente */}

                                                    {/* INICIO - Mejora visual Buscar Revistas */}
                                                    <div className="d-flex flex-column flex-sm-row gap-2 mt-auto pt-3 border-top">
                                                        <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => alternarDetalle(clave)} aria-expanded={Boolean(detallesAbiertos[clave])}>
                                                            {detallesAbiertos[clave] ? "Ocultar información" : "Ver más"}
                                                        </button>
                                                        <LoadingButton className="btn btn-outline-warning btn-sm" onClick={() => guardar(revista)} loading={guardandoId === (revista.sourceId || revista.issn || revista.titulo)} loadingText="Guardando..." disabled={guardandoId !== null}>Guardar favorito</LoadingButton>
                                                        {mostrarScopus && tieneScopus(revista) && (revista.scopus?.enlaceScopus ? <a href={revista.scopus.enlaceScopus} target="_blank" rel="noreferrer" className="btn btn-primary btn-sm ms-sm-auto" onClick={(e) => verEnScopus(e, revista.scopus.enlaceScopus)}>Abrir en Scopus</a> : <button className="btn btn-primary btn-sm ms-sm-auto" disabled title="Scopus no proporcionó un enlace">Abrir en Scopus</button>)}
                                                    </div>
                                                    {/* FIN - Mejora visual Buscar Revistas */}
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
            </div>
        </MainLayout>
    );
    // FIN - Mejora visual Buscar Revistas
}

export default BuscarRevistas;
