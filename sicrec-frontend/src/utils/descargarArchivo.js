// INICIO - Exportación reutilizable
export const descargarArchivo = (archivo, nombre) => {
    const url = window.URL.createObjectURL(new Blob([archivo]));
    const link = document.createElement("a");

    link.href = url;
    link.setAttribute("download", nombre);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
};
// FIN - Exportación reutilizable
