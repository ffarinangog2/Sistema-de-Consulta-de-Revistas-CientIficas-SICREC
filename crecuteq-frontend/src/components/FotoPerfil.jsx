import { useEffect, useState } from "react";
import { obtenerFotoPerfil } from "../services/fotoPerfilService";

export const FOTO_PERFIL_ACTUALIZADA = "crecuteq:foto-perfil-actualizada";

function FotoPerfil({ nombre = "Usuario", grande = false, onEstado }) {
    const [url, setUrl] = useState(null);
    const iniciales = nombre.split(" ").filter(Boolean).slice(0, 2)
        .map((parte) => parte[0]?.toUpperCase()).join("") || "SC";

    useEffect(() => {
        let activa = true;
        let objectUrl;

        const cargar = async () => {
            try {
                const blob = await obtenerFotoPerfil();
                if (!activa) return;
                objectUrl = URL.createObjectURL(blob);
                setUrl(objectUrl);
                onEstado?.(true);
            } catch (error) {
                if (error.response?.status !== 404) console.error(error);
                if (activa) {
                    setUrl(null);
                    onEstado?.(false);
                }
            }
        };

        cargar();
        const actualizar = () => cargar();
        window.addEventListener(FOTO_PERFIL_ACTUALIZADA, actualizar);
        return () => {
            activa = false;
            window.removeEventListener(FOTO_PERFIL_ACTUALIZADA, actualizar);
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [onEstado]);

    return url
        ? <img className={`foto-perfil ${grande ? "foto-perfil--grande" : ""}`} src={url} alt={`Foto de ${nombre}`} />
        : <span className={`foto-perfil foto-perfil--vacia ${grande ? "foto-perfil--grande" : ""}`} aria-label={`Sin foto de perfil para ${nombre}`}>{iniciales}</span>;
}

export default FotoPerfil;
