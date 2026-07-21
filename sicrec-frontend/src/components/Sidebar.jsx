import { Link } from "react-router-dom";

function Sidebar({ admin, abierto = false, onCerrar }) {

    return (

        <>
        {/* INICIO - Sidebar fijo */}
        <aside
            className={`sidebar bg-dark text-white p-3 ${abierto ? "sidebar--abierto" : ""}`}
            style={{
                width: "260px",
                maxWidth: "85vw"
            }}
            onClick={(evento) => evento.target.closest("a") && onCerrar?.()}
        >

            <button type="button" className="btn-close btn-close-white sidebar__cerrar d-lg-none" aria-label="Cerrar menú" onClick={onCerrar} />

            <h3 className="text-center mb-4">
                SICREC
            </h3>

            <ul className="nav flex-column">

                <Link
                    to={admin ? "/admin/dashboard" : "/usuario/dashboard"}
                    className="nav-link text-white"
                >
                    🏠 Inicio
                </Link>

                {admin ? (
                    <>

                        <li className="nav-item mb-2">
                            <Link
                                to="/admin/usuarios"
                                className="nav-link text-white"
                            >
                                👥 Usuarios
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            <Link
                                to="/admin/roles"
                                className="nav-link text-white"
                            >
                                🎓 Roles
                            </Link>
                        </li>



                        <li className="nav-item mb-2">
                            <Link
                                to="/admin/cargos"
                                className="nav-link text-white"
                            >
                                💼 Cargos
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            <Link
                                to="/admin/reportes"
                                className="nav-link text-white"
                            >
                                📊 Reportes
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            {/* INICIO - Auditoría */}
                            <Link
                                to="/admin/auditoria"
                                className="nav-link text-white"
                            >
                                📋 Auditoría
                            </Link>
                            {/* FIN - Auditoría */}
                        </li>

                    </>
                ) : (


                    <>
                        

                        <li className="nav-item mb-2">
                            <Link
                                to="/usuario/revistas"
                                className="nav-link text-white"
                            >
                                🔎 Buscar Revistas
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            <Link
                                to="/usuario/favoritos"
                                className="nav-link text-white"
                            >
                                ⭐ Mis Favoritos
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            <Link
                                to="/usuario/historial"
                                className="nav-link text-white"
                            >
                                📚 Historial
                            </Link>
                        </li>

                        <li className="nav-item mb-2">
                            <Link
                                to="/usuario/ayuda"
                                className="nav-link text-white"
                            >
                                ❓ Ayuda
                            </Link>
                        </li>
                    </>


                )}

                <li className="nav-item mb-2">
                    <Link
                        to={admin ? "/admin/perfil" : "/usuario/perfil"}
                        className="nav-link text-white"
                    >
                        👤 Perfil
                    </Link>
                </li>

                <li className="nav-item mt-4">
                    <Link
                        to="/login"
                        className="btn btn-danger w-100"
                    >
                        Cerrar Sesión
                    </Link>
                </li>

            </ul>

        </aside>

        {abierto && <button type="button" className="sidebar__backdrop d-lg-none" aria-label="Cerrar menú" onClick={onCerrar} />}
        {/* FIN - Sidebar fijo */}
        </>

    );

}

export default Sidebar;
