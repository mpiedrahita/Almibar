// RNF-03: pruebas progresivas con 300, 500 y 700 usuarios.
// Objetivo: >= 99 % de solicitudes exitosas con 300 usuarios y >= 98 % con 500.
// Ejecutar un escenario:  k6 run -e USUARIOS=300 carga/escalabilidad.js   (también 500 y 700)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, iniciarSesion, nombreAlAzar, paginaAlAzar } from './comun.js';

const USUARIOS = Number(__ENV.USUARIOS || 300);
const MAXIMO_FALLAS = { 300: 0.01, 500: 0.02, 700: 0.05 };

export const options = {
  scenarios: {
    escalabilidad: { executor: 'ramping-vus', startVUs: 0, stages: [
      { duration: '2m', target: USUARIOS },   // calentamiento
      { duration: '15m', target: USUARIOS },  // carga sostenida
      { duration: '1m', target: 0 },
    ] },
  },
  thresholds: {
    http_req_failed: [`rate<${MAXIMO_FALLAS[USUARIOS] ?? 0.05}`],
  },
};

export function setup() {
  return { cabeceras: iniciarSesion() };
}

export default function ({ cabeceras }) {
  const r1 = http.get(`${BASE}/api/productos?page=${paginaAlAzar(250)}&size=20`, { headers: cabeceras });
  const r2 = http.get(`${BASE}/api/productos?nombre=${nombreAlAzar()}`, { headers: cabeceras });
  check(r1, { 'listado 200': (r) => r.status === 200 });
  check(r2, { 'búsqueda 200': (r) => r.status === 200 });
  sleep(1);
}
