import { useEffect, useState } from "react";
// INICIO - Mejoras Favoritos
import { useNavigate } from "react-router-dom";
// FIN - Mejoras Favoritos
import MainLayout from "../layouts/MainLayout";
import {
    obtenerFavoritos,
    eliminarFavorito
} from "../services/revistaService";
// INICIO - Mejoras UI/UX reutilizables
import { useNotification } from "../hooks/useNotification";
import LoadingButton from "../components/LoadingButton";
// FIN - Mejoras UI/UX reutilizables

function Favoritos() {

    // INICIO - Mejoras Favoritos
    const navigate = useNavigate();
    // FIN - Mejoras Favoritos

    const [favoritos, setFavoritos] = useState([]);
    // INICIO - Botones con estado de carga
    const [eliminandoId, setEliminandoId] = useState(null);
    // FIN - Botones con estado de carga
    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    // Antes: const usuarioId = 8; (valor fijo de prueba)
    // Ahora: se toma el usuario realmente logueado, igual que en
    // BuscarRevistas.jsx, así los favoritos guardados sí aparecen.
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    const usuarioId = usuario?.id;

    const cargarFavoritos = async () => {

        if (!usuarioId) {

            console.error(
                "cargarFavoritos(): no se pudo obtener un usuario válido desde localStorage.",
                usuario
            );

            return;

        }

        try {

            const data = await obtenerFavoritos(usuarioId);

            console.log("Favoritos:", data);

            setFavoritos(data);

        } catch (error) {

            console.error(error);

        }

    };

    useEffect(() => {

        cargarFavoritos();

    }, []);

    const eliminar = async (id) => {

        if (!window.confirm("¿Eliminar este favorito?")) {
            return;
        }

        try {
            setEliminandoId(id);
            await eliminarFavorito(id);
            notify("Favorito eliminado correctamente.", "success");
            await cargarFavoritos();
        } catch (error) {
            console.error(error);
            notify("No se pudo eliminar el favorito.", "danger");
        } finally {
            setEliminandoId(null);
        }

    };

    // INICIO - Mejoras Favoritos
    const buscarNuevamente = (favorito) => {
        navigate("/usuario/revistas", {
            state: { termino: favorito.titulo || favorito.revista }
        });
    };

    const obtenerEnlaceScopus = (favorito) => favorito.enlaceScopus
        || (favorito.sourceId ? `https://www.scopus.com/sourceid/${encodeURIComponent(favorito.sourceId)}` : null);
    // FIN - Mejoras Favoritos

    return (

        <MainLayout admin={false}>

            <h2 className="mb-4">
                Mis Favoritos
            </h2>

            {favoritos.length === 0 ? (

                <div className="alert alert-info">

                    No tienes favoritos guardados.

                </div>

            ) : (

                <>
                {/* INICIO - Responsividad */}
                <div className="row g-4">

                    {favoritos.map((f) => (

                        <div className="col-12 col-lg-6" key={f.id}>

                            <div className="card shadow-sm">

                                <div className="card-body">

                                    <h5>{f.titulo}</h5>

                                    <p>
                                        <strong>Revista:</strong> {f.revista}
                                    </p>

                                    <p>
                                        <strong>ISSN:</strong> {f.issn}
                                    </p>

                                    <p>
                                        <strong>Cuartil:</strong> {f.cuartil}
                                    </p>

                                    <p>
                                        <strong>Año:</strong> {f.anio}
                                    </p>

                                    {/* INICIO - Mejoras Favoritos */}
                                    <div className="d-flex flex-column flex-sm-row flex-wrap gap-2 mt-3">
                                    <button
                                        type="button"
                                        className="btn btn-primary"
                                        onClick={() => buscarNuevamente(f)}
                                    >
                                        Buscar nuevamente
                                    </button>

                                    {obtenerEnlaceScopus(f) && (
                                        <a
                                            className="btn btn-outline-primary"
                                            href={obtenerEnlaceScopus(f)}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            Ver en Scopus
                                        </a>
                                    )}

                                    <LoadingButton
                                        className="btn btn-danger"
                                        onClick={() => eliminar(f.id)}
                                        loading={eliminandoId === f.id}
                                        loadingText="Eliminando..."
                                        disabled={eliminandoId !== null}
                                    >
                                        Eliminar
                                    </LoadingButton>
                                    </div>
                                    {/* FIN - Mejoras Favoritos */}

                                </div>

                            </div>

                        </div>

                    ))}

                </div>
                {/* FIN - Responsividad */}
                </>

            )}

        </MainLayout>

    );

}

export default Favoritos;
