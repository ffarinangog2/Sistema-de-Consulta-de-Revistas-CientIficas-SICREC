import { useRef, useState } from "react";
import FotoPerfil, { FOTO_PERFIL_ACTUALIZADA } from "./FotoPerfil";
import ConfirmDeleteModal from "./ConfirmDeleteModal";
import { eliminarFotoPerfil, guardarFotoPerfil } from "../services/fotoPerfilService";
import { useNotification } from "../hooks/useNotification";

function FotoPerfilEditor({ nombre }) {
    const { notify } = useNotification();
    const inputRef = useRef(null);
    const [tieneFoto, setTieneFoto] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [confirmarEliminacion, setConfirmarEliminacion] = useState(false);

    const seleccionar = async (evento) => {
        const archivo = evento.target.files?.[0];
        evento.target.value = "";
        if (!archivo) return;
        if (!['image/jpeg', 'image/png', 'image/webp'].includes(archivo.type)) {
            notify("Seleccione una imagen JPG, PNG o WebP.", "warning");
            return;
        }
        if (archivo.size > 5 * 1024 * 1024) {
            notify("La imagen no puede superar los 5 MB.", "warning");
            return;
        }
        try {
            setGuardando(true);
            await guardarFotoPerfil(archivo);
            window.dispatchEvent(new Event(FOTO_PERFIL_ACTUALIZADA));
            notify(tieneFoto ? "Foto de perfil actualizada." : "Foto de perfil agregada.", "success");
        } catch (error) {
            notify(error.response?.data?.detail || "No fue posible guardar la foto de perfil.", "danger");
        } finally {
            setGuardando(false);
        }
    };

    const eliminar = async () => {
        try {
            setGuardando(true);
            await eliminarFotoPerfil();
            setConfirmarEliminacion(false);
            window.dispatchEvent(new Event(FOTO_PERFIL_ACTUALIZADA));
            notify("Foto de perfil eliminada.", "success");
        } catch (error) {
            notify(error.response?.data?.detail || "No fue posible eliminar la foto de perfil.", "danger");
        } finally {
            setGuardando(false);
        }
    };

    return (
        <div className="foto-perfil-editor">
            <FotoPerfil nombre={nombre} grande onEstado={setTieneFoto} />
            <div className="d-flex flex-wrap justify-content-center gap-2 mt-2">
                <button type="button" className="btn btn-sm btn-outline-primary" disabled={guardando} onClick={() => inputRef.current?.click()}>
                    {tieneFoto ? "Cambiar foto" : "Agregar foto"}
                </button>
                {tieneFoto && <button type="button" className="btn btn-sm btn-outline-danger" disabled={guardando} onClick={() => setConfirmarEliminacion(true)}>Eliminar foto</button>}
            </div>
            <input ref={inputRef} className="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" onChange={seleccionar} />
            <ConfirmDeleteModal mostrar={confirmarEliminacion} titulo="Eliminar foto de perfil" mensaje="¿Está seguro de que desea eliminar su foto de perfil?" onCancelar={() => setConfirmarEliminacion(false)} onConfirmar={eliminar} textoConfirmar="Eliminar foto" />
        </div>
    );
}

export default FotoPerfilEditor;
