// Funciones compartidas por las pruebas de carga (k6).
import http from 'k6/http';
import { check } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';

/** Inicia sesión una sola vez (en setup) y devuelve la cabecera con el token. */
export function iniciarSesion() {
  const usuario = __ENV.USUARIO || 'consulta';
  const contrasena = __ENV.CONTRASENA || 'Consulta.2026';
  const r = http.post(`${BASE}/api/auth/login`, JSON.stringify({ usuario, contrasena }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(r, { 'inicio de sesión correcto': (x) => x.status === 200 });
  return { Authorization: `Bearer ${r.json('token')}` };
}

const NOMBRES = ['torta', 'tarta', 'galleta', 'alfajor', 'cheesecake', 'rollo', 'leche', 'chocolate'];
export const nombreAlAzar = () => NOMBRES[Math.floor(Math.random() * NOMBRES.length)];
export const categoriaAlAzar = () => 1 + Math.floor(Math.random() * 5);
export const paginaAlAzar = (max = 20) => Math.floor(Math.random() * max);
