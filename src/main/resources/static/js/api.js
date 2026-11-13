// Comunicación con el controlador (API REST). La vista nunca toca la base de datos:
// todo pasa por aquí, con el token JWT en la cabecera Authorization.
import { cerrarSesion, token } from './sesion.js';

export class ErrorApi extends Error {
  constructor(estado, mensaje, { campos = [], codigo = null } = {}) {
    super(mensaje);
    this.estado = estado;
    this.campos = campos;
    this.codigo = codigo;
  }
}

/**
 * Hace una petición y devuelve el JSON de la respuesta.
 * opciones: { metodo, cuerpo (objeto JSON), formulario (FormData), publica (sin redirigir al login si es 401) }
 */
export async function pedir(ruta, { metodo = 'GET', cuerpo, formulario, publica = false } = {}) {
  const cabeceras = { Accept: 'application/json' };
  const t = token();
  if (t) cabeceras.Authorization = `Bearer ${t}`;
  let body;
  if (formulario) {
    body = formulario;
  } else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json';
    body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(ruta, { method: metodo, headers: cabeceras, body });
  } catch {
    throw new ErrorApi(0, 'No pudimos conectar con el servidor. Revisa que la aplicación esté corriendo e inténtalo de nuevo.');
  }

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      datos = null;
    }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && !publica) {
      cerrarSesion();
      window.location.replace('login.html?sesion=vencida');
    }
    throw new ErrorApi(respuesta.status, datos?.mensaje ?? 'Algo salió mal. Inténtalo de nuevo.', {
      campos: datos?.campos ?? [],
      codigo: datos?.codigo ?? null,
    });
  }
  return datos;
}
