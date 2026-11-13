// Demostración corta de carga: 100 usuarios a la vez durante 1 minuto, sin cargar datos extra.
// Mide en pequeño lo mismo que RNF-01 y RNF-02 (las pruebas completas están en consultas.js y busquedas.js).
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE, iniciarSesion, nombreAlAzar } from './comun.js';


const USUARIOS = Number(__ENV.USUARIOS || 100);


export const options = {
stages: [
    { duration: '20s', target: USUARIOS },
    { duration: '1m', target: USUARIOS },
    { duration: '10s', target: 0 },
],
thresholds: {
    'http_req_duration{tipo:listado}': ['p(95)<2000'],
    'http_req_duration{tipo:detalle}': ['p(95)<2000'],
    'http_req_duration{tipo:busqueda}': ['p(95)<2500'],
    http_req_failed: ['rate<0.01'],
},
};


export function setup() {
const cabeceras = iniciarSesion();
const respuesta = http.get(BASE + '/api/productos?size=100', { headers: cabeceras });
const ids = respuesta.json('contenido').map(function (p) { return p.id; });
return { cabeceras: cabeceras, ids: ids };
}


export default function (datos) {
const cabeceras = datos.cabeceras;
const ids = datos.ids;


const listado = http.get(BASE + '/api/productos?page=0&size=20', { headers: cabeceras, tags: { tipo: 'listado' } });
check(listado, { 'listado 200': function (r) { return r.status === 200; } });


const id = ids[Math.floor(Math.random() * ids.length)];
const detalle = http.get(BASE + '/api/productos/' + id, { headers: cabeceras, tags: { tipo: 'detalle' } });
check(detalle, { 'detalle 200': function (r) { return r.status === 200; } });


const busqueda = http.get(BASE + '/api/productos?nombre=' + nombreAlAzar(), { headers: cabeceras, tags: { tipo: 'busqueda' } });
check(busqueda, { 'busqueda 200': function (r) { return r.status === 200; } });


sleep(1);
}