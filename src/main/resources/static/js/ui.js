// Utilidades de la vista: construir elementos de forma segura, íconos, formatos y avisos.

/** Crea un elemento. Los textos se asignan con textContent, nunca como HTML (evita XSS). */
export function el(etiqueta, atributos = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(atributos)) {
    if (valor === null || valor === undefined || valor === false) continue;
    if (clave === 'clase') nodo.className = valor;
    else if (clave === 'texto') nodo.textContent = valor;
    else if (clave.startsWith('on') && typeof valor === 'function') nodo.addEventListener(clave.slice(2), valor);
    else nodo.setAttribute(clave, valor === true ? '' : valor);
  }
  for (const hijo of hijos.flat()) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    nodo.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
  }
  return nodo;
}

const SVG = 'http://www.w3.org/2000/svg';

/** Ícono del sprite img/iconos.svg. */
export function icono(nombre, clase = 'icono') {
  const svg = document.createElementNS(SVG, 'svg');
  svg.setAttribute('class', clase);
  svg.setAttribute('aria-hidden', 'true');
  const uso = document.createElementNS(SVG, 'use');
  uso.setAttribute('href', `img/iconos.svg#i-${nombre}`);
  svg.append(uso);
  return svg;
}

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 2 });
const fechas = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

export const formatoPrecio = (valor) => pesos.format(Number(valor));
export const formatoFecha = (valor) => (valor ? fechas.format(new Date(valor)) : '');

export function saludoSegunHora(fecha = new Date()) {
  const hora = fecha.getHours();
  if (hora < 12) return 'Buenos días';
  if (hora < 19) return 'Buenas tardes';
  return 'Buenas noches';
}

/** Aviso breve que también leen los lectores de pantalla. */
export function avisar(mensaje, tipo = 'ok') {
  let zona = document.getElementById('avisos');
  if (!zona) {
    zona = el('div', { id: 'avisos', clase: 'avisos', 'aria-live': 'polite', role: 'status' });
    document.body.append(zona);
  }
  const aviso = el('div', { clase: `aviso${tipo === 'error' ? ' aviso--error' : ''}` },
    icono(tipo === 'error' ? 'circle-alert' : 'circle-check'), el('span', { texto: mensaje }));
  zona.append(aviso);
  setTimeout(() => aviso.remove(), tipo === 'error' ? 7000 : 4000);
}

/** Pone un botón en estado de carga y devuelve una función para restaurarlo. */
export function cargando(boton, texto) {
  const original = boton.textContent;
  boton.setAttribute('aria-busy', 'true');
  boton.disabled = true;
  boton.textContent = texto;
  return () => {
    boton.removeAttribute('aria-busy');
    boton.disabled = false;
    boton.textContent = original;
  };
}

/** Plato de madera con la foto del producto, o con migas si no tiene foto. */
export function plato(producto, clase = 'plato') {
  if (producto?.imagenUrl) {
    return el('div', { clase }, el('img', { src: producto.imagenUrl, alt: '', loading: 'lazy' }));
  }
  return el('div', { clase: `${clase} plato--vacio`, 'aria-hidden': 'true' });
}
