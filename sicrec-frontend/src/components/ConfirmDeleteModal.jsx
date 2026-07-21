function ConfirmDeleteModal({ mostrar, titulo, mensaje, onCancelar, onConfirmar }) {

    if (!mostrar) {
        return null;
    }

    return (
        <div
            className="modal d-block"
            tabIndex="-1"
            style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
        >
            {/* INICIO - Responsividad */}
            <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable">
                <div className="modal-content">

                    <div className="modal-header">
                        <h5 className="modal-title">{titulo}</h5>

                        <button
                            className="btn-close"
                            onClick={onCancelar}
                        ></button>
                    </div>

                    <div className="modal-body">
                        <p>{mensaje}</p>
                    </div>

                    <div className="modal-footer">
                        <button
                            className="btn btn-secondary"
                            onClick={onCancelar}
                        >
                            Cancelar
                        </button>

                        <button
                            className="btn btn-danger"
                            onClick={onConfirmar}
                        >
                            Eliminar
                        </button>
                    </div>

                </div>
            </div>
            {/* FIN - Responsividad */}
        </div>
    );
}

export default ConfirmDeleteModal;
