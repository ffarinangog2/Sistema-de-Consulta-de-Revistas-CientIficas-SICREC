import MainLayout from "../layouts/MainLayout";

// INICIO - Pantalla Ayuda
function Ayuda() {
    const filtros = [
        ["Campo de estudio", "Permite seleccionar un área temática y localizar revistas relacionadas con esa disciplina."],
        ["Cuartil", "Filtra las revistas por Q1, Q2, Q3, Q4 o por aquellas que todavía no tienen cuartil."],
        ["Acceso abierto", "Distingue las revistas de acceso abierto de aquellas que requieren suscripción."],
        ["Fuente de datos", "Muestra información de ambas fuentes o limita los resultados a Scopus o SCImago."],
        ["Cantidad de resultados", "Define el máximo de revistas que se solicitarán en cada búsqueda."]
    ];

    const preguntas = [
        ["¿Puedo buscar por ISSN?", "Sí. Escriba el ISSN con o sin guion en el campo principal de búsqueda."],
        ["¿Por qué una revista puede no tener cuartil?", "Puede no disponer de métricas suficientes, no estar clasificada para el período consultado o no existir en la fuente seleccionada."],
        ["¿Scopus y SCImago muestran siempre los mismos datos?", "No. Son fuentes independientes y sus métricas, cobertura y actualizaciones pueden diferir."],
        ["¿Cómo vuelvo a ejecutar una búsqueda anterior?", "Abra Historial y seleccione Buscar nuevamente en el registro correspondiente."],
        ["¿Dónde encuentro una revista guardada?", "Las revistas guardadas se encuentran en la opción Mis Favoritos del menú lateral."]
    ];

    return (
        <MainLayout admin={false}>
            <div className="container-fluid px-0" style={{ maxWidth: "1200px" }}>
                <header className="mb-4">
                    <span className="text-primary text-uppercase fw-semibold small">Centro de ayuda</span>
                    <h2 className="fw-bold mt-1 mb-2">Cómo utilizar SICREC</h2>
                    <p className="text-muted">Guía para buscar, comparar y organizar revistas científicas.</p>
                </header>

                <section className="card border-0 shadow-sm mb-4">
                    <div className="card-body p-3 p-md-4">
                        <h5 className="fw-bold">Objetivo del sistema</h5>
                        <p className="text-muted">
                            SICREC facilita la consulta de revistas científicas y la comparación de información proveniente de Scopus y SCImago, ayudando a identificar fuentes adecuadas para investigación, publicación y revisión bibliográfica.
                        </p>
                    </div>
                </section>

                <div className="row g-4 mb-4">
                    <div className="col-12 col-lg-6">
                        <section className="card border-0 shadow-sm h-100">
                            <div className="card-body p-3 p-md-4">
                                <h5 className="fw-bold">Cómo buscar revistas</h5>
                                <ol className="text-muted mb-0 ps-3">
                                    <li className="mb-2">Abra <strong>Buscar Revistas</strong> desde el menú lateral.</li>
                                    <li className="mb-2">Ingrese el título o ISSN de la revista.</li>
                                    <li className="mb-2">Ajuste los filtros que necesite.</li>
                                    <li className="mb-2">Seleccione <strong>Buscar</strong> y revise los resultados.</li>
                                    <li>Utilice <strong>Ver más</strong> para consultar las métricas adicionales.</li>
                                </ol>
                            </div>
                        </section>
                    </div>

                    <div className="col-12 col-lg-6">
                        <section className="card border-0 shadow-sm h-100">
                            <div className="card-body p-3 p-md-4">
                                <h5 className="fw-bold">Fuentes de información</h5>
                                <div className="border-start border-primary border-3 ps-3 mb-3">
                                    <h6 className="fw-bold mb-1">Scopus</h6>
                                    <p className="text-muted small">Base de datos bibliográfica que proporciona información de revistas, cobertura y métricas como CiteScore, SJR, SNIP y percentiles.</p>
                                </div>
                                <div className="border-start border-dark border-3 ps-3">
                                    <h6 className="fw-bold mb-1">SCImago</h6>
                                    <p className="text-muted small">Portal de indicadores científicos basado en información bibliométrica, utilizado para consultar cuartiles, SJR, índice H, categorías, áreas y ranking.</p>
                                </div>
                            </div>
                        </section>
                    </div>
                </div>

                <section className="mb-4">
                    <h5 className="fw-bold mb-3">Filtros de búsqueda</h5>
                    <div className="row g-3">
                        {filtros.map(([titulo, descripcion]) => (
                            <div className="col-12 col-md-6 col-xl" key={titulo}>
                                <div className="card border-0 shadow-sm h-100">
                                    <div className="card-body p-3">
                                        <h6 className="fw-bold">{titulo}</h6>
                                        <p className="text-muted small">{descripcion}</p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                <div className="row g-4 mb-4">
                    <div className="col-12 col-lg-6">
                        <section className="card border-0 shadow-sm h-100">
                            <div className="card-body p-3 p-md-4">
                                <h5 className="fw-bold">Favoritos e historial</h5>
                                <h6 className="mt-3">Guardar favoritos</h6>
                                <p className="text-muted small mb-3">En una tarjeta de resultados, seleccione <strong>Guardar favorito</strong>. Después podrá consultar la revista desde Mis Favoritos.</p>
                                <h6>Consultar el historial</h6>
                                <p className="text-muted small">Cada búsqueda completada se registra automáticamente. Abra Historial para revisar el término, la fecha y la cantidad de resultados, o para buscar nuevamente.</p>
                            </div>
                        </section>
                    </div>

                    <div className="col-12 col-lg-6">
                        <section className="card border-0 shadow-sm h-100">
                            <div className="card-body p-3 p-md-4">
                                <h5 className="fw-bold">¿Qué significan los cuartiles?</h5>
                                <p className="text-muted small mb-3">Los cuartiles agrupan las revistas de una categoría según su posición relativa.</p>
                                <div className="d-flex flex-column gap-2 small">
                                    <span><span className="badge bg-success me-2">Q1</span>Primer 25 %, revistas mejor posicionadas.</span>
                                    <span><span className="badge bg-primary me-2">Q2</span>Entre el 25 % y el 50 %.</span>
                                    <span><span className="badge bg-warning text-dark me-2">Q3</span>Entre el 50 % y el 75 %.</span>
                                    <span><span className="badge bg-danger me-2">Q4</span>Último 25 % de la categoría.</span>
                                </div>
                            </div>
                        </section>
                    </div>
                </div>

                <section className="card border-0 shadow-sm mb-4">
                    <div className="card-body p-3 p-md-4">
                        <h5 className="fw-bold mb-3">Preguntas frecuentes</h5>
                        <div className="d-flex flex-column gap-2">
                            {preguntas.map(([pregunta, respuesta]) => (
                                <details className="border rounded-3 p-3" key={pregunta}>
                                    <summary className="fw-semibold">{pregunta}</summary>
                                    <p className="text-muted small mt-2">{respuesta}</p>
                                </details>
                            ))}
                        </div>
                    </div>
                </section>

                <section className="card border-0 shadow-sm">
                    <div className="card-body p-3 p-md-4 d-flex flex-column flex-md-row justify-content-between gap-3 align-items-md-center">
                        <div>
                            <h5 className="fw-bold mb-1">Contacto y soporte</h5>
                            <p className="text-muted small">Para asistencia, comuníquese con el responsable institucional de SICREC o con el área de soporte tecnológico de su institución.</p>
                        </div>
                        <span className="badge bg-light text-dark border p-2">Soporte institucional</span>
                    </div>
                </section>
            </div>
        </MainLayout>
    );
}

export default Ayuda;
// FIN - Pantalla Ayuda
