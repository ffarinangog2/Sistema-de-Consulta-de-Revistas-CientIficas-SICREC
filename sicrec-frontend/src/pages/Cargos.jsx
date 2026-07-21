import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import RoleModal from "../components/RoleModal";
import ConfirmDeleteModal from "../components/ConfirmDeleteModal";
// INICIO - Notificaciones globales
import { useNotification } from "../hooks/useNotification";
// FIN - Notificaciones globales

import {
    listarCargos,
    crearCargo,
    actualizarCargo,
    eliminarCargo
} from "../services/cargoService";

function Cargos() {

    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const [cargos, setCargos] = useState([]);
    const [busqueda, setBusqueda] = useState("");

    const [mostrarModal, setMostrarModal] = useState(false);
    const [mostrarEliminar, setMostrarEliminar] = useState(false);

    const [modoEdicion, setModoEdicion] = useState(false);

    const [nombreCargo, setNombreCargo] = useState("");

    const [idCargo, setIdCargo] = useState(null);

    const [paginaActual, setPaginaActual] = useState(1);

    const cargosPorPagina = 5;

    useEffect(() => {

        cargarCargos();

    }, []);

    const cargarCargos = async () => {

        try {

            const respuesta = await listarCargos();

            setCargos(respuesta.data);

        } catch (error) {

            console.error(error);

        }

    };

    const abrirModal = () => {

        setMostrarModal(true);

    };

    const cerrarModal = () => {

        setMostrarModal(false);

        setModoEdicion(false);

        setNombreCargo("");

        setIdCargo(null);

    };

    const guardarCargo = async () => {

        if (!nombreCargo.trim()) {

            notify("Ingrese el nombre del cargo.", "warning");

            return;

        }

        try {

            if (modoEdicion) {

                await actualizarCargo(idCargo, {
                    nombreCargo
                });

                notify("Cargo actualizado.", "success");

            } else {

                await crearCargo({
                    nombreCargo
                });

                notify("Cargo creado.", "success");

            }

            cerrarModal();

            cargarCargos();

        } catch (error) {

            console.error(error);

        }

    };

    const editarCargo = (cargo) => {

        setModoEdicion(true);

        setIdCargo(cargo.id);

        setNombreCargo(cargo.nombreCargo);

        abrirModal();

    };

    const abrirEliminar = (id) => {

        setIdCargo(id);

        setMostrarEliminar(true);

    };

    const confirmarEliminar = async () => {

        try {

            await eliminarCargo(idCargo);

            setMostrarEliminar(false);

            cargarCargos();

        } catch (error) {

            console.error(error);

        }

    };

    const cerrarEliminar = () => {

        setMostrarEliminar(false);

        setIdCargo(null);

    };

    const cargosFiltrados = cargos.filter((cargo) =>
        cargo.nombreCargo
            .toLowerCase()
            .includes(busqueda.toLowerCase())
    );

    const indiceFinal = paginaActual * cargosPorPagina;

    const indiceInicial = indiceFinal - cargosPorPagina;

    const cargosMostrar = cargosFiltrados.slice(
        indiceInicial,
        indiceFinal
    );

    const totalPaginas = Math.ceil(
        cargosFiltrados.length / cargosPorPagina
    );    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Gestión Cargos */}
            <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-4">

                <div>
                    <h2 className="mb-1">Gestión de Cargos</h2>
                    <p className="text-muted">Administre los cargos institucionales registrados.</p>
                </div>

                <button
                    className="btn btn-success"
                    onClick={abrirModal}
                >
                    + Nuevo Cargo
                </button>

            </div>

            <div className="card shadow-sm border-0 mb-3">
                <div className="card-body">
                <label className="form-label fw-semibold" htmlFor="buscar-cargo">Buscar cargo</label>

                <input
                    id="buscar-cargo"
                    type="text"
                    className="form-control"
                    placeholder="Buscar cargo..."
                    value={busqueda}
                    onChange={(e) => {

                        setBusqueda(e.target.value);

                        setPaginaActual(1);

                    }}
                />

                </div>
            </div>

            <div className="card shadow-sm border-0">

                <div className="card-body">

                    <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">

                        <thead>

                        <tr>

                            <th>ID</th>
                            <th>Nombre del Cargo</th>
                            <th>Acciones</th>

                        </tr>

                        </thead>

                        <tbody>

                        {cargosMostrar.map((cargo) => (

                            <tr key={cargo.id}>

                                <td>{cargo.id}</td>

                                <td>{cargo.nombreCargo}</td>

                                <td className="text-nowrap">

                                    <div className="d-flex flex-wrap gap-2">
                                    <button
                                        className="btn btn-warning btn-sm"
                                        onClick={() => editarCargo(cargo)}
                                    >
                                        Editar
                                    </button>

                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => abrirEliminar(cargo.id)}
                                    >
                                        Eliminar
                                    </button>
                                    </div>

                                </td>

                            </tr>

                        ))}

                        </tbody>

                    </table>
                    </div>

                    <div className="d-flex flex-column flex-md-row justify-content-between gap-2 mt-3">

                        <span>

                            Mostrando{" "}

                            {cargosFiltrados.length === 0
                                ? 0
                                : indiceInicial + 1}

                            {" - "}

                            {Math.min(
                                indiceFinal,
                                cargosFiltrados.length
                            )}

                            {" de "}

                            {cargosFiltrados.length}

                        </span>

                        <div className="d-flex flex-wrap align-items-center gap-2">

                            <button
                                className="btn btn-outline-primary btn-sm"
                                disabled={paginaActual === 1}
                                onClick={() =>
                                    setPaginaActual(paginaActual - 1)
                                }
                            >
                                Anterior
                            </button>

                            <span>

                                Página {paginaActual} de {totalPaginas || 1}

                            </span>

                            <button
                                className="btn btn-outline-primary btn-sm"
                                disabled={
                                    paginaActual === totalPaginas ||
                                    totalPaginas === 0
                                }
                                onClick={() =>
                                    setPaginaActual(paginaActual + 1)
                                }
                            >
                                Siguiente
                            </button>

                        </div>

                    </div>

                </div>

            </div>
            {/* FIN - Mejora Gestión Cargos */}

            <RoleModal

                mostrar={mostrarModal}

                modoEdicion={modoEdicion}

                nombreRol={nombreCargo}

                setNombreRol={setNombreCargo}

                onGuardar={guardarCargo}

                onCancelar={cerrarModal}

            />

            <ConfirmDeleteModal

                mostrar={mostrarEliminar}

                titulo="Eliminar Cargo"

                mensaje="¿Está seguro de eliminar este cargo?"

                onCancelar={cerrarEliminar}

                onConfirmar={confirmarEliminar}

            />

        </MainLayout>

    );

}

export default Cargos;
