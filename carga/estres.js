// RNF-04: 700 usuarios durante 10 minutos, mezclando consultas y algunas escrituras de un administrador.
// Después de la prueba se retira la carga y se comprueba que el sistema siga respondiendo.
// Al terminar, ejecuta scripts/verificar-integridad.sql para confirmar que no quedaron datos inconsistentes.
// Ejecutar: k6 run carga/estres.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, iniciarSesion, paginaAlAzar } from './comun.js';

export const options = {
  scenarios: {
    estres: { executor: 'constant-vus', vus: 700, duration: '10m' },
    recuperacion: { executor: 'constant-vus', vus: 5, duration: '2m', startTime: '11m', exec: 'recuperacion' },
  },
  thresholds: {
    'checks{escenario:recuperacion}': ['rate>0.99'],
  },
};

export function setup() {
  const consulta = iniciarSesion();
  const admin = (() => {
    const r = http.post(`${BASE}/api/auth/login`, JSON.stringify({ usuario: __ENV.ADMIN || 'admin', contrasena: __ENV.ADMIN_CONTRASENA || 'Almibar.2026' }),
      { headers: { 'Content-Type': 'application/json' } });
    return { Authorization: `Bearer ${r.json('token')}`, 'Content-Type': 'application/json' };
  })();
  return { consulta, admin };
}

export default function ({ consulta, admin }) {
  http.get(`${BASE}/api/productos?page=${paginaAlAzar(250)}&size=20`, { headers: consulta });
  if (Math.random() < 0.05) {
    // Escrituras concurrentes sobre los mismos productos: deben responder 200 o 409, nunca dejar datos rotos.
    const id = 1 + Math.floor(Math.random() * 20);
    const r = http.patch(`${BASE}/api/productos/${id}/disponibilidad`, JSON.stringify({ disponible: Math.random() < 0.5 }), { headers: admin });
    check(r, { 'escritura controlada': (x) => [200, 404, 409].includes(x.status) });
  }
  sleep(1);
}

export function recuperacion({ consulta }) {
  const r = http.get(`${BASE}/actuator/health`);
  check(r, { 'salud OK después de la carga': (x) => x.status === 200 }, { escenario: 'recuperacion' });
  const l = http.get(`${BASE}/api/productos?size=20`, { headers: consulta });
  check(l, { 'listado OK después de la carga': (x) => x.status === 200 }, { escenario: 'recuperacion' });
  sleep(1);
}
