// RNF-01: con 300 usuarios concurrentes y 5.000 productos, el p95 del listado y del detalle debe ser <= 2 s.
// Ejecutar: k6 run carga/consultas.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, iniciarSesion, paginaAlAzar } from './comun.js';

export const options = {
  scenarios: {
    consultas: { executor: 'ramping-vus', startVUs: 0, stages: [
      { duration: '1m', target: 300 },   // calentamiento
      { duration: '15m', target: 300 },  // carga sostenida
      { duration: '30s', target: 0 },
    ] },
  },
  thresholds: {
    'http_req_duration{tipo:listado}': ['p(95)<2000'],
    'http_req_duration{tipo:detalle}': ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  return { cabeceras: iniciarSesion() };
}

export default function ({ cabeceras }) {
  const listado = http.get(`${BASE}/api/productos?page=${paginaAlAzar(250)}&size=20`, { headers: cabeceras, tags: { tipo: 'listado' } });
  check(listado, { 'listado 200': (r) => r.status === 200 });
  const id = 1 + Math.floor(Math.random() * 5000);
  const detalle = http.get(`${BASE}/api/productos/${id}`, { headers: cabeceras, tags: { tipo: 'detalle' } });
  check(detalle, { 'detalle 200 o 404': (r) => r.status === 200 || r.status === 404 });
  sleep(1);
}
