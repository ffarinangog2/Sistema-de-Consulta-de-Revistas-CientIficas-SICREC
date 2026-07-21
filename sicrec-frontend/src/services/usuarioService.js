import axios from "axios";

//const API = "http://localhost:8080/api/usuarios";
const API = "/api/usuarios";
export const listarUsuarios = () =>
    axios.get(API);

export const crearUsuario = (usuario) =>
    axios.post(API, usuario);

export const actualizarUsuario = (id, usuario) =>
    axios.put(`${API}/${id}`, usuario);

export const eliminarUsuario = (id) =>
    axios.delete(`${API}/${id}`);

export const registrarUsuario = (usuario) =>
    axios.post(API,usuario);