import { useEffect, useState } from "react";

import {
    listarUsuarios,
    registrarUsuario,
    actualizarUsuario
} from "../services/usuarioService";

import axios from "axios";


import MainLayout from "../layouts/MainLayout";
// INICIO - Notificaciones globales
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
import { googleScholarValido, orcidValido } from "../utils/perfilAcademicoValidation";
// FIN - Notificaciones globales

function Usuarios() {

    const usuarioActual = JSON.parse(localStorage.getItem("usuario") || "null");
    const esCuentaActual = (usuario) => Number(usuario?.id) === Number(usuarioActual?.id);

    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const [usuarios, setUsuarios] = useState([]);

    const [busqueda, setBusqueda] = useState("");
    const [paginaActual, setPaginaActual] = useState(1);

    const usuariosPorPagina = 5;

    const [mostrarModal, setMostrarModal] = useState(false);

    const abrirModal = () => setMostrarModal(true);

    const cerrarModal = () => {

        setMostrarModal(false);

        setModoEdicion(false);
        setIdUsuario(null);

        setNombreCompleto("");
        setCorreoInstitucional("");

        setRolId("");
        setCargoId("");
        setEstado(true);
        setOrcid("");
        setGoogleScholar("");

    };

    const [nombreCompleto, setNombreCompleto] = useState("");
    const [correoInstitucional, setCorreoInstitucional] = useState("");

    const [rolId, setRolId] = useState("");
    const [cargoId, setCargoId] = useState("");
    const [estado, setEstado] = useState(true);
    const [orcid, setOrcid] = useState("");
    const [googleScholar, setGoogleScholar] = useState("");
    // INICIO - Validación de correo institucional
    const correoNormalizado = correoInstitucional.trim().toLowerCase();
    const correoInstitucionalValido = /^[^\s@]+@uteq\.edu\.ec$/.test(correoNormalizado);
    // FIN - Validación de correo institucional

    const [roles, setRoles] = useState([]);
    const [cargos, setCargos] = useState([]);
    const [modoEdicion, setModoEdicion] = useState(false);
    const [idUsuario, setIdUsuario] = useState(null);
    // INICIO - Activar y desactivar usuario
    const [mostrarCambioEstado, setMostrarCambioEstado] = useState(false);
    const [usuarioCambioEstado, setUsuarioCambioEstado] = useState(null);
    const [cambiandoEstado, setCambiandoEstado] = useState(false);
    // FIN - Activar y desactivar usuario
    const [guardando, setGuardando] = useState(false);

    const usuariosFiltrados = usuarios.filter((usuario) => {

        const texto = busqueda.toLowerCase();

        return (

            usuario.nombreCompleto.toLowerCase().includes(texto) ||

            usuario.correoInstitucional.toLowerCase().includes(texto) ||

            usuario.rol.toLowerCase().includes(texto)

        );

    });

    const indiceUltimoUsuario = paginaActual * usuariosPorPagina;

    const indicePrimerUsuario = indiceUltimoUsuario - usuariosPorPagina;

    const usuariosPaginados = usuariosFiltrados.slice(
        indicePrimerUsuario,
        indiceUltimoUsuario
    );

    const totalPaginas = Math.ceil(
        usuariosFiltrados.length / usuariosPorPagina
    );

    useEffect(() => {
        let activo = true;

        Promise.all([listarUsuarios(), axios.get("/api/roles"), axios.get("/api/cargos")])
            .then(([respuestaUsuarios, respuestaRoles, respuestaCargos]) => {
                if (!activo) return;
                setUsuarios(respuestaUsuarios.data);
                setRoles(respuestaRoles.data);
                setCargos(respuestaCargos.data);
            })
            .catch((error) => {
                console.error(error);
                if (activo) notify("Error al cargar los datos de usuarios", "danger");
            });

        return () => { activo = false; };
    }, [notify]);

    const cargarUsuarios = async () => {

        try {

            const respuesta = await listarUsuarios();

            setUsuarios(respuesta.data);

        } catch (error) {

            console.error(error);
            notify("Error al cargar los usuarios", "danger");

        }

    };

    const guardarUsuario = async () => {

        if (
            !nombreCompleto ||
            !correoInstitucional ||
            !rolId ||
            !cargoId
        ) {
            notify("Complete todos los campos.", "warning");
            return;
        }

        // INICIO - Validación de correo institucional
        if (!correoInstitucionalValido) {
            notify(
                "El correo institucional debe finalizar en @uteq.edu.ec.",
                "warning"
            );
            return;
        }

        if (!orcidValido(orcid)) {
            notify("Ingrese un ORCID válido, por ejemplo https://orcid.org/0000-0002-1825-0097.", "warning");
            return;
        }

        if (!googleScholarValido(googleScholar)) {
            notify("Ingrese una URL válida de Google Scholar que comience con https://scholar.google.com/.", "warning");
            return;
        }
        // FIN - Validación de correo institucional

        const usuario = {

            nombreCompleto,
            correoInstitucional: correoNormalizado,
            estado,

            rol: {
                id: Number(rolId)
            },

            cargo: {
                id: Number(cargoId)
            }

        };



        try {

            setGuardando(true);

            if (modoEdicion) {

                await actualizarUsuario(
                    idUsuario,
                    usuario
                );

                notify("Usuario actualizado correctamente.", "success");

            } else {

                const registro = {
                    nombreCompleto,
                    correoInstitucional: correoNormalizado,
                    rolId: Number(rolId),
                    cargoId: Number(cargoId),
                    orcid: orcid.trim(),
                    googleScholar: googleScholar.trim()
                };

                await registrarUsuario(registro);

                notify(
                    "Usuario creado correctamente. El usuario y la contraseña temporal fueron enviados al correo institucional.",
                    "success"
                );

            }

            cerrarModal();

            setModoEdicion(false);
            setIdUsuario(null);

            setNombreCompleto("");
            setCorreoInstitucional("");

            setRolId("");
            setCargoId("");
            setEstado(true);
            setOrcid("");
            setGoogleScholar("");

            await cargarUsuarios();

        } catch (error) {

            console.error(error);

            const respuestaError = error.response?.data;
            notify(
                (typeof respuestaError === "string"
                    ? respuestaError
                    : respuestaError?.message) || "Ocurrió un error.",
                "danger"
            );

        } finally {

            setGuardando(false);

        }

    };

    const editarUsuario = (usuario) => {

        if (esCuentaActual(usuario)) {
            notify("Edite sus datos personales desde Mi perfil.", "warning");
            return;
        }

        setModoEdicion(true);

        setIdUsuario(usuario.id);

        setNombreCompleto(usuario.nombreCompleto);
        setCorreoInstitucional(usuario.correoInstitucional);


        const rolSeleccionado = roles.find(
            (r) => r.nombreRol === usuario.rol
        );

        if (rolSeleccionado) {
            setRolId(rolSeleccionado.id);
        }

        const cargoSeleccionado = cargos.find(
            (c) => c.nombreCargo === usuario.cargo
        );

        if (cargoSeleccionado) {
            setCargoId(cargoSeleccionado.id);
        }

        setEstado(usuario.estado);

        abrirModal();

    };
    // INICIO - Activar y desactivar usuario
    const abrirCambioEstado = (usuario) => {
        if (esCuentaActual(usuario)) {
            notify("No puede desactivar la cuenta actualmente autenticada.", "warning");
            return;
        }
        setUsuarioCambioEstado(usuario);
        setMostrarCambioEstado(true);
    };

    const cerrarCambioEstado = () => {
        setMostrarCambioEstado(false);
        setUsuarioCambioEstado(null);
    };

    const confirmarCambioEstado = async () => {

        if (!usuarioCambioEstado) return;

        const rolSeleccionado = roles.find(
            (rol) => rol.nombreRol === usuarioCambioEstado.rol
        );
        const cargoSeleccionado = cargos.find(
            (cargo) => cargo.nombreCargo === usuarioCambioEstado.cargo
        );

        if (!rolSeleccionado) {
            notify("No se pudo identificar el rol del usuario.", "danger");
            return;
        }

        const nuevoEstado = !usuarioCambioEstado.estado;

        try {
            setCambiandoEstado(true);

            await actualizarUsuario(usuarioCambioEstado.id, {
                nombreCompleto: usuarioCambioEstado.nombreCompleto,
                correoInstitucional: usuarioCambioEstado.correoInstitucional,
                estado: nuevoEstado,
                rol: { id: rolSeleccionado.id },
                cargo: cargoSeleccionado ? { id: cargoSeleccionado.id } : null
            });

            setUsuarios((usuariosActuales) =>
                usuariosActuales.map((usuario) =>
                    usuario.id === usuarioCambioEstado.id
                        ? { ...usuario, estado: nuevoEstado }
                        : usuario
                )
            );

            notify(
                "Usuario " + (nuevoEstado ? "activado" : "desactivado") + " correctamente.",
                "success"
            );
            cerrarCambioEstado();

        } catch (error) {
            console.error(error);
            const respuestaError = error.response?.data;
            notify(
                (typeof respuestaError === "string"
                    ? respuestaError
                    : respuestaError?.message) ||
                    "No se pudo cambiar el estado del usuario.",
                "danger"
            );
        } finally {
            setCambiandoEstado(false);
        }

    };
    // FIN - Activar y desactivar usuario

    // INICIO - Estado visual del usuario
    const usuarioBloqueado = (usuario) =>
        usuario.cuentaBloqueada === true ||
        usuario.bloqueadoTemporalmente === true ||
        Boolean(usuario.fechaFinBloqueo);

    const insigniaEstado = (usuario) => {
        if (usuarioBloqueado(usuario)) {
            return <span className="badge bg-warning text-dark">Bloqueado temporalmente</span>;
        }

        return usuario.estado
            ? <span className="badge bg-success">Activo</span>
            : <span className="badge bg-secondary">Inactivo</span>;
    };
    // FIN - Estado visual del usuario
    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Gestión Usuarios */}
            <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mb-4">

                <div>
                    <h2 className="mb-1">Gestión de Usuarios</h2>
                    <p className="text-muted">Administre las cuentas y su estado dentro del sistema.</p>
                </div>

                <button
                    className="btn btn-success"
                    onClick={abrirModal}
                >
                    + Nuevo Usuario
                </button>

            </div>

            <div className="card shadow-sm border-0 mb-3">
                <div className="card-body">
                <label className="form-label fw-semibold" htmlFor="buscar-usuario">Buscar usuario</label>

                <input
                    id="buscar-usuario"
                    type="text"
                    className="form-control"
                    placeholder="Buscar por nombre, correo o rol..."
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

                            <th>Nombre</th>
                            <th>Correo</th>
                            <th>Rol</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                            <th>Perfiles académicos</th>

                        </tr>

                        </thead>

                        <tbody>

                        {usuariosPaginados.map((usuario) => (

                            <tr key={usuario.id}>

                                <td>{usuario.nombreCompleto}{esCuentaActual(usuario) && (
                                    <span className="badge bg-primary ms-2">Cuenta actual</span>
                                )}</td>

                                <td>{usuario.correoInstitucional}</td>

                                <td>{usuario.rol}</td>

                                <td>
                                    {/* INICIO - Estado visual del usuario */}
                                    {insigniaEstado(usuario)}
                                    {/* FIN - Estado visual del usuario */}
                                </td>

                                <td className="text-nowrap">

                                    <div className="d-flex flex-wrap gap-2">
                                    <button
                                        className="btn btn-warning btn-sm"
                                        onClick={() => editarUsuario(usuario)}
                                        disabled={esCuentaActual(usuario)}
                                        title={esCuentaActual(usuario) ? "Edite sus datos desde Mi perfil" : "Editar usuario"}
                                    >
                                        Editar
                                    </button>

                                    {/* INICIO - Activar y desactivar usuario */}
                                    <button
                                        className={`btn btn-sm ${
                                            usuario.estado ? "btn-danger" : "btn-success"
                                        }`}
                                        onClick={() => abrirCambioEstado(usuario)}
                                        disabled={esCuentaActual(usuario)}
                                        title={esCuentaActual(usuario) ? "No puede desactivar su cuenta actual" : "Cambiar estado"}
                                    >
                                        {usuario.estado ? "Desactivar" : "Activar"}
                                    </button>
                                    {/* FIN - Activar y desactivar usuario */}
                                    </div>

                                </td>

                                <td className="text-nowrap">
                                    <div className="d-flex flex-wrap gap-2">
                                        {usuario.orcid && (
                                            <a className="btn btn-outline-success btn-sm" href={usuario.orcid} target="_blank" rel="noopener noreferrer">ORCID</a>
                                        )}
                                        {usuario.googleScholar && (
                                            <a className="btn btn-outline-primary btn-sm" href={usuario.googleScholar} target="_blank" rel="noopener noreferrer">Google Scholar</a>
                                        )}
                                        {!usuario.orcid && !usuario.googleScholar && <span className="text-muted">—</span>}
                                    </div>
                                </td>

                            </tr>

                        ))}

                        </tbody>

                    </table>
                    </div>

                </div>

            </div>


            <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2 mt-3">

    <span>

        Mostrando

        {" "}

        {usuariosFiltrados.length === 0
            ? 0
            : indicePrimerUsuario + 1}

        {" - "}

        {Math.min(
            indiceUltimoUsuario,
            usuariosFiltrados.length
        )}

        {" de "}

        {usuariosFiltrados.length}

        {" usuarios"}

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
                        disabled={paginaActual === totalPaginas || totalPaginas === 0}
                        onClick={() =>
                            setPaginaActual(paginaActual + 1)
                        }
                    >
                        Siguiente
                    </button>

                </div>

            </div>
            {/* FIN - Mejora Gestión Usuarios */}
            {mostrarModal && (

                <div
                    className="modal d-block"
                    tabIndex="-1"
                    style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
                >

                    <div className="modal-dialog modal-lg modal-dialog-scrollable">

                        <div className="modal-content">

                            <div className="modal-header">

                                <h5>

                                    {modoEdicion
                                        ? "Editar Usuario"
                                        : "Nuevo Usuario"}

                                </h5>

                                <button
                                    className="btn-close"
                                    onClick={cerrarModal}
                                ></button>

                            </div>

                            <div className="modal-body">

                                <div className="mb-3">

                                    <label className="form-label">
                                        Nombre Completo
                                    </label>

                                    <input
                                        type="text"
                                        className="form-control"
                                        value={nombreCompleto}
                                        onChange={(e) => setNombreCompleto(e.target.value)}
                                    />

                                </div>

                                <div className="mb-3">

                                    <label className="form-label">
                                        Correo Institucional
                                    </label>

                                    <input
                                        type="email"
                                        className={`form-control ${
                                            correoInstitucional
                                                ? correoInstitucionalValido
                                                    ? "is-valid"
                                                    : "is-invalid"
                                                : ""
                                        }`}
                                        value={correoInstitucional}
                                        onChange={(e) => setCorreoInstitucional(e.target.value)}
                                    />

                                    {/* INICIO - Validación de correo institucional */}
                                    {correoInstitucional && !correoInstitucionalValido && (
                                        <div className="invalid-feedback">
                                            El correo debe finalizar en @uteq.edu.ec.
                                        </div>
                                    )}
                                    {/* FIN - Validación de correo institucional */}

                                </div>

                                {!modoEdicion && (
                                    <div className="row g-3 mb-3">
                                        <div className="col-md-6">
                                            <label className="form-label" htmlFor="nuevo-usuario-orcid">ORCID <span className="form-text">(opcional)</span></label>
                                            <input
                                                id="nuevo-usuario-orcid"
                                                type="url"
                                                className={`form-control ${orcid && !orcidValido(orcid) ? "is-invalid" : ""}`}
                                                placeholder="https://orcid.org/0000-0002-1825-0097"
                                                value={orcid}
                                                onChange={(e) => setOrcid(e.target.value)}
                                            />
                                            {orcid && !orcidValido(orcid) && <div className="invalid-feedback">Ingrese una URL ORCID válida.</div>}
                                        </div>
                                        <div className="col-md-6">
                                            <label className="form-label" htmlFor="nuevo-usuario-scholar">Google Scholar <span className="form-text">(opcional)</span></label>
                                            <input
                                                id="nuevo-usuario-scholar"
                                                type="url"
                                                className={`form-control ${googleScholar && !googleScholarValido(googleScholar) ? "is-invalid" : ""}`}
                                                placeholder="https://scholar.google.com/citations?user=..."
                                                value={googleScholar}
                                                onChange={(e) => setGoogleScholar(e.target.value)}
                                            />
                                            {googleScholar && !googleScholarValido(googleScholar) && <div className="invalid-feedback">Ingrese una URL válida de Google Scholar.</div>}
                                        </div>
                                    </div>
                                )}


                                {/* INICIO - Responsividad */}
                                <div className="row g-3">

                                    <div className="col-md-6">

                                        <label className="form-label">
                                            Rol
                                        </label>

                                        <select
                                            className="form-select"
                                            value={rolId}
                                            onChange={(e) => setRolId(e.target.value)}
                                        >

                                            <option value="">
                                                Seleccione un rol
                                            </option>

                                            {roles.map((rol) => (

                                                <option
                                                    key={rol.id}
                                                    value={rol.id}
                                                >
                                                    {rol.nombreRol}
                                                </option>

                                            ))}

                                        </select>

                                    </div>

                                    <div className="col-md-6">

                                        <label className="form-label">
                                            Cargo
                                        </label>

                                        <select
                                            className="form-select"
                                            value={cargoId}
                                            onChange={(e) => setCargoId(e.target.value)}
                                        >

                                            <option value="">
                                                Seleccione un cargo
                                            </option>

                                            {cargos.map((cargo) => (

                                                <option
                                                    key={cargo.id}
                                                    value={cargo.id}
                                                >
                                                    {cargo.nombreCargo}
                                                </option>

                                            ))}

                                        </select>

                                    </div>

                                </div>
                                {/* FIN - Responsividad */}

                                {/* INICIO - Activar y desactivar usuario */}
                                {modoEdicion && (
                                    <div className="mt-3">
                                        <label className="form-label d-block">Estado</label>
                                        {estado
                                            ? <span className="badge bg-success">Activo</span>
                                            : <span className="badge bg-secondary">Inactivo</span>}
                                        <div className="form-text">
                                            Utilice la acción Activar o Desactivar del listado para cambiar el estado.
                                        </div>
                                    </div>
                                )}
                                {/* FIN - Activar y desactivar usuario */}

                            </div>

                            <div className="modal-footer">

                                <button
                                    className="btn btn-secondary"
                                    onClick={cerrarModal}
                                >
                                    Cancelar
                                </button>

                                <LoadingButton
                                    className="btn btn-success"
                                    onClick={guardarUsuario}
                                    loading={guardando}
                                    loadingText={modoEdicion ? "Actualizando..." : "Guardando..."}
                                >
                                    {modoEdicion ? "Actualizar" : "Guardar"}
                                </LoadingButton>

                            </div>

                        </div>

                    </div>



                </div>

            )}
            {/* INICIO - Activar y desactivar usuario */}
            {mostrarCambioEstado && usuarioCambioEstado && (

                <div
                    className="modal d-block"
                    tabIndex="-1"
                    style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
                >

                    <div className="modal-dialog">

                        <div className="modal-content">

                            <div className="modal-header">

                                <h5 className="modal-title">
                                    Confirmar cambio de estado
                                </h5>

                                <button
                                    className="btn-close"
                                    onClick={cerrarCambioEstado}
                                ></button>

                            </div>

                            <div className="modal-body">

                                <p>
                                    ¿Está seguro de {usuarioCambioEstado.estado
                                        ? "desactivar"
                                        : "activar"} a {usuarioCambioEstado.nombreCompleto}?
                                </p>

                            </div>

                            <div className="modal-footer">

                                <button
                                    className="btn btn-secondary"
                                    onClick={cerrarCambioEstado}
                                >
                                    Cancelar
                                </button>

                                <LoadingButton
                                    className={`btn ${
                                        usuarioCambioEstado.estado
                                            ? "btn-danger"
                                            : "btn-success"
                                    }`}
                                    onClick={confirmarCambioEstado}
                                    loading={cambiandoEstado}
                                    loadingText={usuarioCambioEstado.estado
                                        ? "Desactivando..."
                                        : "Activando..."}
                                >
                                    {usuarioCambioEstado.estado ? "Desactivar" : "Activar"}
                                </LoadingButton>

                            </div>

                        </div>

                    </div>

                </div>

            )}
            {/* FIN - Activar y desactivar usuario */}
        </MainLayout>

    );

}

export default Usuarios;
