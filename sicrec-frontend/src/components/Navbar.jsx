import { useNavigate } from "react-router-dom";

function Navbar({ onAbrirSidebar }) {

    const navigate = useNavigate();

    const usuario = JSON.parse(localStorage.getItem("usuario"));

    const cerrarSesion = () => {

        localStorage.removeItem("usuario");
        navigate("/login");

    };

    return (

        // INICIO - Ajuste Responsive
        <nav
            className="navbar navbar-expand-lg shadow-sm"
            style={{
                backgroundColor: "#ffffff",
                borderBottom: "4px solid #1b7505"
            }}
        >

            <div className="container-fluid flex-nowrap gap-2">

                {/* INICIO - Responsividad */}
                <button type="button" className="btn btn-outline-secondary d-lg-none flex-shrink-0" aria-label="Abrir menú" onClick={onAbrirSidebar}>
                    ☰
                </button>
                {/* FIN - Responsividad */}

                <span
                    className="navbar-brand fw-bold"
                    style={{
                        color: "#1b7505",
                        fontSize: "24px"
                    }}
                >
                    SICREC
                </span>

                <div className="d-flex align-items-center gap-2 ms-auto min-w-0">

                    <div className="text-end d-none d-sm-block navbar__usuario">

                        <div className="fw-bold">
                            {usuario?.nombreCompleto}
                        </div>

                        <small className="text-muted">
                            {usuario?.rol}
                        </small>

                    </div>

                    <button
                        className="btn btn-outline-danger flex-shrink-0"
                        onClick={cerrarSesion}
                    >
                        Cerrar sesión
                    </button>

                </div>

            </div>

        </nav>
        // FIN - Ajuste Responsive

    );

}

export default Navbar;
