import { useEffect, useRef, useState } from "react";
import {
    BarElement, CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale,
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
    PointElement, Title, Tooltip, Legend, Filler);

const Icono = ({ nombre, size = 20 }) => {
    const trazos = {
        reportes: <><path d="M4 19V5a2 2 0 0 1 2-2h9l5 5v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2Z"/><path d="M14 3v6h6M8 16v-3m4 3V9m4 7v-4"/></>,
        calendario: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
        buscar: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
        excel: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 13l4 5m0-5-4 5"/></>,
        pdf: <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><path d="M14 2v6h6M8 15h8M8 18h5"/></>,
        descarga: <><path d="M12 3v12m-5-5 5 5 5-5M5 21h14"/></>,
        total: <><path d="M4 19V9m5 10V5m5 14v-7m5 7V3"/></>,
        correcto: <><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></>,
        vacio: <><circle cx="12" cy="12" r="9"/><path d="M8 12h8"/></>,
        usuarios: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></>,
        tendencia: <><path d="m3 17 6-6 4 4 8-9"/><path d="M15 6h6v6"/></>,
        tabla: <><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 10h18M9 4v16"/></>,
        izquierda: <path d="m15 18-6-6 6-6"/>, derecha: <path d="m9 18 6-6-6-6"/>
    };
    return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{trazos[nombre]}</svg>;
};

const formatearNumero = (valor) => new Intl.NumberFormat("es-EC").format(valor ?? 0);
const formatearFecha = (valor, incluirHora = false) => {
    if (!valor) return "—";
    const fecha = new Date(incluirHora ? valor : `${valor}T00:00:00`);
    if (Number.isNaN(fecha.getTime())) return valor.replace("T", " ");
    return new Intl.DateTimeFormat("es-EC", incluirHora
        ? { dateStyle: "medium", timeStyle: "short" }
        : { day: "2-digit", month: "short", year: "numeric" }).format(fecha);
};

function GraficoReporte({ titulo, subtitulo, etiquetas, valores, tipo = "bar", tema, archivo, destacado = false }) {
    const chartRef = useRef(null);
    const oscuro = tema === "dark";
    const colorTexto = oscuro ? "#c4cbc5" : "#59636d";
    const colorCuadricula = oscuro ? "rgba(174,211,188,.12)" : "rgba(27,117,5,.10)";
    const data = {
        labels: etiquetas,
        datasets: [{
            label: "Búsquedas", data: valores,
            backgroundColor: tipo === "line" ? "rgba(49,166,19,.12)" : (oscuro ? "rgba(49,166,19,.72)" : "rgba(27,117,5,.78)"),
            borderColor: oscuro ? "#72d45a" : "#1b7505", borderWidth: tipo === "line" ? 2.5 : 0,
            borderRadius: tipo === "bar" ? 7 : 0, borderSkipped: false,
            pointBackgroundColor: oscuro ? "#72d45a" : "#1b7505", pointBorderColor: oscuro ? "#2d332e" : "#fff",
            pointBorderWidth: 2, pointRadius: 3, pointHoverRadius: 5, tension: .35, fill: tipo === "line"
        }]
    };
    const options = {
        responsive: true, maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        plugins: {
            legend: { display: false }, title: { display: false },
            tooltip: { backgroundColor: oscuro ? "#101d17" : "#212529", padding: 12, cornerRadius: 8,
                displayColors: false, callbacks: { label: (contexto) => `${formatearNumero(contexto.raw)} búsquedas` } }
        },
        scales: {
            x: { ticks: { color: colorTexto, maxRotation: 35, font: { size: 11 } }, grid: { display: false }, border: { display: false } },
            y: { beginAtZero: true, ticks: { color: colorTexto, precision: 0, padding: 8 }, grid: { color: colorCuadricula }, border: { display: false } }
        }
    };
    const Chart = tipo === "line" ? Line : Bar;

    const descargarPng = () => {
        const url = chartRef.current?.toBase64Image("image/png", 1);
        if (!url) return;
        const link = document.createElement("a");
        link.href = url; link.download = `${archivo}.png`; link.click();
    };

    const descargarExcel = () => {
        const filas = etiquetas.map((etiqueta, indice) => `<tr><td>${String(etiqueta).replaceAll("&", "&amp;").replaceAll("<", "&lt;")}</td><td>${valores[indice] ?? 0}</td></tr>`).join("");
        const contenido = `<!doctype html><html><head><meta charset="UTF-8"></head><body><table><thead><tr><th>${titulo}</th><th>Búsquedas</th></tr></thead><tbody>${filas}</tbody></table></body></html>`;
        const url = window.URL.createObjectURL(new Blob([contenido], { type: "application/vnd.ms-excel;charset=utf-8" }));
        const link = document.createElement("a");
        link.href = url; link.download = `${archivo}.xls`; link.click(); window.URL.revokeObjectURL(url);
    };

    return <article className={`reporte-chart ${destacado ? "reporte-chart--destacado" : ""}`}>
        <header className="reporte-chart__header">
            <div><span className="reporte-section__eyebrow">Analítica</span><h3>{titulo}</h3><p>{subtitulo}</p></div>
            <div className="reporte-chart__actions">
                <button onClick={descargarExcel} disabled={!etiquetas.length} title="Exportar datos a Excel"><Icono nombre="excel" size={16}/>Excel</button>
                <button className="reporte-icon-button" onClick={descargarPng} disabled={!etiquetas.length} title="Descargar gráfico como PNG" aria-label={`Descargar gráfico ${titulo} como PNG`}><Icono nombre="descarga" size={18}/><span>PNG</span></button>
            </div>
        </header>
        {!etiquetas.length
            ? <div className="reporte-chart__empty"><Icono nombre="tendencia" size={28}/><span>No hay datos para este período</span></div>
            : <div className="reporte-chart__canvas"><Chart ref={chartRef} data={data} options={options}/></div>}
    </article>;
}

function Reportes() {
    const { notify } = useNotification();
    const [datos, setDatos] = useState(null);
    const [cargando, setCargando] = useState(false);
    const [descargando, setDescargando] = useState("");
    const [vista, setVista] = useState("resumen");
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

    const cargarReporte = async (inicio, fin, numeroPagina = 0, aplicarFiltros = false) => {
        setCargando(true); setError("");
        try {
            const respuesta = await obtenerAnaliticaReporte(inicio, fin, numeroPagina);
            setDatos(respuesta);
            if (aplicarFiltros) setFiltrosAplicados({ fechaInicio: inicio, fechaFin: fin });
        } catch (e) {
            console.error(e); setError("No se pudo generar el reporte. Inténtelo nuevamente.");
            notify("No se pudo generar el reporte de búsquedas.", "danger");
        } finally { setCargando(false); }
    };

    const generarReporte = () => {
        const inicio = fechaInicio || fechaFin;
        const fin = fechaFin || fechaInicio;
        if (inicio && inicio > fin) return notify("La fecha inicial no puede ser posterior a la fecha final.", "warning");
        setFechaInicio(inicio); setFechaFin(fin); setVista("resumen");
        cargarReporte(inicio, fin, 0, true);
    };

    const limpiarFechas = () => { setFechaInicio(""); setFechaFin(""); };
    const aplicarPeriodo = (dias) => {
        const fin = new Date(); const inicio = new Date();
        inicio.setDate(fin.getDate() - (dias - 1));
        const iso = (fecha) => fecha.toLocaleDateString("en-CA");
        setFechaInicio(iso(inicio)); setFechaFin(iso(fin));
    };
    const cambiarPagina = (numeroPagina) => {
        if (numeroPagina < 0 || numeroPagina >= datos.pagina.totalPaginas || cargando) return;
        cargarReporte(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin, numeroPagina);
    };
    const descargar = async (formato) => {
        setDescargando(formato);
        try {
            const archivo = formato === "excel"
                ? await exportarReporteHistorialExcel(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin)
                : await exportarReporteHistorialPDF(filtrosAplicados.fechaInicio, filtrosAplicados.fechaFin);
            const url = window.URL.createObjectURL(new Blob([archivo]));
            const link = document.createElement("a");
            link.href = url; link.download = `Reporte_Historial.${formato === "excel" ? "xlsx" : "pdf"}`;
            document.body.appendChild(link); link.click(); link.remove(); window.URL.revokeObjectURL(url);
        } catch (e) { console.error(e); notify(`No se pudo descargar el reporte ${formato.toUpperCase()}.`, "danger"); }
        finally { setDescargando(""); }
    };

    const resumen = datos?.resumen;
    const pagina = datos?.pagina;
    const porcentajeExito = resumen?.total ? Math.round((resumen.conResultados / resumen.total) * 100) : 0;
    const tarjetas = resumen ? [
        { titulo: "Total de búsquedas", valor: resumen.total, icono: "total", detalle: "Consultas registradas", tono: "principal" },
        { titulo: "Con resultados", valor: resumen.conResultados, icono: "correcto", detalle: `${porcentajeExito}% de efectividad`, tono: "exito" },
        { titulo: "Sin resultados", valor: resumen.sinResultados, icono: "vacio", detalle: "Oportunidades de mejora", tono: "alerta" },
        { titulo: "Usuarios activos", valor: resumen.usuarios, icono: "usuarios", detalle: "Usuarios que consultaron", tono: "informacion" }
    ] : [];
    const desde = pagina?.totalRegistros ? pagina.paginaActual * pagina.tamano + 1 : 0;
    const hasta = pagina?.totalRegistros ? Math.min(desde + pagina.contenido.length - 1, pagina.totalRegistros) : 0;
    const periodo = filtrosAplicados.fechaInicio
        ? `${formatearFecha(filtrosAplicados.fechaInicio)} — ${formatearFecha(filtrosAplicados.fechaFin)}` : "Todo el historial";

    return <MainLayout admin={true}>
        <section className="reportes-page" aria-labelledby="reportes-titulo">
            <header className="reportes-heading">
                <h2 id="reportes-titulo">Reportes de búsquedas</h2>
                <p className="text-muted">Análisis del historial de consultas realizadas en CRECUTEQ.</p>
            </header>

            <section className="reporte-toolbar" aria-label="Configuración del reporte">
                <div className="reporte-toolbar__top"><div className="reporte-toolbar__heading"><Icono nombre="calendario"/><div><h2>¿Qué período desea analizar?</h2><p>Una sola fecha consulta ese día; agregue la segunda para crear un rango.</p></div></div>
                    <div className="reporte-presets"><button onClick={() => aplicarPeriodo(7)}>7 días</button><button onClick={() => aplicarPeriodo(30)}>30 días</button><button onClick={() => aplicarPeriodo(90)}>90 días</button><button onClick={limpiarFechas}>Todo</button></div></div>
                <div className="reporte-toolbar__controls">
                    <label className="reporte-date"><span>Fecha inicial</span><div><Icono nombre="calendario" size={17}/><input type="date" value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} max={fechaFin || undefined}/></div></label>
                    <span className="reporte-date__separator" aria-hidden="true">hasta</span>
                    <label className="reporte-date"><span>Fecha final <em>(opcional)</em></span><div><Icono nombre="calendario" size={17}/><input type="date" value={fechaFin} onChange={(e) => setFechaFin(e.target.value)} min={fechaInicio || undefined}/></div></label>
                    {(fechaInicio || fechaFin) && <button className="reporte-clear" onClick={limpiarFechas}>Quitar fechas</button>}
                    <button className="reporte-generate" onClick={generarReporte} disabled={cargando}>
                        {cargando ? <span className="spinner-border spinner-border-sm"/> : <Icono nombre="buscar" size={18}/>} {cargando ? "Generando…" : "Generar reporte"}
                    </button>
                </div>
            </section>

            {error && <div className="reporte-alert" role="alert"><strong>No fue posible cargar la información.</strong><span>{error}</span><button onClick={generarReporte}>Reintentar</button></div>}

            {!datos && !cargando && <section className="reporte-empty">
                <div className="reporte-empty__visual"><Icono nombre="tendencia" size={42}/></div>
                <span className="reporte-section__eyebrow">Análisis bajo demanda</span><h2>Convierta los datos en decisiones</h2>
                <p>Seleccione un período específico o genere el reporte sin fechas para analizar todo el historial disponible.</p>
                <div className="reporte-empty__features"><span>Indicadores clave</span><span>Tendencias visuales</span><span>Exportación profesional</span></div>
            </section>}

            {datos && <div className={`reporte-results ${cargando ? "reporte-results--loading" : ""}`} aria-busy={cargando}>
                <div className="reporte-results__bar"><div><span className="reporte-section__eyebrow">Informe generado</span><h2>{periodo}</h2></div>
                    <div className="reporte-export"><span>Exportar informe</span>
                        <button onClick={() => descargar("excel")} disabled={!!descargando}><Icono nombre="excel" size={17}/>{descargando === "excel" ? "Preparando…" : "Excel"}</button>
                        <button onClick={() => descargar("pdf")} disabled={!!descargando}><Icono nombre="pdf" size={17}/>{descargando === "pdf" ? "Preparando…" : "PDF"}</button>
                    </div></div>

                <nav className="reporte-tabs" aria-label="Secciones del reporte">
                    <button className={vista === "resumen" ? "is-active" : ""} onClick={() => setVista("resumen")}><Icono nombre="total" size={18}/>Resumen</button>
                    <button className={vista === "analitica" ? "is-active" : ""} onClick={() => setVista("analitica")}><Icono nombre="tendencia" size={18}/>Gráficos</button>
                    <button className={vista === "registros" ? "is-active" : ""} onClick={() => setVista("registros")}><Icono nombre="tabla" size={18}/>Registros <span>{formatearNumero(pagina.totalRegistros)}</span></button>
                </nav>

                {vista === "resumen" && <section className="reporte-view reporte-view--summary"><div className="reporte-view__heading"><span className="reporte-section__eyebrow">Resumen ejecutivo</span><h2>Indicadores principales</h2><p>Vista rápida del rendimiento del período seleccionado.</p></div><div className="reporte-kpis">{tarjetas.map((tarjeta) => <article className={`reporte-kpi reporte-kpi--${tarjeta.tono}`} key={tarjeta.titulo}>
                    <div className="reporte-kpi__icon"><Icono nombre={tarjeta.icono} size={22}/></div><div className="reporte-kpi__content"><span>{tarjeta.titulo}</span><strong>{formatearNumero(tarjeta.valor)}</strong><small>{tarjeta.detalle}</small></div>
                </article>)}</div><button className="reporte-view__next" onClick={() => setVista("analitica")}>Ver análisis gráfico <Icono nombre="derecha" size={17}/></button></section>}

                {vista === "analitica" && <section className="reporte-view"><div className="reporte-view__heading"><span className="reporte-section__eyebrow">Analítica visual</span><h2>Tendencias del período</h2><p>Exporte cada visualización como imagen PNG o sus datos como Excel.</p></div><div className="reporte-charts">
                    <GraficoReporte titulo="Evolución de búsquedas" subtitulo="Comportamiento diario de las consultas en el período." etiquetas={datos.busquedasPorDia.map((i) => i.etiqueta)} valores={datos.busquedasPorDia.map((i) => i.total)} tipo="line" tema={tema} archivo="busquedas-por-dia" destacado/>
                    <GraficoReporte titulo="Términos más consultados" subtitulo="Los diez temas con mayor interés entre los usuarios." etiquetas={datos.terminosMasConsultados.map((i) => i.etiqueta)} valores={datos.terminosMasConsultados.map((i) => i.total)} tema={tema} archivo="terminos-mas-consultados"/>
                    <GraficoReporte titulo="Usuarios con mayor actividad" subtitulo="Los diez usuarios con mayor volumen de consultas." etiquetas={datos.usuariosMasActivos.map((i) => i.etiqueta)} valores={datos.usuariosMasActivos.map((i) => i.total)} tema={tema} archivo="usuarios-mas-activos"/>
                </div></section>}

                {vista === "registros" && <section className="reporte-view"><div className="reporte-view__heading"><span className="reporte-section__eyebrow">Detalle operativo</span><h2>Registros encontrados</h2><p>Consulte exclusivamente el historial que coincide con el período solicitado.</p></div><section className="reporte-history">
                    <header className="reporte-history__header"><div className="reporte-history__title"><span className="reporte-history__icon"><Icono nombre="tabla"/></span><div><span className="reporte-section__eyebrow">Detalle operativo</span><h2>Historial de búsquedas</h2><p>{formatearNumero(pagina.totalRegistros)} registros · {periodo}</p></div></div>
                        <span className="reporte-history__page">Página {pagina.totalPaginas ? pagina.paginaActual + 1 : 0} de {pagina.totalPaginas}</span></header>
                    <div className="reporte-table-wrap"><table className="reporte-table"><thead><tr><th scope="col">N.º</th><th scope="col">Fecha y hora</th><th scope="col">Usuario</th><th scope="col">Término consultado</th><th scope="col" className="text-end">Resultados</th></tr></thead>
                        <tbody>{pagina.contenido.length === 0 ? <tr><td colSpan="5"><div className="reporte-table__empty">No existen búsquedas para el período seleccionado.</div></td></tr>
                            : pagina.contenido.map((item, index) => <tr key={`${item.fecha}-${item.usuario}-${desde + index}`}><td><span className="reporte-table__number">{desde + index}</span></td><td className="reporte-table__date">{formatearFecha(item.fecha, true)}</td><td><span className="reporte-table__user">{(item.usuario || "?").charAt(0).toUpperCase()}</span>{item.usuario || "Sin identificar"}</td><td className="reporte-table__term">{item.termino || "—"}</td><td className="text-end"><span className={`reporte-table__result ${Number(item.resultados) > 0 ? "is-positive" : "is-empty"}`}>{formatearNumero(item.resultados)}</span></td></tr>)}</tbody></table></div>
                    <footer className="reporte-history__footer"><span>Mostrando <strong>{desde}–{hasta}</strong> de <strong>{formatearNumero(pagina.totalRegistros)}</strong> registros</span><div className="reporte-pagination">
                        <button disabled={cargando || pagina.paginaActual <= 0} onClick={() => cambiarPagina(pagina.paginaActual - 1)} aria-label="Página anterior"><Icono nombre="izquierda" size={17}/>Anterior</button>
                        <span>{pagina.totalPaginas ? pagina.paginaActual + 1 : 0}</span>
                        <button disabled={cargando || !pagina.totalPaginas || pagina.paginaActual >= pagina.totalPaginas - 1} onClick={() => cambiarPagina(pagina.paginaActual + 1)}>Siguiente<Icono nombre="derecha" size={17}/></button>
                    </div></footer>
                </section></section>}
            </div>}
        </section>
    </MainLayout>;
}

export default Reportes;
