// Vista principal: listado de la vitrina con búsqueda, filtros, paginación y acciones.
import { pedir } from './api.js';
import { abrirDetalle, abrirFormulario, cerrarPanel, prepararCategorias, sellosDe } from './paneles.js';
import { cerrarSesion, esAdmin, sesionActual, token } from './sesion.js';
import { avisar, el, formatoPrecio, icono, plato } from './ui.js';

if (!token()) window.location.replace('login.html');

const TAMANO_PAGINA = 20;
const FILTRO_ESTADO = {
  todos: {},
  vitrina: { activo: true, disponible: true },
  agotados: { activo: true, disponible: false },
  fuera: { activo: false },
};

const estado = { estado: 'todos', categoriaId: null, nombre: '', pagina: 0 };
let categorias = [];
let consultaActual = 0;

const filas = document.getElementById('filas');
const tabla = document.getElementById('lista');
const zonaEstado = document.getElementById('estado-lista');
const paginacion = document.getElementById('paginacion');
const botonAgregar = document.getElementById('boton-agregar');
const buscador = document.getElementById('buscar');
const dialogo = document.getElementById('dialogo-confirmar');

iniciar();

async function iniciar() {
  mostrarUsuario(sesionActual()?.usuario);
  try {
    // Confirma que el token sigue siendo válido y trae los datos actualizados del usuario.
    const usuario = await pedir('/api/auth/yo');
    mostrarUsuario(usuario);
  } catch {
    return; // pedir() ya redirige al login si la sesión venció
  }
  botonAgregar.hidden = !esAdmin();
  await Promise.all([cargarCategorias(), cargarResumen(), cargarLista()]);
}

function mostrarUsuario(usuario) {
  if (!usuario) return;
  const rol = usuario.rol === 'ADMIN' ? 'acceso completo' : 'solo consulta';
  document.getElementById('usuario-conectado').replaceChildren(
    el('strong', { texto: usuario.nombre }), el('span', { clase: 'barra__rol', texto: `, ${rol}` }));
}

document.getElementById('boton-salir').addEventListener('click', () => {
  cerrarSesion();
  window.location.replace('login.html');
});

botonAgregar.addEventListener('click', () => abrirFormulario(null, {
  disparador: botonAgregar,
  alGuardar: () => recargarTodo(),
}));

// ---------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------

document.querySelectorAll('.estados__opcion').forEach((boton) => boton.addEventListener('click', () => {
  estado.estado = boton.dataset.estado;
  estado.pagina = 0;
  document.querySelectorAll('.estados__opcion').forEach((b) => b.setAttribute('aria-pressed', String(b === boton)));
  cargarLista();
}));

let esperaBusqueda;
buscador.addEventListener('input', () => {
  clearTimeout(esperaBusqueda);
  esperaBusqueda = setTimeout(() => {
    estado.nombre = buscador.value.trim();
    estado.pagina = 0;
    cargarLista();
  }, 350);
});

async function cargarCategorias() {
  try {
    categorias = await pedir('/api/categorias', { publica: true });
  } catch {
    categorias = [];
  }
  prepararCategorias(categorias);
  pintarCategorias();
}

function pintarCategorias() {
  const fichas = [{ id: null, nombre: 'Todas' }, ...categorias].map((c) => el('button', {
    clase: 'ficha', type: 'button', 'aria-pressed': String(estado.categoriaId === c.id), texto: c.nombre,
    onclick: () => {
      estado.categoriaId = c.id;
      estado.pagina = 0;
      pintarCategorias();
      cargarLista();
    },
  }));
  document.getElementById('categorias').replaceChildren(...fichas);
}

function hayFiltros() {
  return estado.estado !== 'todos' || estado.categoriaId !== null || estado.nombre !== '';
}

function quitarFiltros() {
  estado.estado = 'todos';
  estado.categoriaId = null;
  estado.nombre = '';
  estado.pagina = 0;
  buscador.value = '';
  document.querySelectorAll('.estados__opcion').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.estado === 'todos')));
  pintarCategorias();
  cargarLista();
}

// ---------------------------------------------------------------------------
// Datos
// ---------------------------------------------------------------------------

async function cargarResumen() {
  try {
    const resumen = await pedir('/api/productos/resumen');
    document.querySelectorAll('[data-conteo]').forEach((nodo) => {
      nodo.textContent = String(resumen[nodo.dataset.conteo] ?? 0);
    });
  } catch {
    /* las pestañas siguen funcionando sin los números */
  }
}

async function cargarLista() {
  const numero = ++consultaActual;
  mostrarCargando();
  const parametros = new URLSearchParams({ page: String(estado.pagina), size: String(TAMANO_PAGINA) });
  Object.entries(FILTRO_ESTADO[estado.estado]).forEach(([clave, valor]) => parametros.set(clave, String(valor)));
  if (estado.categoriaId !== null) parametros.set('categoriaId', String(estado.categoriaId));
  if (estado.nombre) parametros.set('nombre', estado.nombre);
  try {
    const pagina = await pedir(`/api/productos?${parametros}`);
    if (numero !== consultaActual) return; // llegó una respuesta vieja
    if (!pagina.contenido.length && estado.pagina > 0) {
      estado.pagina -= 1;
      cargarLista();
      return;
    }
    pintarLista(pagina);
  } catch (e) {
    if (numero !== consultaActual) return;
    mostrarError(e.message);
  }
}

function recargarTodo() {
  cargarResumen();
  cargarLista();
}

// ---------------------------------------------------------------------------
// Pintar la lista y sus estados
// ---------------------------------------------------------------------------

function mostrarCargando() {
  filas.setAttribute('aria-busy', 'true');
  tabla.hidden = false;
  zonaEstado.replaceChildren();
  filas.replaceChildren(...Array.from({ length: 4 }, () => el('tr', { clase: 'esqueleto', 'aria-hidden': 'true' },
    el('td', {}, el('div', { clase: 'producto' }, el('div', { clase: 'esqueleto__circulo' }),
      el('div', { clase: 'esqueleto__texto' }, el('span', { clase: 'esqueleto__barra' }), el('span', { clase: 'esqueleto__barra esqueleto__barra--corta' })))),
    el('td', {}, el('span', { clase: 'esqueleto__barra' })),
    el('td', {}, el('span', { clase: 'esqueleto__barra' })),
    el('td', {}, el('span', { clase: 'esqueleto__barra' })),
    el('td', {}))));
}

function pintarLista(pagina) {
  filas.setAttribute('aria-busy', 'false');
  if (!pagina.contenido.length) {
    tabla.hidden = true;
    paginacion.hidden = true;
    zonaEstado.replaceChildren(estadoVacio());
    return;
  }
  tabla.hidden = false;
  zonaEstado.replaceChildren();
  filas.replaceChildren(...pagina.contenido.map(fila));
  pintarPaginacion(pagina);
}

function estadoVacio() {
  if (hayFiltros()) {
    return el('div', { clase: 'vacio' },
      el('div', { clase: 'plato vacio__plato plato--vacio', 'aria-hidden': 'true' }),
      el('h2', { clase: 'vacio__titulo', texto: 'No hay productos con esos filtros' }),
      el('p', { clase: 'vacio__texto', texto: 'Prueba con otro nombre u otra categoría.' }),
      el('button', { clase: 'boton boton--secundario', type: 'button', texto: 'Quitar filtros', onclick: quitarFiltros }));
  }
  return el('div', { clase: 'vacio' },
    el('div', { clase: 'plato vacio__plato plato--vacio', 'aria-hidden': 'true' }),
    el('h2', { clase: 'vacio__titulo', texto: 'Tu vitrina está vacía' }),
    el('p', { clase: 'vacio__texto', texto: esAdmin() ? 'Agrega el primer producto para empezar a armar la carta.' : 'Todavía no hay productos en la carta.' }),
    esAdmin() ? el('button', { clase: 'boton boton--principal', type: 'button', onclick: () => botonAgregar.click() }, icono('plus'), 'Agregar producto') : null);
}

function mostrarError(mensaje) {
  filas.setAttribute('aria-busy', 'false');
  tabla.hidden = true;
  paginacion.hidden = true;
  zonaEstado.replaceChildren(el('div', { clase: 'vacio', role: 'alert' },
    el('h2', { clase: 'vacio__titulo', texto: 'No pudimos cargar la vitrina' }),
    el('p', { clase: 'vacio__texto', texto: mensaje }),
    el('button', { clase: 'boton boton--secundario', type: 'button', onclick: () => recargarTodo() }, icono('refresh-cw'), 'Reintentar')));
}

function fila(p) {
  const admin = esAdmin();
  const celdaDisponibilidad = el('td', {});
  if (p.activo && admin) celdaDisponibilidad.append(interruptor(p));
  else celdaDisponibilidad.append(...sellosDe(p));

  const acciones = el('div', { clase: 'acciones' });
  if (admin) {
    acciones.append(el('button', {
      clase: 'boton boton--texto', type: 'button', 'aria-label': `Editar ${p.nombre}`,
      onclick: (evento) => abrirFormulario(p, { disparador: evento.currentTarget, alGuardar: () => recargarTodo() }),
    }, icono('pencil'), 'Editar'));
    acciones.append(p.activo
      ? el('button', { clase: 'boton boton--texto', type: 'button', 'aria-label': `Sacar ${p.nombre} de la carta`, onclick: () => confirmarDesactivar(p) }, icono('archive'), 'Sacar de carta')
      : el('button', { clase: 'boton boton--texto', type: 'button', 'aria-label': `Volver a poner ${p.nombre} en la carta`, onclick: (evento) => reactivar(p, evento.currentTarget) }, icono('archive-restore'), 'Volver a la carta'));
  }

  return el('tr', { clase: p.activo ? null : 'fila--fuera' },
    el('td', {}, el('button', {
      clase: 'producto producto__abrir', type: 'button', 'aria-label': `Ver el detalle de ${p.nombre}`,
      onclick: (evento) => abrirDetalle(p.id, {
        esAdmin: admin,
        disparador: evento.currentTarget,
        alEditar: (actual) => abrirFormulario(actual, { alGuardar: () => recargarTodo() }),
      }),
    }, plato(p), el('div', {},
      el('p', { clase: 'producto__nombre', texto: p.nombre }),
      p.descripcion ? el('p', { clase: 'producto__descripcion', texto: p.descripcion }) : null))),
    el('td', { clase: 'celda-categoria', texto: p.categoria.nombre }),
    el('td', { clase: 'num' }, el('span', { clase: 'precio', texto: formatoPrecio(p.precio) })),
    celdaDisponibilidad,
    el('td', {}, acciones));
}

/** Cambio rápido de disponibilidad (RF-P03). */
function interruptor(p) {
  const boton = el('button', {
    clase: 'interruptor', type: 'button', role: 'switch', 'aria-checked': String(p.disponibilidad),
    'aria-label': `Disponibilidad de ${p.nombre}`,
  }, el('span', { clase: 'interruptor__riel' }), el('span', { clase: 'interruptor__texto', texto: p.disponibilidad ? 'En vitrina' : 'Agotado' }));
  boton.addEventListener('click', async () => {
    const disponible = boton.getAttribute('aria-checked') !== 'true';
    boton.disabled = true;
    try {
      const actualizado = await pedir(`/api/productos/${p.id}/disponibilidad`, { metodo: 'PATCH', cuerpo: { disponible } });
      boton.setAttribute('aria-checked', String(actualizado.disponibilidad));
      boton.querySelector('.interruptor__texto').textContent = actualizado.disponibilidad ? 'En vitrina' : 'Agotado';
      avisar(actualizado.disponibilidad ? `«${p.nombre}» quedó en vitrina` : `«${p.nombre}» quedó agotado`);
      cargarResumen();
      if (estado.estado !== 'todos') cargarLista();
    } catch (e) {
      avisar(e.message, 'error');
    } finally {
      boton.disabled = false;
    }
  });
  return boton;
}

// ---------------------------------------------------------------------------
// Desactivar (sacar de la carta) y reactivar
// ---------------------------------------------------------------------------

let productoPorDesactivar = null;

function confirmarDesactivar(p) {
  productoPorDesactivar = p;
  document.getElementById('confirmar-titulo').textContent = `Sacar «${p.nombre}» de la carta`;
  document.getElementById('confirmar-texto').textContent =
    'Se desactiva: deja de aparecer para nuevos pedidos y queda como agotado, pero se conserva su información y su historial. Puedes volver a ponerlo en la carta cuando quieras.';
  dialogo.showModal();
}

document.getElementById('confirmar-cancelar').addEventListener('click', () => dialogo.close());
document.getElementById('confirmar-aceptar').addEventListener('click', async (evento) => {
  const boton = evento.currentTarget;
  const p = productoPorDesactivar;
  if (!p) return;
  boton.disabled = true;
  try {
    await pedir(`/api/productos/${p.id}`, { metodo: 'DELETE' });
    dialogo.close();
    avisar(`«${p.nombre}» salió de la carta`);
    recargarTodo();
  } catch (e) {
    dialogo.close();
    avisar(e.message, 'error');
  } finally {
    boton.disabled = false;
    productoPorDesactivar = null;
  }
});

async function reactivar(p, boton) {
  boton.disabled = true;
  try {
    await pedir(`/api/productos/${p.id}/reactivar`, { metodo: 'PATCH' });
    avisar(`«${p.nombre}» volvió a la carta. Márcalo en vitrina cuando esté disponible.`);
    recargarTodo();
  } catch (e) {
    avisar(e.message, 'error');
    boton.disabled = false;
  }
}

// ---------------------------------------------------------------------------
// Paginación
// ---------------------------------------------------------------------------

function pintarPaginacion(pagina) {
  if (pagina.totalPaginas <= 1) {
    paginacion.hidden = pagina.totalElementos === 0;
    document.getElementById('paginacion-texto').textContent = `${pagina.totalElementos} ${pagina.totalElementos === 1 ? 'producto' : 'productos'}`;
    document.getElementById('paginacion-paginas').replaceChildren();
    return;
  }
  paginacion.hidden = false;
  const desde = pagina.pagina * pagina.tamano + 1;
  const hasta = desde + pagina.contenido.length - 1;
  document.getElementById('paginacion-texto').textContent = `Mostrando ${desde}–${hasta} de ${pagina.totalElementos}`;

  const ir = (numero) => () => {
    estado.pagina = numero;
    cargarLista();
    document.querySelector('.estados').scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
  const botones = [el('button', { type: 'button', 'aria-label': 'Página anterior', disabled: pagina.pagina === 0, onclick: ir(pagina.pagina - 1) }, icono('chevron-left'))];
  let anterior = -1;
  for (let i = 0; i < pagina.totalPaginas; i++) {
    const visible = i === 0 || i === pagina.totalPaginas - 1 || Math.abs(i - pagina.pagina) <= 1;
    if (!visible) continue;
    if (i - anterior > 1) botones.push(el('span', { clase: 'puntos', 'aria-hidden': 'true', texto: '…' }));
    botones.push(el('button', {
      type: 'button', texto: String(i + 1), 'aria-label': `Página ${i + 1}`,
      'aria-current': i === pagina.pagina ? 'page' : null, onclick: ir(i),
    }));
    anterior = i;
  }
  botones.push(el('button', { type: 'button', 'aria-label': 'Página siguiente', disabled: pagina.pagina >= pagina.totalPaginas - 1, onclick: ir(pagina.pagina + 1) }, icono('chevron-right')));
  document.getElementById('paginacion-paginas').replaceChildren(...botones);
}

// Al cerrar sesión desde otra pestaña o si vence el token, pedir() lleva al login.
window.addEventListener('pageshow', () => {
  if (!token()) {
    cerrarPanel({ devolverFoco: false });
    window.location.replace('login.html');
  }
});
