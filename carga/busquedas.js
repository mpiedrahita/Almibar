// RNF-02: con 300 usuarios concurrentes, el p95 de las búsquedas por nombre, categoría o disponibilidad debe ser <= 2,5 s.
// Ejecutar: k6 run carga/busquedas.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, categoriaAlAzar, iniciarSesion, nombreAlAzar } from './comun.js';

export const options = {
  scenarios: {
    busquedas: { executor: 'ramping-vus', startVUs: 0, stages: [
      { duration: '1m', target: 300 },
      { duration: '15m', target: 300 },
      { duration: '30s', target: 0 },
    ] },
  },
  thresholds: {
    'http_req_duration{tipo:busqueda}': ['p(95)<2500'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  return { cabeceras: iniciarSesion() };
}

export default function ({ cabeceras }) {
  const opciones = { headers: cabeceras, tags: { tipo: 'busqueda' } };
  const respuestas = [
    http.get(`${BASE}/api/productos?nombre=${nombreAlAzar()}`, opciones),
    http.get(`${BASE}/api/productos?categoriaId=${categoriaAlAzar()}`, opciones),
    http.get(`${BASE}/api/productos?activo=true&disponible=true`, opciones),
    http.get(`${BASE}/api/productos/disponibles?page=0&size=20`, opciones),
  ];
  respuestas.forEach((r) => check(r, { 'búsqueda 200': (x) => x.status === 200 }));
  sleep(1);
}
