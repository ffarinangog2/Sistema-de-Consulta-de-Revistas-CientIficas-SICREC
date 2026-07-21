import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import RoleModal from "../components/RoleModal";
import ConfirmDeleteModal from "../components/ConfirmDeleteModal";
// INICIO - Notificaciones globales
import { useNotification } from "../hooks/useNotification";
// FIN - Notificaciones globales

import {
    listarRoles,
    crearRol,
    actualizarRol,
    eliminarRol
} from "../services/rolService";

function Roles() {

    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const [roles, setRoles] = useState([]);
    const [busqueda, setBusqueda] = useState("");

    const [mostrarModal, setMostrarModal] = useState(false);
    const [mostrarEliminar, setMostrarEliminar] = useState(false);

    const [modoEdicion, setModoEdicion] = useState(false);

    const [nombreRol, setNombreRol] = useState("");

    const [idRol, setIdRol] = useState(null);

    const [paginaActual, setPaginaActual] = useState(1);

    const rolesPorPagina = 5;

    useEffect(() => {

        cargarRoles();

    }, []);

    const cargarRoles = async () => {

        try {

            const respuesta = await listarRoles();

            setRoles(respuesta.data);

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

        setNombreRol("");

        setIdRol(null);

    };

    const guardarRol = async () => {

        if (!nombreRol.trim()) {

            notify("Ingrese el nombre del rol.", "warning");

            return;

        }

        try {

            if (modoEdicion) {

                await actualizarRol(idRol, {
                    nombreRol
                });

                notify("Rol actualizado.", "success");

            } else {

                await crearRol({
                    nombreRol
                });

                notify("Rol creado.", "success");

            }

            cerrarModal();

            cargarRoles();

        } catch (error) {

            console.error(error);

        }

    };

    const editarRol = (rol) => {

        setModoEdicion(true);

        setIdRol(rol.id);

        setNombreRol(rol.nombreRol);

        abrirModal();

    };

    const abrirEliminar = (id) => {

        setIdRol(id);

        setMostrarEliminar(true);

    };

    const confirmarEliminar = async () => {

        try {

            await eliminarRol(idRol);

            setMostrarEliminar(false);

            cargarRoles();

        } catch (error) {

            console.error(error);

        }

    };

    const cerrarEliminar = () => {

        setMostrarEliminar(false);

        setIdRol(null);

    };

    const rolesFiltrados = roles.filter((rol) =>
        rol.nombreRol
            .toLowerCase()
            .includes(busqueda.toLowerCase())
    );

    const indiceFinal = paginaActual * rolesPorPagina;

    const indiceInicial = indiceFinal - rolesPorPagina;

    const rolesMostrar = rolesFiltrados.slice(
        indiceInicial,
        indiceFinal
    );

    const totalPaginas = Math.ceil(
        rolesFiltrados.length / rolesPorPagina
    );
    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Gestión Roles */}
            <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-4">

                <div>
                    <h2 className="mb-1">Gestión de Roles</h2>
                    <p className="text-muted">Organice los perfiles de acceso disponibles.</p>
                </div>

                <button
                    className="btn btn-success"
                    onClick={abrirModal}
                >
                    + Nuevo Rol
                </button>

            </div>

            <div className="card shadow-sm border-0 mb-3">
                <div className="card-body">
                <label className="form-label fw-semibold" htmlFor="buscar-rol">Buscar rol</label>

                <input
                    id="buscar-rol"
                    type="text"
                    className="form-control"
                    placeholder="Buscar rol..."
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
                            <th>Nombre del Rol</th>
                            <th>Acciones</th>

                        </tr>

                        </thead>

                        <tbody>

                        {rolesMostrar.map((rol) => (

                            <tr key={rol.id}>

                                <td>{rol.id}</td>

                                <td>{rol.nombreRol}</td>

                                <td className="text-nowrap">

                                    <div className="d-flex flex-wrap gap-2">
                                    <button
                                        className="btn btn-warning btn-sm"
                                        onClick={() => editarRol(rol)}
                                    >
                                        Editar
                                    </button>

                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => abrirEliminar(rol.id)}
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

                            Mostrando

                            {" "}

                            {rolesFiltrados.length === 0
                                ? 0
                                : indiceInicial + 1}

                            {" - "}

                            {Math.min(
                                indiceFinal,
                                rolesFiltrados.length
                            )}

                            {" de "}

                            {rolesFiltrados.length}

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
            {/* FIN - Mejora Gestión Roles */}

            <RoleModal

                mostrar={mostrarModal}

                modoEdicion={modoEdicion}

                nombreRol={nombreRol}

                setNombreRol={setNombreRol}

                onGuardar={guardarRol}

                onCancelar={cerrarModal}

            />

            <ConfirmDeleteModal

                mostrar={mostrarEliminar}

                titulo="Eliminar Rol"

                mensaje="¿Está seguro de eliminar este rol?"

                onCancelar={cerrarEliminar}

                onConfirmar={confirmarEliminar}

            />

        </MainLayout>

    );

}

export default Roles;
