// Sesión del navegador: guarda el token y los datos del usuario mientras la pestaña esté abierta.
// sessionStorage puede fallar (modo privado, almacenamiento bloqueado), por eso todo va con try/catch.

const CLAVE = 'almibar.sesion';
let enMemoria = null;

export function guardarSesion(token, usuario) {
  enMemoria = { token, usuario };
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify(enMemoria));
  } catch {
    /* se queda solo en memoria */
  }
}

export function sesionActual() {
  if (enMemoria) return enMemoria;
  try {
    const guardada = sessionStorage.getItem(CLAVE);
    enMemoria = guardada ? JSON.parse(guardada) : null;
  } catch {
    enMemoria = null;
  }
  return enMemoria;
}

export function token() {
  return sesionActual()?.token ?? null;
}

export function esAdmin() {
  return sesionActual()?.usuario?.rol === 'ADMIN';
}

export function cerrarSesion() {
  enMemoria = null;
  try {
    sessionStorage.removeItem(CLAVE);
  } catch {
    /* nada que borrar */
  }
}
