function UltimasBusquedasTable({ datos }) {

    return (

        // INICIO - Mejora Historial
        <div className="card shadow-sm border-0 mt-4">

            <div className="card-header bg-white border-0 pt-3 px-3 px-md-4">

                <h5 className="mb-1">Últimas búsquedas realizadas</h5>
                <p className="text-muted small">Actividad reciente registrada en el sistema.</p>

            </div>

            <div className="card-body px-3 px-md-4 pt-2">

                {/* INICIO - Responsividad */}
                <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">

                    <thead className="table-dark">

                    <tr>

                        <th>Fecha</th>
                        <th>Usuario</th>
                        <th>Término</th>
                        <th>Resultados</th>

                    </tr>

                    </thead>

                    <tbody>

                    {datos.map((item, index) => (

                        <tr key={index}>

                            <td>
                                {item.fecha?.replace("T", " ")}
                            </td>

                            <td>{item.usuario}</td>

                            <td>{item.termino}</td>

                            <td>{item.resultados}</td>

                        </tr>

                    ))}

                    </tbody>

                </table>
                </div>
                {/* FIN - Responsividad */}

            </div>

        </div>
        // FIN - Mejora Historial

    );

}

export default UltimasBusquedasTable;
