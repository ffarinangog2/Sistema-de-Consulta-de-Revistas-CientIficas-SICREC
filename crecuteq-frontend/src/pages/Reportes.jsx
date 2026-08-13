import { useEffect, useRef, useState } from "react";
import {
    BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale,
    LineElement, PointElement, Title, Tooltip
} from "chart.js";
import { Bar, Line } from "react-chartjs-2";
import MainLayout from "../layouts/MainLayout";
import { useNotification } from "../hooks/useNotification";
import {
    obtenerAnaliticaReporte,
    exportarReporteHistorialExcel,
    exportarReporteHistorialPDF
} from "../services/reporteService";

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement,
    PointElement, Title, Tooltip, Legend);

function GraficoReporte({ titulo, subtitulo, etiquetas, valores, tipo = "bar", tema, archivo }) {
    const chartRef = useRef(null);
    const oscuro = tema === "dark";
    const colorTexto = oscuro ? "#b9c9be" : "#526258";
    const colorCuadricula = oscuro ? "rgba(174,211,188,.10)" : "rgba(38,79,46,.10)";
    const data = {
        labels: etiquetas,
        datasets: [{
            label: "Búsquedas", data: valores,
            backgroundColor: oscuro ? "rgba(49,166,19,.65)" : "rgba(27,117,5,.72)",
            borderColor: oscuro ? "#72d45a" : "#1b7505",
            borderWidth: tipo === "line" ? 2 : 0,
            borderRadius: tipo === "bar" ? 6 : 0,
            tension: .3, fill: false
        }]
    };
    const options = {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false }, title: { display: false }, tooltip: { intersect: false } },
        scales: {
            x: { ticks: { color: colorTexto, maxRotation: 45 }, grid: { color: colorCuadricula } },
            y: { beginAtZero: true, ticks: { color: colorTexto, precision: 0 }, grid: { color: colorCuadricula } }
        }
    };
    const Chart = tipo === "line" ? Line : Bar;

    const descargarPng = () => {
        const url = chartRef.current?.toBase64Image("image/png", 1);
        if (!url) return;
        const link = document.createElement("a");
        link.href = url;
        link.download = `${archivo}.png`;
        link.click();
    };

    return (
        <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
                <div className="d-flex justify-content-between align-items-start gap-2 mb-3">
                    <div><h5 className="mb-1">{titulo}</h5><p className="text-muted small mb-0">{subtitulo}</p></div>
                    <button className="btn btn-outline-primary btn-sm flex-shrink-0" onClick={descargarPng}
                        disabled={etiquetas.length === 0} title="Descargar gráfico en PNG">Descargar PNG</button>
                </div>
                {etiquetas.length === 0
                    ? <div className="text-muted text-center py-5">No hay datos para el período seleccionado.</div>
                    : <div style={{ height: 280 }}><Chart ref={chartRef} data={data} options={options} /></div>}
            </div>
        </div>
    );
}

function Reportes() {
    const { notify } = useNotification();
    const [datos, setDatos] = useState(null);
    const [cargando, setCargando] = useState(false);
    const [error, setError] = useState("");
    const [fechaInicio, setFechaInicio] = useState("");
    const [fechaFin, setFechaFin] = useState("");
    const [filtrosAplicados, setFiltrosAplicados] = useState({ fechaInicio: "", fechaFin: "" });
    const [tema, setTema] = useState(() => document.documentElement.dataset.theme || "light");

    useEffect(() => {
        const actualizarTema = (evento) => setTema(evento.detail);
        window.addEventListener("crecuteq-theme-change", actualizarTema);
        return () => window.removeEventListener("crecuteq-theme-change", actualizarTema);
    }, []);

    const cargarReporte = async (inicio, fin, pagina = 0, aplicarFiltros = false) => {
        setCargando(true);
        setError("");
        try {
            const respuesta = await obtenerAnaliticaReporte(inicio, fin, pagina);
            setDatos(respuesta);
            if (aplicarFiltros) setFiltrosAplicados({ fechaInicio: inicio, fechaFin: fin });
        } catch (e) {
            console.error(e);
            setError("No se pudo generar el reporte. Puede volver a intentarlo sin recargar la página.");
            notify("No se pudo generar el reporte de búsquedas.", "danger");
        } finally {
            setCargando(false);
        }
    };

    const generarReporte = () => {
        if ((!fechaInicio && fechaFin) || (fechaInicio && !fechaFin)) {
            notify("Seleccione las fechas Desde y Hasta.", "warning");
            return;
        }
        if (fechaInicio && fechaInicio > fechaFin) {
            notify("La fecha Desde no puede ser posterior a Hasta.", "warning");
            return;
        }
        cargarReporte(fechaInicio, fechaFin, 0, true);
    };

    const cambiarPagina = (pagina) => {
        if (pagina < 0 || pagina >= datos.pagina.totalPaginas || cargando) return;
        cargarReporte(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin, pagina);
    };

    const descargar = async (formato) => {
        try {
            const archivo = formato === "excel"
                ? await exportarReporteHistorialExcel(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin)
                : await exportarReporteHistorialPDF(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin);
            const url = window.URL.createObjectURL(new Blob([archivo]));
            const link = document.createElement("a");
            link.href = url;
            link.download = `Reporte_Historial.${formato === "excel" ? "xlsx" : "pdf"}`;
            document.body.appendChild(link);
            link.click(); link.remove(); window.URL.revokeObjectURL(url);
        } catch (e) {
            console.error(e);
            notify(`No se pudo descargar el reporte ${formato.toUpperCase()}.`, "danger");
        }
    };

    const resumen = datos?.resumen;
    const pagina = datos?.pagina;
    const tarjetas = resumen ? [
        ["Total de búsquedas", resumen.total, "▥", "primary"],
        ["Con resultados", resumen.conResultados, "✓", "success"],
        ["Sin resultados", resumen.sinResultados, "○", "warning"],
        ["Usuarios que buscaron", resumen.usuarios, "♙", "info"]
    ] : [];
    const desde = pagina?.totalRegistros ? pagina.paginaActual * pagina.tamano + 1 : 0;
    const hasta = pagina?.totalRegistros ? Math.min(desde + pagina.contenido.length - 1, pagina.totalRegistros) : 0;

    return (
        <MainLayout admin={true}>
            <div className="mb-4">
                <h2 className="mb-1">Reporte de búsquedas</h2>
                <p className="text-muted">Análisis del historial real de consultas realizadas en CRECUTEQ.</p>
            </div>

            <div className="card shadow-sm border-0 mb-4"><div className="card-body">
                <div className="row g-3 align-items-end">
                    <div className="col-12 col-sm-6 col-lg-3">
                        <label className="form-label" htmlFor="reporte-desde">Desde</label>
                        <input id="reporte-desde" type="date" className="form-control" value={fechaInicio}
                            onChange={(e) => setFechaInicio(e.target.value)} />
                    </div>
                    <div className="col-12 col-sm-6 col-lg-3">
                        <label className="form-label" htmlFor="reporte-hasta">Hasta</label>
                        <input id="reporte-hasta" type="date" className="form-control" value={fechaFin}
                            onChange={(e) => setFechaFin(e.target.value)} />
                    </div>
                    <div className="col-12 col-lg-6 d-flex flex-wrap gap-2">
                        <button className="btn btn-primary" onClick={generarReporte} disabled={cargando}>
                            {cargando ? "Generando..." : "Generar reporte"}
                        </button>
                        <button className="btn btn-success" onClick={() => descargar("excel")} disabled={!datos}>Excel</button>
                        <button className="btn btn-danger" onClick={() => descargar("pdf")} disabled={!datos}>PDF</button>
                    </div>
                </div>
                {cargando && <div className="d-flex align-items-center gap-2 text-muted small mt-3" role="status">
                    <span className="spinner-border spinner-border-sm text-primary" aria-hidden="true" /> Cargando datos del reporte...
                </div>}
                {error && <div className="alert alert-danger mt-3 mb-0" role="alert">{error}</div>}
            </div></div>

            {!datos && !cargando && (
                <div className="card shadow-sm border-0"><div className="card-body text-center text-muted py-5">
                    Seleccione un período opcional y presione “Generar reporte” para consultar la información.
                </div></div>
            )}

            {datos && <>
                <div className="row g-3 mb-4">
                    {tarjetas.map(([titulo, valor, icono, color]) => (
                        <div className="col-12 col-sm-6 col-xl-3" key={titulo}><div className="card shadow-sm border-0 h-100">
                            <div className="card-body d-flex align-items-center justify-content-between gap-3">
                                <div><p className="text-muted small mb-1">{titulo}</p><h3 className="mb-0">{valor}</h3></div>
                                <span className={`fs-3 text-${color}`} aria-hidden="true">{icono}</span>
                            </div>
                        </div></div>
                    ))}
                </div>

                <div className="row g-3 mb-4">
                    <div className="col-12 col-xl-6"><GraficoReporte titulo="Búsquedas por día"
                        subtitulo="Evolución diaria dentro del período seleccionado."
                        etiquetas={datos.busquedasPorDia.map((item) => item.etiqueta)}
                        valores={datos.busquedasPorDia.map((item) => item.total)} tipo="line" tema={tema} archivo="busquedas-por-dia" /></div>
                    <div className="col-12 col-xl-6"><GraficoReporte titulo="Términos más consultados"
                        subtitulo="Diez términos con mayor frecuencia."
                        etiquetas={datos.terminosMasConsultados.map((item) => item.etiqueta)}
                        valores={datos.terminosMasConsultados.map((item) => item.total)} tema={tema} archivo="terminos-mas-consultados" /></div>
                    <div className="col-12"><GraficoReporte titulo="Usuarios con más búsquedas"
                        subtitulo="Diez usuarios con mayor actividad de consulta."
                        etiquetas={datos.usuariosMasActivos.map((item) => item.etiqueta)}
                        valores={datos.usuariosMasActivos.map((item) => item.total)} tema={tema} archivo="usuarios-mas-activos" /></div>
                </div>

                <div className="card shadow-sm border-0"><div className="card-body">
                    <div className="d-flex flex-column flex-sm-row justify-content-between gap-2 mb-3">
                        <div><h5 className="mb-1">Historial de búsquedas</h5>
                            <p className="text-muted small mb-0">Total: {pagina.totalRegistros} registros</p></div>
                        {filtrosAplicados.fechaInicio && <span className="text-muted small">
                            {filtrosAplicados.fechaInicio} — {filtrosAplicados.fechaFin}</span>}
                    </div>
                    <div className="table-responsive"><table className="table table-hover table-bordered mb-0">
                        <thead className="table-dark"><tr><th>#</th><th>Fecha</th><th>Usuario</th><th>Término</th><th>Resultados</th></tr></thead>
                        <tbody>{pagina.contenido.length === 0
                            ? <tr><td colSpan="5" className="text-center text-muted py-4">No existen búsquedas para el período seleccionado.</td></tr>
                            : pagina.contenido.map((item, index) => <tr key={`${item.fecha}-${item.usuario}-${desde + index}`}>
                                <td>{desde + index}</td><td>{item.fecha?.replace("T", " ")}</td>
                                <td>{item.usuario}</td><td>{item.termino}</td><td>{item.resultados}</td>
                            </tr>)}</tbody>
                    </table></div>
                    <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mt-3">
                        <span className="text-muted small">Mostrando {desde}–{hasta} de {pagina.totalRegistros} registros · Página {pagina.totalPaginas ? pagina.paginaActual + 1 : 0} de {pagina.totalPaginas}</span>
                        <div className="d-flex gap-2">
                            <button className="btn btn-outline-primary btn-sm" disabled={cargando || pagina.paginaActual <= 0}
                                onClick={() => cambiarPagina(pagina.paginaActual - 1)}>Anterior</button>
                            <button className="btn btn-outline-primary btn-sm" disabled={cargando || pagina.totalPaginas === 0 || pagina.paginaActual >= pagina.totalPaginas - 1}
                                onClick={() => cambiarPagina(pagina.paginaActual + 1)}>Siguiente</button>
                        </div>
                    </div>
                </div></div>
            </>}
        </MainLayout>
    );
}

export default Reportes;
