# Almíbar · Módulo de productos

Módulo de productos del **sistema de gestión de pedidos para repostería**, desarrollado con arquitectura **MVC** para la asignatura Arquitectura de Sistemas (Universidad Católica Luis Amigó).

Permite registrar, consultar, editar, buscar, listar, desactivar y reactivar productos, controlar su disponibilidad y asociarles una imagen. Incluye API REST con seguridad JWT, persistencia en MySQL, auditoría de operaciones e interfaz web propia.

El documento técnico completo está en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

---

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Backend | Spring Boot 3.5 (Web, Validation, Data JPA, Security, Actuator) |
| Base de datos | MySQL 8.4 + Flyway (migraciones) |
| Seguridad | Spring Security + JWT (jjwt) + BCrypt |
| Vista | HTML5, CSS3, JavaScript y la grilla de Bootstrap 5.3, con componentes y estilos propios |
| Pruebas | JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo, k6 |
| Herramientas | Maven (incluido con Maven Wrapper), Docker Compose, Lombok, Git |

---

## Requisitos previos

| Necesitas | Versión | Para qué |
|---|---|---|
| JDK | 17 o 21 (recomendado) | Compilar y ejecutar la aplicación |
| Docker Desktop | Cualquiera reciente | Levantar MySQL y correr las pruebas de integración |
| Git | Cualquiera | Clonar el repositorio |
| Navegador | Chrome, Edge o Firefox (dos últimas versiones) | Usar la aplicación |
| k6 *(opcional)* | 0.50 o superior | Pruebas de carga |

**No necesitas instalar Maven:** el proyecto trae Maven Wrapper (`mvnw` y `mvnw.cmd`), que descarga Maven la primera vez.

Comprueba Java con `java -version`. Si sale una versión menor que 17, instala un JDK 17 o 21 (por ejemplo, Eclipse Temurin).

---

## Instalación y ejecución

Los comandos están para **PowerShell en Windows**. En Linux o macOS, usa `cp` en vez de `copy` y `./mvnw` en vez de `.\mvnw.cmd`.

### 1. Clonar el proyecto

```powershell
git clone https://github.com/811sg/reposteria-productos.git
cd reposteria-productos
```

### 2. Crear el archivo `.env`

```powershell
copy .env.example .env
```

Abre `.env` y cambia:

- `DB_PASSWORD` y `DB_ROOT_PASSWORD`: inventa dos contraseñas.
- `JWT_SECRET`: genera uno con este comando y pega el resultado:

  ```powershell
  [Convert]::ToBase64String([byte[]](1..32 | ForEach-Object { Get-Random -Maximum 256 }))
  ```

El archivo `.env` no se sube a GitHub (está en `.gitignore`).

### 3. Levantar MySQL

Con Docker Desktop abierto:

```powershell
docker compose up -d
docker compose ps
```

Espera a que la columna `STATUS` de `almibar-mysql` diga `healthy` (unos 20 segundos la primera vez).

### 4. Arrancar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

La primera vez descarga las dependencias (puede tardar unos minutos). Flyway crea las tablas, los índices y los datos iniciales automáticamente. Está lista cuando aparece `Started ProductosApplication`.

### 5. Abrir la aplicación

Entra a **http://localhost:8080** (te lleva al inicio de sesión).

### Usuarios de prueba

| Usuario | Contraseña | Rol | Puede |
|---|---|---|---|
| `admin` | `Almibar.2026` | ADMIN | Ver, crear, editar, desactivar, reactivar, cambiar disponibilidad y subir imágenes |
| `consulta` | `Consulta.2026` | CONSULTA | Solo ver el catálogo |

Las contraseñas se guardan en la base de datos con BCrypt; aquí aparecen solo para poder probar.

### Detener todo

`Ctrl + C` en la terminal de la aplicación, y para MySQL: `docker compose down` (los datos se conservan en un volumen de Docker).

---

## Pruebas

```powershell
.\mvnw.cmd test      # pruebas unitarias, de API y de integración
.\mvnw.cmd verify    # lo mismo, y además exige 70 % de cobertura en la lógica de negocio
```

- El reporte de cobertura queda en `target/site/jacoco/index.html`.
- Las pruebas de integración (`ProductosIntegracionTest`) levantan su propio MySQL con Testcontainers: **necesitan Docker Desktop abierto**. Si Docker no está corriendo, se omiten y las demás se ejecutan igual.

| Qué se prueba | Clase de prueba |
|---|---|
| Creación, consulta, actualización, desactivación y reactivación | `ProductoServiceImplTest`, `ProductoControllerTest`, `ProductosIntegracionTest` |
| Campos obligatorios y validación de precios | `ReglasProductoTest`, `ProductoControllerTest` |
| Productos inexistentes (404) | `ProductoServiceImplTest`, `ProductoControllerTest` |
| Regla de negocio (inactivo no puede estar disponible, 409) | `ReglasProductoTest`, `ProductoServiceImplTest`, `ProductoControllerTest`, `ProductosIntegracionTest` |
| Manejo de errores (400, 422, 500 sin detalles internos) | `ProductoControllerTest` |
| Acceso a endpoints protegidos (401 y 403, token vencido o alterado) | `ProductoControllerTest`, `JwtServiceTest`, `ProductosIntegracionTest` |
| Estrategias de búsqueda (Strategy) | `BusquedaStrategyTest` |
| Imágenes: formato real, tamaño y almacenamiento (Adapter) | `FormatoImagenTest`, `ImagenValidatorTest`, `LocalImageStorageTest`, `CloudImageStorageAdapterTest`, `ImageStorageContratoTest` |
| Restricciones de MySQL y auditoría | `ProductosIntegracionTest` |

---

## Pruebas de carga (k6)

Con la aplicación corriendo:

1. Carga 5.000 productos de prueba (solo en una base de pruebas):

   ```powershell
   docker cp scripts\semilla-5000-productos.sql almibar-mysql:/tmp/semilla.sql
   docker exec -it almibar-mysql sh -c "mysql -u root -p almibar < /tmp/semilla.sql"
   ```

   Te pedirá la contraseña de root (`DB_ROOT_PASSWORD`).

2. Ejecuta la prueba que necesites:

   | Requisito | Comando |
   |---|---|
   | RNF-01 (listado y detalle, p95 ≤ 2 s) | `k6 run carga\consultas.js` |
   | RNF-02 (búsquedas, p95 ≤ 2,5 s) | `k6 run carga\busquedas.js` |
   | RNF-03 (300, 500 y 700 usuarios) | `k6 run -e USUARIOS=300 carga\escalabilidad.js` |
   | RNF-04 (estrés, 700 usuarios por 10 min) | `k6 run carga\estres.js` |

3. Después del estrés, revisa que no haya datos inconsistentes (todas las filas deben dar 0):

   ```powershell
   docker cp scripts\verificar-integridad.sql almibar-mysql:/tmp/verificar.sql
   docker exec -it almibar-mysql sh -c "mysql -u root -p almibar < /tmp/verificar.sql"
   ```

Los tiempos solo son válidos si se miden en un servidor con características documentadas; en un portátil sirven como referencia.

---

## Respaldos (RNF-17)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\respaldo.ps1
powershell -ExecutionPolicy Bypass -File scripts\restaurar.ps1 respaldos\almibar-AAAAMMDD-HHMM.sql
```

Para el respaldo diario, programa el primer comando en el **Programador de tareas** de Windows (o usa `scripts/respaldo.sh` con `cron` en Linux). Se conservan los últimos 14 respaldos.

---

## Configuración opcional

### Imágenes en la nube (Cloudinary)

Por defecto las imágenes se guardan en la carpeta `uploads/`. Para usar Cloudinary, crea una cuenta gratuita y en `.env` pon:

```
IMAGENES_PROVEEDOR=nube
CLOUDINARY_CLOUD_NAME=tu-cuenta
CLOUDINARY_API_KEY=tu-clave
CLOUDINARY_API_SECRET=tu-secreto
```

### Producción con HTTPS (RNF-08)

1. Genera o consigue un certificado PKCS12. Para pruebas:

   ```powershell
   keytool -genkeypair -alias almibar -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore almibar.p12 -validity 365
   ```

2. Agrega a `.env`: `SSL_KEYSTORE=file:almibar.p12`, `SSL_KEYSTORE_PASSWORD=la-clave-que-elegiste`.
3. Compila y arranca con el perfil de producción:

   ```powershell
   .\mvnw.cmd clean package
   java -jar target\reposteria-productos-1.0.0.jar --spring.profiles.active=prod
   ```

   La aplicación queda en `https://localhost:8443`.

---

## API REST

Todas las respuestas de error tienen el mismo formato: `estado`, `error`, `mensaje`, `codigo` (si aplica), `campos` (si aplica), `ruta` y `fecha`. Las rutas protegidas reciben el token en la cabecera `Authorization: Bearer <token>`.

| Método | Ruta | Acceso | Respuestas |
|---|---|---|---|
| POST | `/api/auth/login` | Público | 200 token · 400 · 401 |
| GET | `/api/auth/yo` | Con sesión | 200 · 401 |
| GET | `/api/categorias` | Público | 200 |
| GET | `/api/productos?nombre=&categoriaId=&disponible=&activo=&page=&size=` | Con sesión | 200 · 400 · 401 · 404 (categoría) |
| GET | `/api/productos/disponibles?page=&size=` | Público | 200 · 400 |
| GET | `/api/productos/resumen` | Con sesión | 200 · 401 |
| GET | `/api/productos/{id}` | Con sesión | 200 · 401 · 404 |
| POST | `/api/productos` | ADMIN | 201 + `Location` · 400 · 401 · 403 · 422 |
| PUT | `/api/productos/{id}` | ADMIN | 200 · 400 · 401 · 403 · 404 · 409 · 422 |
| DELETE | `/api/productos/{id}` (desactivación lógica) | ADMIN | 200 · 401 · 403 · 404 · 409 |
| PATCH | `/api/productos/{id}/reactivar` | ADMIN | 200 · 401 · 403 · 404 · 409 |
| PATCH | `/api/productos/{id}/disponibilidad` | ADMIN | 200 · 400 · 401 · 403 · 404 · 409 |
| POST | `/api/productos/{id}/imagen` (multipart, campo `archivo`) | ADMIN | 200 · 401 · 403 · 404 · 413 · 422 |
| GET | `/actuator/health` | Público | 200 |
| GET | `/actuator/metrics` | ADMIN | 200 · 401 · 403 |

La documentación formal con Swagger/OpenAPI y la colección de Postman las prepara otro integrante del equipo. Para Swagger basta con agregar la dependencia `org.springdoc:springdoc-openapi-starter-webmvc-ui` (versión 2.8.x, compatible con Spring Boot 3.5) y permitir `/swagger-ui/**` y `/v3/api-docs/**` en `SecurityConfig`.

---

## Estructura del proyecto

```
src/main/java/com/reposteria/productos/
├── controller/        Controladores (C de MVC)
├── dto/               Datos que entran (request) y salen (response)
├── mapper/            Entidad → DTO
├── model/             Modelo (M de MVC)
│   ├── entity/        Entidades JPA
│   ├── repository/    Acceso a datos (Repository)
│   ├── service/       Lógica de negocio
│   ├── rules/         Reglas de negocio
│   ├── search/        Estrategias de búsqueda (Strategy)
│   ├── event/         Eventos y auditoría (Observer)
│   └── storage/       Almacenamiento de imágenes (Adapter)
├── security/          Seguridad: reglas de acceso y JWT
├── config/            Configuración
└── exception/         Excepciones y manejador central de errores
src/main/resources/
├── db/migration/      Migraciones de Flyway
└── static/            Vista (V de MVC): HTML, CSS, JS, fuentes e imágenes
```

---

## Errores frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | No existe el archivo `.env` o le falta `JWT_SECRET`. Repite el paso 2 de la instalación. |
| `JWT_SECRET debe tener al menos 32 bytes` | El secreto es muy corto. Genera uno nuevo con el comando del paso 2. |
| `Communications link failure` o `Connection refused` al arrancar | MySQL no está corriendo. Abre Docker Desktop y ejecuta `docker compose up -d`. |
| `Access denied for user 'almibar'` | Cambiaste las contraseñas del `.env` después de crear el contenedor. Ejecuta `docker compose down -v` (borra los datos) y luego `docker compose up -d`. |
| `Port 3306 is already allocated` | Tienes otro MySQL instalado en el equipo. Detenlo o cambia el puerto en `docker-compose.yml` (por ejemplo `"3307:3306"`) y en `DB_URL`. |
| `Port 8080 was already in use` | Otra aplicación usa el puerto. Agrega `PORT=8081` al `.env`. |
| `mvnw.cmd : El término no se reconoce` | En PowerShell hay que escribir `.\mvnw.cmd`, con el punto y la barra al inicio. |
| La vista dice "No pudimos conectar con el servidor" | La aplicación no está corriendo o se detuvo. Revisa la terminal. |
| Las pruebas de integración aparecen como omitidas | Docker Desktop no está abierto. Ábrelo y vuelve a ejecutar las pruebas. |
| El IDE marca errores en `getNombre()` o similares | Falta activar el procesamiento de anotaciones para Lombok (IntelliJ: *Settings → Build → Compiler → Annotation Processors*; VS Code: extensión *Lombok Annotations Support*). Compilar con `mvnw` funciona igual. |

---

## Créditos

- Tipografías **Corben** y **Figtree** (SIL Open Font License), incluidas en `static/fonts`.
- Íconos de **Lucide** (licencia ISC).
- Grilla de **Bootstrap 5.3** (licencia MIT), incluida en `static/css/vendor`.
- Fotografía de los alfajores: aportada por el equipo.

Equipo: Mariana Piedrahita Flórez, Sara Melisa Sánchez Vásquez y Sebastián González Campiño. Docente: Javier Darío Cardona Cárdenas.
