# Preparación de GitHub

## Antes del primer push

1. Descomprime el proyecto.
2. Abre una terminal dentro de la carpeta raíz `kido-platform`.
3. Verifica que `.env` no aparezca en Git. Solo debe subirse `.env.example`.
4. Crea un repositorio vacío llamado `kido`.
5. Ejecuta los comandos siguientes y reemplaza la URL por la de tu repositorio.

```bash
git init
git add .
git commit -m "feat: base distribuida de Kido"
git branch -M main
git remote add origin https://github.com/USUARIO/kido.git
git push -u origin main
```

## Topics solicitados para la evaluación

En GitHub abre **Settings** o el engranaje junto a About y agrega:

- `campus-juliaca`
- `semestre-2026-2`
- `linea-software`
- `tipo-ps`
- `dist`
- `seccion-g1`
- `grupo-<numero>-kido`

Reemplacen el número pendiente con el número de grupo asignado por el docente. Si su sección no es G1, corrijan también ese topic antes de presentar.

## Cómo mostrar colaboración real

Adan realiza el primer commit con esta base. Midwar debe clonar el repositorio, crear una rama y subir su propio código con su cuenta:

```bash
git checkout -b feature/pagos-notificaciones
git add .
git commit -m "feat(midwar): agrega pagos retiros reembolsos y notificaciones"
git push -u origin feature/pagos-notificaciones
```

Luego se revisa el Pull Request y se integra a `main`. No deben compartir una sola cuenta ni cambiar el autor de los commits.

## Publicar MkDocs

El flujo `.github/workflows/documentacion.yml` crea la rama `gh-pages` automáticamente después del primer push a `main`. Cuando termine la acción:

1. Abre **Settings > Pages** en GitHub.
2. En **Build and deployment**, selecciona **Deploy from a branch**.
3. Elige la rama `gh-pages` y la carpeta `/ (root)`.
4. Guarda y espera a que GitHub muestre el enlace público.
