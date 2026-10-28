// Mostrar / ocultar contraseña. Sirve para login.ftl (botón con texto) y para las
// plantillas heredadas de base (botón con <i> y data-icon-show / data-icon-hide).
const botones = document.querySelectorAll('[data-password-toggle]');
for (const boton of botones) {
  boton.addEventListener('click', () => {
    const campo = document.getElementById(boton.getAttribute('aria-controls'));
    if (!campo) return;
    const mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    boton.setAttribute('aria-label', boton.dataset[mostrar ? 'labelHide' : 'labelShow'] || '');
    boton.setAttribute('aria-pressed', String(mostrar));
    if (boton.dataset.textShow) {
      boton.textContent = boton.dataset[mostrar ? 'textHide' : 'textShow'];
    }
    const icono = boton.querySelector('i');
    if (icono && boton.dataset.iconShow) {
      icono.className = boton.dataset[mostrar ? 'iconHide' : 'iconShow'];
    }
  });
}

// Avisos locales: no cambian ni envían la contraseña.
const password = document.getElementById('password');
const capsWarning = document.getElementById('kc-caps-warning');
const spacesWarning = document.getElementById('kc-spaces-warning');
if (password) {
  const caps = (event) => { if (capsWarning && event.getModifierState) capsWarning.hidden = !event.getModifierState('CapsLock'); };
  password.addEventListener('keydown', caps);
  password.addEventListener('keyup', caps);
  password.addEventListener('blur', () => { if (capsWarning) capsWarning.hidden = true; });
  password.addEventListener('input', () => { if (spacesWarning) spacesWarning.hidden = password.value === password.value.trim(); });
}
