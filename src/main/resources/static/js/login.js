// Vista de inicio de sesión: valida el formulario, pide el token y entra a la vitrina.
import { ErrorApi, pedir } from './api.js';
import { guardarSesion, token } from './sesion.js';
import { cargando, icono, saludoSegunHora } from './ui.js';

if (token()) window.location.replace('vitrina.html');

const formulario = document.getElementById('form-login');
const usuario = document.getElementById('usuario');
const clave = document.getElementById('clave');
const error = document.getElementById('error-login');
const boton = document.getElementById('boton-entrar');
const mostrar = document.getElementById('mostrar-clave');

document.getElementById('saludo').textContent = saludoSegunHora();

if (new URLSearchParams(window.location.search).get('sesion') === 'vencida') {
  mostrarError('Tu sesión terminó. Vuelve a entrar.');
}

mostrar.addEventListener('click', () => {
  const visible = clave.type === 'text';
  clave.type = visible ? 'password' : 'text';
  mostrar.setAttribute('aria-pressed', String(!visible));
  mostrar.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
  mostrar.replaceChildren(icono(visible ? 'eye' : 'eye-off'));
});

[usuario, clave].forEach((campo) => campo.addEventListener('input', () => {
  campo.removeAttribute('aria-invalid');
  error.hidden = true;
}));

formulario.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const faltan = [usuario, clave].filter((campo) => !campo.value.trim());
  faltan.forEach((campo) => campo.setAttribute('aria-invalid', 'true'));
  if (faltan.length) {
    mostrarError(faltan.length === 2 ? 'Escribe tu usuario y tu contraseña.' : `Escribe tu ${faltan[0] === usuario ? 'usuario' : 'contraseña'}.`);
    faltan[0].focus();
    return;
  }

  const restaurar = cargando(boton, 'Entrando…');
  try {
    const respuesta = await pedir('/api/auth/login', {
      metodo: 'POST',
      cuerpo: { usuario: usuario.value.trim(), contrasena: clave.value },
      publica: true,
    });
    guardarSesion(respuesta.token, respuesta.usuario);
    window.location.replace('vitrina.html');
  } catch (e) {
    restaurar();
    mostrarError(e instanceof ErrorApi ? e.message : 'No se pudo iniciar sesión.');
    if (e.estado === 401) {
      clave.value = '';
      clave.focus();
    }
  }
});

function mostrarError(mensaje) {
  error.replaceChildren(icono('circle-alert'), document.createTextNode(mensaje));
  error.hidden = false;
}
