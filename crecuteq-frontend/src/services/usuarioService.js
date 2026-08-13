import axios from "axios";

//const API = "http://localhost:8080/api/usuarios";
const API = "/api/usuarios";
const autenticacion = () => {
    const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
    return { headers: { Authorization: `Bearer ${usuario?.token || ""}` } };
};
export const listarUsuarios = () =>
    axios.get(API);

export const crearUsuario = (usuario) =>
    axios.post(API, usuario);

export const actualizarUsuario = (id, usuario) =>
    axios.put(`${API}/${id}`, usuario, autenticacion());

export const eliminarUsuario = (id) =>
    axios.delete(`${API}/${id}`, autenticacion());

export const registrarUsuario = (usuario) =>
    axios.post(API,usuario);
