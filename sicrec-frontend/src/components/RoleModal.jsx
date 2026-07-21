function RoleModal({

                       mostrar,
                       modoEdicion,
                       nombreRol,
                       setNombreRol,
                       onGuardar,
                       onCancelar

                   }) {

    if (!mostrar) {
        return null;
    }

    return (

        <div
            className="modal d-block"
            tabIndex="-1"
            style={{
                backgroundColor: "rgba(0,0,0,0.5)"
            }}
        >

            {/* INICIO - Responsividad */}
            <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable">

                <div className="modal-content">

                    <div className="modal-header">

                        <h5 className="modal-title">

                            {modoEdicion
                                ? "Editar Rol"
                                : "Nuevo Rol"}

                        </h5>

                        <button
                            className="btn-close"
                            onClick={onCancelar}
                        ></button>

                    </div>

                    <div className="modal-body">

                        <label className="form-label">

                            Nombre del Rol

                        </label>

                        <input
                            type="text"
                            className="form-control"
                            value={nombreRol}
                            onChange={(e) =>
                                setNombreRol(e.target.value)
                            }
                        />

                    </div>

                    <div className="modal-footer">

                        <button
                            className="btn btn-secondary"
                            onClick={onCancelar}
                        >
                            Cancelar
                        </button>

                        <button
                            className="btn btn-success"
                            onClick={onGuardar}
                        >

                            {modoEdicion
                                ? "Actualizar"
                                : "Guardar"}

                        </button>

                    </div>

                </div>

            </div>
            {/* FIN - Responsividad */}

        </div>

    );

}

export default RoleModal;
