// Paneles laterales de la vitrina: formulario para crear o editar, y detalle de un producto.
import { ErrorApi, pedir } from './api.js';
import { avisar, cargando, el, formatoFecha, formatoPrecio, icono, plato } from './ui.js';

const TAMANO_MAXIMO = 5 * 1024 * 1024;
const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp'];
const ENFOCABLES = 'a[href], button:not([disabled]), input:not([disabled]):not([type="file"]), select, textarea, [tabindex]:not([tabindex="-1"])';

const velo = document.getElementById('velo');
let panelAbierto = null;
let disparadorPanel = null;

// ---------------------------------------------------------------------------
// Abrir y cerrar paneles (con foco atrapado y tecla Esc)
// ---------------------------------------------------------------------------

function abrirPanel(panel, disparador) {
  if (panelAbierto && panelAbierto !== panel) cerrarPanel({ devolverFoco: false });
  panelAbierto = panel;
  disparadorPanel = disparador ?? document.activeElement;
  panel.hidden = false;
  velo.hidden = false;
  document.body.classList.add('sin-desplazamiento');
  requestAnimationFrame(() => {
    panel.classList.add('abierto');
    velo.classList.add('abierto');
  });
  const primero = panel.querySelector('input:not([type="file"]), select, textarea, button');
  primero?.focus({ preventScroll: true });
}

function cerrarPanel({ devolverFoco = true } = {}) {
  const panel = panelAbierto;
  if (!panel) return;
  panelAbierto = null;
  panel.classList.remove('abierto');
  velo.classList.remove('abierto');
  document.body.classList.remove('sin-desplazamiento');
  setTimeout(() => {
    if (panelAbierto !== panel) panel.hidden = true;
    if (!panelAbierto) velo.hidden = true;
  }, 260);
  if (devolverFoco && disparadorPanel && document.contains(disparadorPanel)) disparadorPanel.focus();
}

velo.addEventListener('click', () => cerrarPanel());
document.querySelectorAll('.panel-lateral [data-cerrar]').forEach((boton) => boton.addEventListener('click', () => cerrarPanel()));
document.addEventListener('keydown', (evento) => {
  if (!panelAbierto) return;
  if (evento.key === 'Escape') {
    evento.preventDefault();
    cerrarPanel();
    return;
  }
  if (evento.key === 'Tab') {
    const elementos = [...panelAbierto.querySelectorAll(ENFOCABLES)].filter((e) => e.offsetParent !== null);
    if (!elementos.length) return;
    const primero = elementos[0];
    const ultimo = elementos[elementos.length - 1];
    if (evento.shiftKey && document.activeElement === primero) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && document.activeElement === ultimo) {
      evento.preventDefault();
      primero.focus();
    }
  }
});

// ---------------------------------------------------------------------------
// Formulario de producto
// ---------------------------------------------------------------------------

const panelFormulario = document.getElementById('panel-formulario');
const formulario = document.getElementById('form-producto');
const titulo = document.getElementById('formulario-titulo');
const errorGeneral = document.getElementById('formulario-error');
const campoNombre = document.getElementById('campo-nombre');
const campoDescripcion = document.getElementById('campo-descripcion');
const campoCategoria = document.getElementById('campo-categoria');
const campoPrecio = document.getElementById('campo-precio');
const campoDisponible = document.getElementById('campo-disponible');
const campoFoto = document.getElementById('campo-foto');
const zonaFoto = document.getElementById('foto-soltar');
const textoFoto = document.getElementById('foto-texto');
const vistaFoto = document.getElementById('foto-vista');
const botonGuardar = document.getElementById('boton-guardar');

let productoEnEdicion = null;
let fotoElegida = null;
let urlVistaPrevia = null;
let alGuardarFormulario = () => {};

/** Llena el selector de categorías del formulario. */
export function prepararCategorias(categorias) {
  campoCategoria.replaceChildren(el('option', { value: '', texto: 'Elige una' }),
    ...categorias.map((c) => el('option', { value: String(c.id), texto: c.nombre })));
}

/** Abre el formulario vacío (producto = null) o con los datos de un producto para editarlo. */
export function abrirFormulario(producto, { alGuardar, disparador } = {}) {
  productoEnEdicion = producto;
  alGuardarFormulario = alGuardar ?? (() => {});
  formulario.reset();
  limpiarErrores();
  fotoElegida = null;
  titulo.textContent = producto ? 'Editar producto' : 'Agregar producto';
  botonGuardar.textContent = producto ? 'Guardar cambios' : 'Guardar producto';
  campoNombre.value = producto?.nombre ?? '';
  campoDescripcion.value = producto?.descripcion ?? '';
  campoCategoria.value = producto ? String(producto.categoria.id) : '';
  campoPrecio.value = producto ? String(Number(producto.precio)) : '';
  ponerDisponible(producto ? producto.disponibilidad : true);
  campoDisponible.dataset.bloqueado = producto && !producto.activo ? 'true' : 'false';
  mostrarFoto(producto?.imagenUrl ?? null);
  textoFoto.textContent = producto?.imagenUrl ? 'Cambiar la foto' : 'Elige una foto o arrástrala aquí';
  actualizarContadores();
  abrirPanel(panelFormulario, disparador);
}

function ponerDisponible(disponible) {
  campoDisponible.setAttribute('aria-checked', String(disponible));
  campoDisponible.querySelector('.interruptor__texto').textContent = disponible ? 'En vitrina' : 'Agotado';
}

campoDisponible.addEventListener('click', () => {
  const nuevo = campoDisponible.getAttribute('aria-checked') !== 'true';
  if (nuevo && campoDisponible.dataset.bloqueado === 'true') {
    mostrarErrorGeneral('Este producto está fuera de carta. Vuelve a ponerlo en la carta antes de marcarlo como disponible.');
    return;
  }
  ponerDisponible(nuevo);
});

function actualizarContadores() {
  document.getElementById('contador-nombre').textContent = `${campoNombre.value.length} / 100`;
  document.getElementById('contador-descripcion').textContent = `${campoDescripcion.value.length} / 255`;
}
campoNombre.addEventListener('input', () => { actualizarContadores(); limpiarError('nombre'); });
campoDescripcion.addEventListener('input', () => { actualizarContadores(); limpiarError('descripcion'); });
campoCategoria.addEventListener('change', () => limpiarError('categoriaId'));
campoPrecio.addEventListener('input', () => limpiarError('precio'));

// Foto: elegir con el explorador o arrastrar y soltar
campoFoto.addEventListener('change', () => elegirFoto(campoFoto.files?.[0] ?? null));
zonaFoto.addEventListener('dragover', (evento) => { evento.preventDefault(); zonaFoto.classList.add('encima'); });
zonaFoto.addEventListener('dragleave', () => zonaFoto.classList.remove('encima'));
zonaFoto.addEventListener('drop', (evento) => {
  evento.preventDefault();
  zonaFoto.classList.remove('encima');
  elegirFoto(evento.dataTransfer?.files?.[0] ?? null);
});

function elegirFoto(archivo) {
  limpiarError('archivo');
  if (!archivo) return;
  if (!TIPOS_PERMITIDOS.includes(archivo.type)) {
    mostrarErrorCampo('archivo', 'Esa foto no es JPG, PNG o WEBP.');
    return;
  }
  if (archivo.size > TAMANO_MAXIMO) {
    mostrarErrorCampo('archivo', 'La foto pesa más de 5 MB. Usa una más liviana.');
    return;
  }
  fotoElegida = archivo;
  if (urlVistaPrevia) URL.revokeObjectURL(urlVistaPrevia);
  urlVistaPrevia = URL.createObjectURL(archivo);
  mostrarFoto(urlVistaPrevia);
  textoFoto.textContent = archivo.name;
}

function mostrarFoto(url) {
  vistaFoto.replaceChildren();
  vistaFoto.classList.toggle('plato--vacio', !url);
  if (url) vistaFoto.append(el('img', { src: url, alt: '' }));
}

/** Validación inmediata en la vista; la que manda es la del backend. */
function validarEnVista() {
  const errores = {};
  if (!campoNombre.value.trim()) errores.nombre = 'Escribe el nombre del producto.';
  if (!campoCategoria.value) errores.categoriaId = 'Elige una categoría.';
  const precio = campoPrecio.value.trim();
  if (!precio) errores.precio = 'Escribe el precio.';
  else if (!(Number(precio) > 0)) errores.precio = 'El precio debe ser mayor que cero.';
  else if (!/^\d{1,8}(\.\d{1,2})?$/.test(precio)) errores.precio = 'Usa máximo 8 cifras y dos decimales.';
  return errores;
}

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  limpiarErrores();
  const errores = validarEnVista();
  if (Object.keys(errores).length) {
    Object.entries(errores).forEach(([campo, mensaje]) => mostrarErrorCampo(campo, mensaje));
    document.getElementById(idDeCampo(Object.keys(errores)[0]))?.focus();
    return;
  }

  const datos = {
    nombre: campoNombre.value.trim(),
    descripcion: campoDescripcion.value.trim() || null,
    categoriaId: Number(campoCategoria.value),
    precio: Number(campoPrecio.value),
    disponible: campoDisponible.getAttribute('aria-checked') === 'true',
  };
  const esNuevo = !productoEnEdicion;
  const restaurar = cargando(botonGuardar, 'Guardando…');
  try {
    let producto = esNuevo
      ? await pedir('/api/productos', { metodo: 'POST', cuerpo: datos })
      : await pedir(`/api/productos/${productoEnEdicion.id}`, { metodo: 'PUT', cuerpo: datos });
    productoEnEdicion = producto;

    if (fotoElegida) {
      const envio = new FormData();
      envio.append('archivo', fotoElegida);
      try {
        producto = await pedir(`/api/productos/${producto.id}/imagen`, { metodo: 'POST', formulario: envio });
      } catch (e) {
        restaurar();
        titulo.textContent = 'Editar producto';
        botonGuardar.textContent = 'Guardar cambios';
        mostrarErrorCampo('archivo', e.message);
        avisar(`«${producto.nombre}» se guardó, pero la foto no. Revisa el archivo e inténtalo de nuevo.`, 'error');
        alGuardarFormulario(producto);
        return;
      }
    }
    restaurar();
    cerrarPanel();
    avisar(esNuevo ? `«${producto.nombre}» ya está en la carta` : 'Cambios guardados');
    alGuardarFormulario(producto);
  } catch (e) {
    restaurar();
    mostrarErroresApi(e);
  }
});

function mostrarErroresApi(error) {
  if (!(error instanceof ErrorApi)) {
    mostrarErrorGeneral('No se pudo guardar. Inténtalo de nuevo.');
    return;
  }
  if (error.campos.length) {
    error.campos.forEach(({ campo, mensaje }) => mostrarErrorCampo(campo, mensaje));
    if (error.estado === 400) mostrarErrorGeneral(error.message);
    document.getElementById(idDeCampo(error.campos[0].campo))?.focus();
  } else {
    mostrarErrorGeneral(error.message);
  }
}

const ID_CAMPO = { nombre: 'campo-nombre', descripcion: 'campo-descripcion', categoriaId: 'campo-categoria', precio: 'campo-precio', archivo: 'campo-foto' };
const idDeCampo = (campo) => ID_CAMPO[campo] ?? null;

function mostrarErrorCampo(campo, mensaje) {
  const destino = document.getElementById(`error-${campo}`);
  const control = document.getElementById(idDeCampo(campo));
  if (!destino) {
    mostrarErrorGeneral(mensaje);
    return;
  }
  destino.replaceChildren(icono('circle-alert'), document.createTextNode(mensaje));
  destino.hidden = false;
  control?.setAttribute('aria-invalid', 'true');
}

function limpiarError(campo) {
  const destino = document.getElementById(`error-${campo}`);
  if (destino) destino.hidden = true;
  document.getElementById(idDeCampo(campo))?.removeAttribute('aria-invalid');
}

function mostrarErrorGeneral(mensaje) {
  errorGeneral.replaceChildren(icono('circle-alert'), el('span', { texto: mensaje }));
  errorGeneral.hidden = false;
}

function limpiarErrores() {
  errorGeneral.hidden = true;
  Object.keys(ID_CAMPO).forEach(limpiarError);
}

// ---------------------------------------------------------------------------
// Detalle de producto
// ---------------------------------------------------------------------------

const panelDetalle = document.getElementById('panel-detalle');
const cuerpoDetalle = document.getElementById('detalle-cuerpo');
const pieDetalle = document.getElementById('detalle-pie');

/** Muestra la información completa de un producto (RF-02). */
export async function abrirDetalle(id, { esAdmin, alEditar, disparador } = {}) {
  document.getElementById('detalle-titulo').textContent = 'Cargando…';
  document.getElementById('detalle-categoria').textContent = '';
  cuerpoDetalle.replaceChildren(el('div', { clase: 'esqueleto__circulo detalle__plato' }));
  pieDetalle.replaceChildren(el('button', { clase: 'boton boton--secundario', type: 'button', onclick: () => cerrarPanel(), texto: 'Cerrar' }));
  abrirPanel(panelDetalle, disparador);
  try {
    const p = await pedir(`/api/productos/${id}`);
    document.getElementById('detalle-titulo').textContent = p.nombre;
    document.getElementById('detalle-categoria').textContent = p.categoria.nombre;
    cuerpoDetalle.replaceChildren(el('div', { clase: 'detalle' },
      plato(p, 'plato detalle__plato'),
      el('p', { clase: 'detalle__precio', texto: formatoPrecio(p.precio) }),
      el('div', { clase: 'detalle__estados' }, ...sellosDe(p)),
      p.descripcion ? el('p', { clase: 'detalle__descripcion', texto: p.descripcion }) : null,
      el('dl', { clase: 'detalle__datos' },
        el('dt', { texto: 'Se puede vender' }), el('dd', { texto: p.vendible ? 'Sí' : 'No' }),
        el('dt', { texto: 'Código' }), el('dd', { texto: String(p.id) }),
        el('dt', { texto: 'Registrado' }), el('dd', { texto: formatoFecha(p.fechaRegistro) }),
        el('dt', { texto: 'Última modificación' }), el('dd', { texto: formatoFecha(p.fechaActualizacion) }))));
    const acciones = [el('button', { clase: 'boton boton--secundario', type: 'button', onclick: () => cerrarPanel(), texto: 'Cerrar' })];
    if (esAdmin) {
      acciones.push(el('button', {
        clase: 'boton boton--principal', type: 'button', texto: 'Editar producto',
        onclick: () => alEditar?.(p),
      }));
    }
    pieDetalle.replaceChildren(...acciones);
  } catch (e) {
    document.getElementById('detalle-titulo').textContent = 'No se pudo abrir';
    cuerpoDetalle.replaceChildren(el('p', { clase: 'aviso-error' }, icono('circle-alert'), el('span', { texto: e.message })));
  }
}

/** Sellos de estado con ícono y texto (no solo color, RNF-13). */
export function sellosDe(p) {
  if (!p.activo) return [el('span', { clase: 'sello sello--fuera' }, icono('archive'), 'Fuera de carta')];
  return p.disponibilidad
    ? [el('span', { clase: 'sello sello--vitrina' }, icono('check'), 'En vitrina')]
    : [el('span', { clase: 'sello sello--agotado' }, icono('clock'), 'Agotado')];
}

export { cerrarPanel };
