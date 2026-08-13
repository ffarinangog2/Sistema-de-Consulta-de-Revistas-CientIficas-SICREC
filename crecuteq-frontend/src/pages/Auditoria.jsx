// INICIO - Auditoría
import { useCallback, useEffect, useMemo, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import LoadingButton from "../components/LoadingButton";
import { useNotification } from "../hooks/useNotification";
import {
    exportarAuditoriaExcel,
    exportarAuditoriaPDF,
    obtenerAuditoria
} from "../services/auditoriaService";
import { descargarArchivo } from "../utils/descargarArchivo";

const REGISTROS_POR_PAGINA = 10;

const textoUsuario = (registro) =>
    registro.usuario?.usuario ||
    registro.usuario?.nombreCompleto ||
    registro.nombreUsuario ||
    registro.usuario ||
    "Sistema";

const fechaRegistro = (registro) =>
    registro.fechaAccion || registro.fecha || registro.fechaHora || "";

const opciones = (registros, campo) => [...new Set(
    registros.map((registro) => campo(registro)).filter(Boolean)
)].sort((a, b) => String(a).localeCompare(String(b)));

function Auditoria() {

    const { notify } = useNotification();
    const [registros, setRegistros] = useState([]);
    const [cargando, setCargando] = useState(false);
    const [exportando, setExportando] = useState("");
    const [paginaActual, setPaginaActual] = useState(1);

    // INICIO - Filtros
    const [filtros, setFiltros] = useState({
        usuario: "",
        modulo: "",
        accion: "",
        resultado: "",
        fechaDesde: "",
        fechaHasta: "",
        busqueda: ""
    });

    const actualizarFiltro = (campo, valor) => {
        setFiltros((actuales) => ({ ...actuales, [campo]: valor }));
        setPaginaActual(1);
    };

    const usuarios = useMemo(
        () => opciones(registros, textoUsuario),
        [registros]
    );
    const modulos = useMemo(
        () => opciones(registros, (registro) => registro.modulo),
        [registros]
    );
    const acciones = useMemo(
        () => opciones(registros, (registro) => registro.accion),
        [registros]
    );

    const registrosFiltrados = useMemo(() => {
        const busqueda = filtros.busqueda.trim().toLowerCase();

        return registros
            .filter((registro) => {
                const fecha = fechaRegistro(registro).slice(0, 10);
                const valores = [
                    textoUsuario(registro),
                    registro.modulo,
                    registro.accion,
                    registro.descripcion,
                    registro.ipOrigen,
                    registro.resultado
                ].map((valor) => String(valor || "").toLowerCase());

                return (!filtros.usuario || textoUsuario(registro) === filtros.usuario) &&
                    (!filtros.modulo || registro.modulo === filtros.modulo) &&
                    (!filtros.accion || registro.accion === filtros.accion) &&
                    (!filtros.resultado || registro.resultado === filtros.resultado) &&
                    (!filtros.fechaDesde || fecha >= filtros.fechaDesde) &&
                    (!filtros.fechaHasta || fecha <= filtros.fechaHasta) &&
                    (!busqueda || valores.some((valor) => valor.includes(busqueda)));
            })
            .sort((a, b) => new Date(fechaRegistro(b)) - new Date(fechaRegistro(a)));
    }, [filtros, registros]);
    // FIN - Filtros

    const totalPaginas = Math.ceil(registrosFiltrados.length / REGISTROS_POR_PAGINA);
    const indiceInicial = (paginaActual - 1) * REGISTROS_POR_PAGINA;
    const registrosPagina = registrosFiltrados.slice(
        indiceInicial,
        indiceInicial + REGISTROS_POR_PAGINA
    );

    const cargarAuditoria = useCallback(async () => {
        try {
            setCargando(true);
            const data = await obtenerAuditoria();
            setRegistros(Array.isArray(data) ? data : data?.content || []);
            setPaginaActual(1);
        } catch (error) {
            console.error(error);
            const data = error.response?.data;
            notify(
                (typeof data === "string" ? data : data?.message) ||
                    "No fue posible consultar la auditoría.",
                "danger"
            );
        } finally {
            setCargando(false);
        }
    }, [notify]);

    useEffect(() => {
        cargarAuditoria();
    }, [cargarAuditoria]);

    // INICIO - Exportación
    const exportar = async (formato) => {
        try {
            setExportando(formato);
            const archivo = formato === "excel"
                ? await exportarAuditoriaExcel(filtros)
                : await exportarAuditoriaPDF(filtros);

            descargarArchivo(
                archivo,
                formato === "excel" ? "Auditoria_CRECUTEQ.xlsx" : "Auditoria_CRECUTEQ.pdf"
            );
        } catch (error) {
            console.error(error);
            notify(`No fue posible exportar la auditoría en ${formato.toUpperCase()}.`, "danger");
        } finally {
            setExportando("");
        }
    };
    // FIN - Exportación

    const colorModulo = (modulo) => {
        const colores = {
            "AUTENTICACIÓN": "bg-primary",
            USUARIOS: "bg-info text-dark",
            FAVORITOS: "bg-warning text-dark",
            "BÚSQUEDAS": "bg-secondary"
        };
        return colores[String(modulo || "").toUpperCase()] || "bg-dark";
    };

    const formatearFecha = (registro) => {
        const fecha = fechaRegistro(registro);
        if (!fecha) return "-";
        const valor = new Date(fecha);
        return Number.isNaN(valor.getTime()) ? fecha.replace("T", " ") : valor.toLocaleString();
    };

    return (
        <MainLayout admin={true}>
            {/* INICIO - Mejora Auditoría */}
            <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
                <div>
                    <h2 className="mb-1">Auditoría del Sistema</h2>
                    <p className="text-muted mb-1">Consulte y exporte el registro de actividad del sistema.</p>
                    <span className="text-muted small">
                        Total de registros encontrados: <strong>{registrosFiltrados.length}</strong>
                    </span>
                </div>

                {/* INICIO - Responsividad */}
                <div className="d-flex flex-wrap gap-2">
                    <LoadingButton
                        className="btn btn-success"
                        onClick={() => exportar("excel")}
                        loading={exportando === "excel"}
                        loadingText="Exportando Excel..."
                        disabled={Boolean(exportando)}
                    >
                        📊 Excel
                    </LoadingButton>
                    <LoadingButton
                        className="btn btn-danger"
                        onClick={() => exportar("pdf")}
                        loading={exportando === "pdf"}
                        loadingText="Exportando PDF..."
                        disabled={Boolean(exportando)}
                    >
                        📄 PDF
                    </LoadingButton>
                </div>
                {/* FIN - Responsividad */}
            </div>

            {/* INICIO - Filtros */}
            <div className="card shadow-sm border-0 mb-3">
                <div className="card-header bg-white border-0 pt-3 pb-0">
                    <h5 className="mb-0">Filtros de consulta</h5>
                </div>
                <div className="card-body">
                    <div className="row g-3">
                        <div className="col-12 col-lg-4">
                            <label className="form-label">Buscador general</label>
                            <input
                                className="form-control"
                                placeholder="Buscar en todos los campos..."
                                value={filtros.busqueda}
                                onChange={(e) => actualizarFiltro("busqueda", e.target.value)}
                            />
                        </div>
                        <div className="col-6 col-lg-2">
                            <label className="form-label">Usuario</label>
                            <select className="form-select" value={filtros.usuario} onChange={(e) => actualizarFiltro("usuario", e.target.value)}>
                                <option value="">Todos</option>
                                {usuarios.map((valor) => <option key={valor}>{valor}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-lg-2">
                            <label className="form-label">Módulo</label>
                            <select className="form-select" value={filtros.modulo} onChange={(e) => actualizarFiltro("modulo", e.target.value)}>
                                <option value="">Todos</option>
                                {modulos.map((valor) => <option key={valor}>{valor}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-lg-2">
                            <label className="form-label">Acción</label>
                            <select className="form-select" value={filtros.accion} onChange={(e) => actualizarFiltro("accion", e.target.value)}>
                                <option value="">Todas</option>
                                {acciones.map((valor) => <option key={valor}>{valor}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-lg-2">
                            <label className="form-label">Resultado</label>
                            <select className="form-select" value={filtros.resultado} onChange={(e) => actualizarFiltro("resultado", e.target.value)}>
                                <option value="">Todos</option>
                                <option value="ÉXITO">ÉXITO</option>
                                <option value="ERROR">ERROR</option>
                            </select>
                        </div>
                        <div className="col-6 col-md-3">
                            <label className="form-label">Fecha desde</label>
                            <input type="date" className="form-control" value={filtros.fechaDesde} onChange={(e) => actualizarFiltro("fechaDesde", e.target.value)} />
                        </div>
                        <div className="col-6 col-md-3">
                            <label className="form-label">Fecha hasta</label>
                            <input type="date" className="form-control" value={filtros.fechaHasta} onChange={(e) => actualizarFiltro("fechaHasta", e.target.value)} />
                        </div>
                        <div className="col-12 col-md-3 d-flex align-items-end">
                            <LoadingButton
                                className="btn btn-primary w-100"
                                onClick={cargarAuditoria}
                                loading={cargando}
                                loadingText="Consultando..."
                            >
                                🔎 Consultar
                            </LoadingButton>
                        </div>
                    </div>
                </div>
            </div>
            {/* FIN - Filtros */}

            {/* INICIO - Tabla de auditoría */}
            <div className="card shadow-sm border-0">
                <div className="card-header bg-white border-0 pt-3 pb-0">
                    <h5 className="mb-0">Registros de auditoría</h5>
                </div>
                <div className="card-body">
                    {cargando ? (
                        <div className="text-center py-5">
                            <div className="spinner-border text-primary" role="status" />
                            <p className="text-muted mt-2 mb-0">Consultando auditoría...</p>
                        </div>
                    ) : registrosPagina.length === 0 ? (
                        <div className="alert alert-info mb-0">
                            No existen registros de auditoría que coincidan con los filtros.
                        </div>
                    ) : (
                        <div className="table-responsive">
                            <table className="table table-hover table-bordered align-middle mb-0">
                                <thead className="table-dark">
                                    <tr>
                                        <th>Fecha y hora</th>
                                        <th>Usuario</th>
                                        <th>Módulo</th>
                                        <th>Acción</th>
                                        <th>Descripción</th>
                                        <th>Dirección IP</th>
                                        <th>Resultado</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {registrosPagina.map((registro, index) => (
                                        <tr key={registro.id || indiceInicial + index}>
                                            <td className="text-nowrap">{formatearFecha(registro)}</td>
                                            <td>{textoUsuario(registro)}</td>
                                            <td><span className={`badge ${colorModulo(registro.modulo)}`}>{registro.modulo || "-"}</span></td>
                                            <td>{registro.accion || "-"}</td>
                                            <td className="text-break">{registro.descripcion || "-"}</td>
                                            <td className="text-nowrap">{registro.ipOrigen || registro.direccionIp || "-"}</td>
                                            <td>
                                                <span className={`badge ${registro.resultado === "ÉXITO" ? "bg-success" : "bg-danger"}`}>
                                                    {registro.resultado || "ERROR"}
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
            {/* FIN - Tabla de auditoría */}

            {totalPaginas > 1 && (
                <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mt-3">
                    <span>Página {paginaActual} de {totalPaginas}</span>
                    <div>
                        <button className="btn btn-outline-primary btn-sm me-2" disabled={paginaActual === 1} onClick={() => setPaginaActual((pagina) => pagina - 1)}>Anterior</button>
                        <button className="btn btn-outline-primary btn-sm" disabled={paginaActual === totalPaginas} onClick={() => setPaginaActual((pagina) => pagina + 1)}>Siguiente</button>
                    </div>
                </div>
            )}
            {/* FIN - Mejora Auditoría */}
        </MainLayout>
    );
}

export default Auditoria;
// FIN - Auditoría
