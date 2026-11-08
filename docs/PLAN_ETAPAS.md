# Plan por etapas — Sistema de Activo Fijo

## Cómo usar este documento

Cada etapa produce algo **que funciona y se puede probar**. El ciclo es siempre el mismo:

1. Abre el proyecto en VS Code y en el panel de Claude pega el **Prompt** de la etapa.
2. Claude propone un plan → revísalo, corrígelo si hace falta y apruébalo.
3. Claude implementa, escribe tests y los ejecuta.
4. **Tú pruebas** con la lista "Cómo probar". Si algo falla, pídele a Claude que lo corrija en la misma etapa.
5. Cuando todo pasa: revisa `docs/AVANCE.md`, confirma el commit y crea un tag: `git tag etapa-N`.
6. Recién ahí pasas a la siguiente etapa (idealmente en una **conversación nueva** de Claude,
   para que parta limpio leyendo `CLAUDE.md` y `AVANCE.md`).

Si una etapa sale mal sin remedio: `git reset --hard etapa-(N-1)` y vuelves a intentarla.

| Etapa | Tema | Resultado visible |
|---|---|---|
| 0 | Esqueleto del proyecto | Backend y frontend corriendo y conectados |
| 1 | Seguridad, empresas y usuarios | Login, menú, administración de usuarios, aislamiento entre empresas |
| 2 | Tablas maestras y localidades | Todos los catálogos mantenibles e importables desde Excel |
| 3 | Proveedores y Bienes | El módulo central: listado con filtros, ficha, historial, edición masiva |
| 4 | Partes y Software | Componentes de equipos, software y licencias |
| 5 | Recepción y Altas | Ingreso de facturas/guías y alta de bienes en ubicaciones |
| 5B | Depreciación | Cálculo mensual según vidas útiles SII, valor libro, informes |
| 6 | Movimientos | Traslados, asignaciones, préstamos, devoluciones y bajas con su guía |
| 7 | Contratos, garantías y mantenciones | Contratos asociados a bienes, costos de mantención |
| 8 | Etiquetas e informes | Etiquetas con código de barras y catálogo de informes PDF/Excel |
| 9 | Inventario físico | Toma de inventario con el celular y conciliación |
| 10 | Dashboard, auditoría e importación | Gráficos de gestión, informe de auditoría, carga masiva de bienes |
| 11 | Despliegue | Instalado en el servidor con HTTPS y respaldos |

---

## Etapa 0 — Esqueleto del proyecto

**Objetivo:** dejar la base técnica lista, sin funcionalidad de negocio.

**Alcance**
- `docker-compose.yml` con PostgreSQL (usuario/clave/BD de desarrollo, volumen persistente).
- Backend Spring Boot con las dependencias del stack, perfiles `dev`, `test` y `prod`
  (`application-*.yml`), configuración por variables de entorno para producción.
- Flyway con una migración inicial vacía o mínima.
- Manejador global de errores con `ProblemDetail` y mensajes en español.
- Endpoint `GET /api/v1/salud` que responde versión y estado de la BD.
- Utilidad de RUT (validar, normalizar, formatear) con tests unitarios.
- Frontend Angular con Angular Material, locale `es-CL`, proxy `/api` → `localhost:8080`,
  layout base (barra superior + menú lateral vacío) y una página que muestra la respuesta de `/salud`.
- Test de integración con Testcontainers que levanta el contexto y llama a `/salud`.
- `docs/AVANCE.md` creado; versiones anotadas en la sección 9 de `CLAUDE.md`.
- `.gitignore`, `README.md` breve con los comandos.

**Cómo probar**
- [ ] `docker compose up -d` levanta PostgreSQL.
- [ ] `./mvnw verify` pasa (incluye Testcontainers).
- [ ] `npm start` y abrir `http://localhost:4200` muestra el layout y "Base de datos: OK".
- [ ] Apagar PostgreSQL → la página muestra un error claro, no una pantalla en blanco.

**Prompt**
```
Lee CLAUDE.md y docs/PLAN_ETAPAS.md. Vamos a ejecutar la Etapa 0 (esqueleto del proyecto).
Primero verifica las últimas versiones estables de Spring Boot y Angular compatibles con Java 21,
y propón un plan con la estructura de carpetas y las dependencias. Espera mi aprobación antes de
crear archivos. Al terminar, ejecuta los tests, completa la sección 9 de CLAUDE.md, crea
docs/AVANCE.md con lo realizado y cómo probarlo, y haz commit "etapa 0: esqueleto".
No implementes nada de etapas posteriores.
```

---

## Etapa 1 — Seguridad, empresas y usuarios

**Objetivo:** login seguro, multiempresa funcionando y administración de usuarios.

**Modelo de datos**
- `empresa`: id, rut, razon_social, nombre_fantasia, email_contacto, telefono, activa, creado_en.
- `usuario`: id, empresa_id (null solo para SUPERADMIN), rut, nombre, username (único global),
  email, telefono_anexo, cargo, password_hash (BCrypt), rol, activo, debe_cambiar_password,
  ultimo_acceso, intentos_fallidos, bloqueado_hasta.
- `evento_auditoria`: id, empresa_id, usuario_id, tipo (LOGIN_OK, LOGIN_FALLIDO, LOGOUT,
  CAMBIO_PASSWORD, ENTRAR_COMO_EMPRESA, ...), detalle, ip, fecha.
- Clase base `EntidadEmpresa` con `@TenantId empresaId` + `CurrentTenantIdentifierResolver`.

**Alcance backend**
- Login `POST /api/v1/auth/login`, `POST /auth/logout`, `GET /auth/yo` (usuario, rol, empresa).
- Sesión con Spring Session JDBC; CSRF con cookie `XSRF-TOKEN`.
- Bloqueo temporal tras 5 intentos fallidos. Política de clave mínima (8 caracteres, letras y números).
- Cambio de contraseña propio; reseteo por ADMIN (genera clave temporal y obliga a cambiarla).
- CRUD de empresas (solo SUPERADMIN) con creación del primer ADMIN de la empresa.
- CRUD de usuarios de la empresa (ADMIN), con filtros por nombre/RUT/rol/activo.
- "Entrar como empresa" para SUPERADMIN (queda auditado).
- Datos semilla en perfil `dev`: un SUPERADMIN, dos empresas demo con un ADMIN cada una.

**Alcance frontend**
- Pantalla de login, interceptor de errores (401 → login, 403 → mensaje).
- Guards por rol; menú lateral según rol (mostrar ítems futuros deshabilitados o no mostrarlos).
- Pantallas: Mi cuenta / cambiar contraseña; Usuarios (listado + formulario); Empresas (SUPERADMIN).
- Componente compartido `tabla-filtrable` (se reutilizará en todo el sistema).

**Cómo probar**
- [ ] Ingresar con el ADMIN de la empresa A; crear un usuario OPERADOR y uno CONSULTA.
- [ ] Ingresar como ADMIN de la empresa B: no ve los usuarios de A. Pedir por URL/API un usuario de A → 404.
- [ ] 5 claves malas seguidas → usuario bloqueado temporalmente.
- [ ] El usuario CONSULTA no ve el menú de administración y la API le responde 403.
- [ ] Resetear clave → al ingresar obliga a cambiarla.
- [ ] RUT inválido es rechazado con mensaje claro.
- [ ] Tests de aislamiento entre empresas pasan.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 1 de docs/PLAN_ETAPAS.md. Implementa seguridad,
multiempresa (Hibernate @TenantId) y administración de empresas y usuarios. Antes de escribir
código, propón el plan, incluyendo cómo resolverás el tenant en cada request y cómo probarás el
aislamiento. Espera mi aprobación. Incluye tests de seguridad (roles) y de aislamiento entre
empresas. Al final ejecuta todos los tests, actualiza docs/AVANCE.md y haz commit "etapa 1: ...".
```

---

## Etapa 2 — Tablas maestras y localidades

**Objetivo:** todos los catálogos que usan los módulos siguientes, mantenibles e importables.

**Modelo de datos**
- **Catálogo genérico** `catalogo_item`: id, empresa_id, tipo (enum), codigo, nombre, prefijo
  (nullable, usado por tipo de equipo), padre_id (nullable: Modelo→Marca, Versión→Nombre de software,
  Subgrupo→Grupo), activo. `UNIQUE(empresa_id, tipo, codigo)`.
  Tipos: `TIPO_EQUIPO`, `TIPO_PARTE`, `CLASIFICACION_BIEN`, `MARCA`, `MODELO`, `FAMILIA`, `GRUPO`,
  `SUBGRUPO`, `CALIDAD` (estado físico: bueno/regular/malo...), `NORMA`, `PROCESADOR`,
  `VELOCIDAD_PROCESADOR`, `MEMORIA_RAM`, `DISCO_DURO`, `TARJETA_VIDEO`, `MONITOR`, `TIPO_SERVIDOR`,
  `TIPO_SOFTWARE`, `MARCA_SOFTWARE`, `NOMBRE_SOFTWARE`, `VERSION_SOFTWARE`, `TIPO_PROVEEDOR`,
  `TIPO_CONTRATO`, `CENTRO_COSTO`.
  El servicio valida que una referencia apunte al tipo correcto (ej. `bien.marca_id` debe ser `MARCA`).
- **Localidades** (tablas propias, jerárquicas): `establecimiento` → `servicio` → `unidad` →
  `localizacion`. Cada una: codigo, nombre, padre, activo (+ dirección en establecimiento).
- **Perfiles**: `perfil_ubicacion` (usuario ↔ establecimientos/servicios visibles) y
  `perfil_tipo_bien` (usuario ↔ tipos de equipo visibles).
- Homologaciones (procesador y nombre de software): tabla `homologacion(tipo, texto_origen, item_id)`
  para mapear textos variados a un ítem canónico al importar.

**Alcance**
- API genérica `/api/v1/catalogos/{tipo}` (listar con filtro, crear, editar, desactivar) y
  `/api/v1/localidades/...`. Endpoint de árbol de localidades para selectores.
- Pantalla "Tablas maestras" con tarjetas agrupadas (Localidades, Equipos/Bienes, Hardware,
  Software, Perfiles, Otras) que abren **un mismo componente mantenedor genérico** parametrizado por tipo.
- Componente compartido `selector-localidad` (4 combos encadenados) y `selector-catalogo` (autocompletar).
- Importar desde Excel: descargar plantilla por tipo y subirla; informe de filas OK / con error.
- Asignación de perfiles de ubicación y de tipo de bien a usuarios (pestaña en el formulario de usuario).
- Datos semilla demo (perfil `dev`): algunos tipos de equipo con prefijo, marcas, modelos y localidades.

**Cómo probar**
- [ ] Crear marca "HP" y modelo "ProDesk 400" asociado a HP; el modelo no aparece al elegir otra marca.
- [ ] No se puede crear dos veces el mismo código en el mismo tipo. Sí en otra empresa.
- [ ] Desactivar una marca → ya no aparece en selectores, pero sigue visible en el mantenedor.
- [ ] Crear la cadena Establecimiento → Servicio → Unidad → Localización y verla en el selector encadenado.
- [ ] Importar 50 localidades desde la plantilla Excel, con 2 filas con error → el informe las indica.
- [ ] Asignar perfil de ubicación a un usuario (se aplicará desde la Etapa 3).

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 2 de docs/PLAN_ETAPAS.md. Implementa las tablas maestras
con un catálogo genérico, las localidades jerárquicas, los perfiles de usuario y la importación
desde Excel. Mira docs/referencia/09-tablas-maestras.png solo como guía de qué catálogos existen.
Prioriza reutilización: un solo componente mantenedor genérico en Angular. Propón el plan y
espera aprobación. Tests de servicio, de API y de aislamiento. Al final actualiza
docs/AVANCE.md y haz commit "etapa 2: ...".
```

---

## Etapa 3 — Proveedores y Bienes (módulo central)

**Objetivo:** el corazón del sistema. Ver `docs/referencia/03-bienes.png` y `10-proveedores.png`.

**Modelo de datos**
- `proveedor`: codigo, rut, razon_social, contacto, telefono1, telefono2, email, direccion, tipo_proveedor_id, activo.
- `bien`: codigo (autogenerado con prefijo del tipo, editable solo por ADMIN), num_inventario,
  tipo_equipo_id, clasificacion_id, familia_id, grupo_id, subgrupo_id, marca_id, modelo_id, num_serie,
  descripcion, establecimiento_id, servicio_id, unidad_id, localizacion_id, responsable (texto o usuario),
  calidad_id, norma_id, procesador_id, velocidad_procesador_id, memoria_ram_id, disco_duro_id,
  tarjeta_video_id, monitor_id, direccion_ip, nombre_equipo_red, proveedor_id, fecha_compra,
  valor_compra, numero_factura, fecha_garantia_hasta, estado (`ACTIVO`, `EN_PRESTAMO`,
  `EN_MANTENCION`, `DADO_DE_BAJA`), observaciones. Auditado con Envers.
  `UNIQUE(empresa_id, codigo)`; índices por tipo, localidad, marca, serie, num_inventario.
- Los campos de hardware se muestran solo si el tipo de equipo es computacional
  (atributo booleano `es_computacional` en el tipo de equipo; agregar migración).

**Alcance**
- CRUD de proveedores con filtro por tipo y exportación.
- Listado de bienes con paginación en servidor y **todos los filtros** de la referencia: tipo, unidad,
  servicio, establecimiento, localización, marca, modelo, grupo, subgrupo, procesador, RAM desde/hasta,
  disco desde/hasta, norma, calidad, estado + búsqueda libre (código, inventario, serie, descripción).
  Mostrar total de registros. Opción "mostrar dados de baja".
- **Respetar perfiles de ubicación y tipo de bien** del usuario en todas las consultas.
- Ficha del bien (vista detalle con pestañas: datos, ubicación, hardware, compra/garantía, historial).
- **Historial**: cambios de campos desde Envers (quién, cuándo, valor anterior → nuevo).
- **Edición masiva**: seleccionar varios bienes (o todos los del filtro) y cambiar uno o más campos
  (calidad, norma, responsable, grupo...). Pide confirmación mostrando cuántos registros cambiarán.
  **No** permite cambiar ubicación (eso es un movimiento, Etapa 6).
- Exportar listado a Excel y a PDF con los filtros aplicados. Ficha individual en PDF.
- Datos semilla demo: ~2.000 bienes generados para probar rendimiento.

**Cómo probar**
- [ ] Crear un bien de tipo "Aire" → recibe código `AIR00001`; el siguiente, `AIR00002`.
- [ ] Combinar 3 filtros; el total coincide; exportar a Excel trae exactamente esos registros.
- [ ] Con 2.000 bienes, el listado responde en menos de 1 segundo.
- [ ] Editar un bien y ver el cambio en la pestaña Historial.
- [ ] Edición masiva de 30 bienes → los 30 cambian y cada uno tiene su historial.
- [ ] Usuario con perfil restringido a un servicio solo ve bienes de ese servicio (también por API).
- [ ] CONSULTA puede ver y exportar, pero no editar.
- [ ] Descargar la ficha PDF de un bien.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 3 de docs/PLAN_ETAPAS.md. Implementa Proveedores y Bienes,
el módulo central. Revisa docs/referencia/03-bienes.png para los filtros y columnas (solo como guía).
Usa Specifications de JPA para los filtros dinámicos y aplica los perfiles de ubicación/tipo del
usuario en el servicio. Implementa historial con Envers, edición masiva, exportación Excel/PDF y
ficha PDF. Propón el plan (incluye modelo de datos e índices) y espera aprobación. Tests incluyendo
filtros, perfiles y aislamiento. Al final actualiza docs/AVANCE.md y commit "etapa 3: ...".
```

---

## Etapa 4 — Partes/componentes y Software/licencias

**Objetivo:** ver `docs/referencia/04-partes.png` y `05-software.png`.

**Modelo de datos**
- `parte`: codigo (autogenerado con prefijo del tipo de parte), bien_id (equipo al que pertenece,
  nullable si está en bodega), tipo_parte_id, marca_id, modelo_id, num_serie, capacidad, descripcion,
  localidad (hereda del bien si está asignada), calidad_id, proveedor/compra/garantía, estado. Auditado.
- `software`: codigo, marca_software_id, nombre_software_id, version_software_id, tipo_software_id,
  numero_licencia, cantidad_licencias, fecha_compra, valor, proveedor_id, fecha_vencimiento (para suscripciones).
- `instalacion_software`: software_id, bien_id, fecha_instalacion, fecha_desinstalacion, observacion.
  `asignadas` = instalaciones vigentes (calculado, no guardado).

**Alcance**
- CRUD y listados con filtros como en la referencia, exportación Excel/PDF, historial.
- Asignar/desasignar una parte a un bien (queda en el historial de ambos).
- Instalar/desinstalar software en bienes; no permitir instalar más licencias que las compradas
  (advertencia con opción de forzar solo ADMIN).
- En la ficha del bien (Etapa 3), agregar pestañas "Partes" y "Software instalado".
- Informes: licencias disponibles, software por equipo, catastro de software.

**Cómo probar**
- [ ] Crear una parte y asignarla a un bien; aparece en la ficha del bien.
- [ ] Software con 5 licencias: instalar en 5 equipos OK; el sexto muestra advertencia.
- [ ] Desinstalar libera una licencia.
- [ ] Informe de licencias disponibles muestra los números correctos.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 4 de docs/PLAN_ETAPAS.md. Implementa Partes/componentes y
Software/licencias reutilizando los patrones y componentes de la Etapa 3. Propón el plan y espera
aprobación. Tests incluyendo control de licencias y aislamiento. Al final actualiza
docs/AVANCE.md y commit "etapa 4: ...".
```

---

## Etapa 5 — Recepción y Altas

**Objetivo:** registrar la llegada de bienes (factura o guía) y darlos de alta en una ubicación.
Ver `docs/referencia/06-recepcion.png` y `07-altas.png`.

**Modelo de datos**
- `recepcion`: folio (correlativo), tipo_documento (`FACTURA`, `GUIA_DESPACHO`), numero_factura,
  fecha_factura, numero_guia, fecha_guia, proveedor_id, fecha_recepcion, orden_compra, observaciones,
  monto_total, estado (`BORRADOR`, `RECIBIDA`, `DADA_DE_ALTA` parcial/total).
- `recepcion_item`: recepcion_id, descripcion, tipo_equipo_id, marca_id, modelo_id, cantidad,
  valor_unitario, cantidad_dada_de_alta.
- `alta`: folio, numero_alta (`AA-NN`), fecha_alta, establecimiento/servicio/unidad/localizacion destino,
  centro_costo_id, recepcion_id (opcional), observaciones.
- `alta_item`: alta_id, bien_id.

**Alcance**
- Recepción: CRUD con ítems, filtros por fechas de factura/recepción, adjuntar PDF de la factura
  (guardar archivo en disco configurable, no en BD).
- Alta: desde una recepción, crear los bienes (uno por unidad de cada ítem, con número de serie
  ingresable en una grilla) y ubicarlos en la localidad destino. También alta manual de bienes ya creados.
- Al dar de alta, los bienes heredan proveedor, factura, fecha y valor de la recepción.
- Documento PDF "Acta de alta" con el detalle de bienes.
- Informes: resumen de recepciones y altas por período.

**Cómo probar**
- [ ] Registrar factura con 2 ítems: 3 notebooks y 2 monitores.
- [ ] Dar de alta los 5 → se crean 5 bienes con su código, proveedor, valor y ubicación.
- [ ] La recepción queda "dada de alta total"; con alta parcial queda "parcial".
- [ ] El acta PDF lista los 5 bienes con sus códigos.
- [ ] Folio de alta con formato `26-1`, `26-2`... y reinicia al cambiar de año.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 5 de docs/PLAN_ETAPAS.md. Implementa Recepción (facturas y
guías de despacho) y Altas, creando bienes a partir de los ítems recibidos. Usa los numeradores con
bloqueo pesimista. Propón el plan y espera aprobación. Tests de la generación de bienes, de los
folios concurrentes y de aislamiento. Actualiza docs/AVANCE.md y commit "etapa 5: ...".
```

---

## Etapa 5B — Depreciación

**Objetivo:** calcular la depreciación de cada bien y su valor libro. Es parte central del producto.

**Modelo de datos (propuesta, a confirmar al iniciar la etapa)**
- `vida_util_sii`: tabla de vidas útiles normales del SII (nueva y acelerada en años), cargada por
  migración Flyway y actualizable por SUPERADMIN.
- En `tipo de equipo` (catálogo): referencia a la vida útil SII que corresponde por defecto.
- En `bien`: fecha de inicio de depreciación (fecha de alta), valor depreciable, valor residual
  (por defecto $1), vida útil asignada en meses, método (`LINEAL_NORMAL`, `LINEAL_ACELERADA`).
- `depreciacion_periodo`: bien_id, anio, mes, valor_inicial, depreciacion_mes,
  depreciacion_acumulada, valor_libro (cierre mensual; un período cerrado no se recalcula).

**Alcance**
- Proceso de cierre mensual de depreciación por empresa (idempotente, con bloqueo).
- Ficha del bien: pestaña "Depreciación" con el detalle por período y el valor libro actual.
- Una baja detiene la depreciación desde el mes siguiente.
- Informes: depreciación del período, acumulada y valor libro por tipo / servicio / cuenta contable.

**Preguntas a resolver antes de implementar**
- ¿Norma tributaria SII (Art. 31 N°5 LIR) o contable pública (NICSP-CGR), o ambas en paralelo?
- ¿Se aplica corrección monetaria (IPC) o no?
- ¿Depreciación mensual o anual? ¿Desde el mes de alta o el siguiente?
- ¿Los bienes en comodato/arriendo se excluyen?

**Cómo probar**
- [ ] Bien de $1.200.000 con vida útil de 6 años (lineal) deprecia ≈ $16.667 al mes (72 meses); valor libro cuadra.
- [ ] Cerrar dos veces el mismo mes no duplica registros.
- [ ] Bien dado de baja deja de depreciar.

---

## Etapa 6 — Movimientos

**Objetivo:** cambiar la ubicación o el estado de los bienes de forma trazable.
Ver `docs/referencia/08-movimientos.png`.

**Modelo de datos**
- `movimiento`: folio, numero (`AA-NN`), tipo (`TRASLADO`, `ASIGNACION`, `PRESTAMO`, `DEVOLUCION`, `BAJA`),
  fecha_solicitud, solicitante, origen (establecimiento/servicio/unidad/localizacion),
  destino (ídem; no aplica a BAJA), responsable_destino, fecha_devolucion_esperada (préstamo),
  motivo_baja (baja), estado (`PENDIENTE`, `REALIZADO`, `ANULADO`), fecha_realizado, realizado_por,
  observaciones.
- `movimiento_item`: movimiento_id, bien_id (o parte_id), ubicacion_anterior (snapshot).

**Reglas**
- Al crear: estado `PENDIENTE`; los bienes no cambian todavía.
- Al marcar `REALIZADO`: se actualiza la ubicación/estado de cada bien en una transacción
  (traslado/asignación → nueva ubicación; préstamo → `EN_PRESTAMO`; devolución → vuelve a origen y
  `ACTIVO`; baja → `DADO_DE_BAJA`).
- Un bien no puede estar en dos movimientos pendientes a la vez.
- Una devolución se crea a partir de un préstamo.
- Anular solo si está pendiente.

**Alcance**
- Formulario único con el tipo como primer campo; los demás campos se adaptan al tipo.
- Agregar bienes por búsqueda, por lector de código de barras (input que recibe el código) o
  desde una selección del listado de bienes ("Mover" en la pantalla de Bienes).
- Listado con filtros: fechas, tipo, estado, origen, destino, bien.
- PDF del documento de cada tipo (guía de traslado, acta de asignación, comprobante de préstamo,
  acta de baja) con espacio para firmas.
- Informes: préstamos vigentes y vencidos, movimientos por servicio, resumen/detallado por período.
- La ficha del bien muestra sus movimientos.

**Cómo probar**
- [ ] Trasladar 3 bienes: hasta marcarlo realizado siguen en el origen; después, en el destino.
- [ ] Intentar incluir un bien que ya está en otro movimiento pendiente → error claro.
- [ ] Préstamo con fecha de devolución vencida aparece en el informe de vencidos.
- [ ] Devolución vuelve el bien a su ubicación original.
- [ ] Baja → el bien queda dado de baja y no aparece en el listado normal.
- [ ] Imprimir la guía de traslado en PDF.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 6 de docs/PLAN_ETAPAS.md. Implementa Movimientos con las
reglas indicadas (pendiente → realizado de forma transaccional, sin bienes en dos movimientos
pendientes). Un solo formulario que se adapta al tipo. Propón el plan, incluyendo la máquina de
estados, y espera aprobación. Tests de cada tipo de movimiento, de reglas y de aislamiento.
Actualiza docs/AVANCE.md y commit "etapa 6: ...".
```

---

## Etapa 7 — Contratos, garantías y mantenciones

**Objetivo:** ver `docs/referencia/11-contratos.png`.

**Modelo de datos**
- `contrato`: numero, proveedor_id, tipo_contrato_id (mantención, arriendo, garantía extendida, soporte),
  fecha_inicio, duracion_meses, fecha_termino (calculada), valor, periodicidad_pago, descripcion,
  archivo adjunto, estado.
- `contrato_bien`: contrato_id, bien_id.
- `mantencion`: bien_id, fecha, tipo (`PREVENTIVA`, `CORRECTIVA`), proveedor_id, contrato_id (opcional),
  costo, descripcion, documento.

**Alcance**
- CRUD de contratos y asociación de bienes (por selección desde el listado de bienes).
- Registro de mantenciones por bien; pestaña "Mantenciones y contratos" en la ficha del bien.
- Alertas en el inicio: contratos que vencen en los próximos 60 días y garantías por vencer.
- Informes: contratos vigentes, bienes con garantía vigente/vencida, costos de mantención por período.

**Cómo probar**
- [ ] Crear contrato de 12 meses asociado a 10 bienes; aparece en la ficha de cada uno.
- [ ] Contrato que vence en 30 días aparece en las alertas.
- [ ] Registrar 3 mantenciones y verlas sumadas en el informe de costos.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 7 de docs/PLAN_ETAPAS.md. Implementa Contratos, garantías y
mantenciones, con las alertas de vencimiento. Propón el plan y espera aprobación. Tests y
aislamiento. Actualiza docs/AVANCE.md y commit "etapa 7: ...".
```

---

## Etapa 8 — Etiquetas e informes

**Objetivo:** etiquetas imprimibles y un catálogo ordenado de informes.

**Alcance**
- **Etiquetas de bienes**: elegir bienes (selección o filtro) → PDF con etiquetas que incluyen
  código de barras Code128 (o QR, configurable), código, descripción corta y nombre de la empresa.
  Formato configurable (tamaño de etiqueta, filas/columnas por hoja A4, o una por página para
  impresora térmica).
- **Etiquetas de localidades**: una etiqueta por localización con su código, para pegar en la puerta
  (se usará en el inventario físico).
- **Hoja mural**: PDF por localización con todos los bienes que hay en ella, para pegar en la sala.
- **Pantalla "Informes"**: catálogo con todos los informes del sistema agrupados por módulo, cada
  uno con sus filtros y botones PDF/Excel. Incluir los ya hechos y completar los que falten:
  resumen por tipo, por servicio, por unidad; compras por período; garantías; detallado.
- Encabezado común en todos los PDF (empresa, título, filtros aplicados, fecha y usuario que lo generó).

**Cómo probar**
- [ ] Imprimir etiquetas de 20 bienes en hoja A4; el código se lee con un lector USB o con el celular.
- [ ] Hoja mural de una sala muestra exactamente los bienes de esa localización.
- [ ] Todos los informes del catálogo se generan en PDF y Excel sin error.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 8 de docs/PLAN_ETAPAS.md. Implementa etiquetas con código de
barras (ZXing), etiquetas de localidades, hoja mural y la pantalla de catálogo de informes con
encabezado común. Propón el plan y espera aprobación. Tests de generación (que el PDF se genere y
contenga los códigos esperados). Actualiza docs/AVANCE.md y commit "etapa 8: ...".
```

---

## Etapa 9 — Inventario físico

**Objetivo:** verificar en terreno qué bienes están realmente en cada lugar, usando el celular.

**Modelo de datos**
- `toma_inventario`: nombre, fecha_inicio, fecha_cierre, alcance (establecimiento/servicio/unidad),
  estado (`ABIERTA`, `CERRADA`), creada_por.
- `toma_lectura`: toma_id, localizacion_id, codigo_leido, bien_id (si existe), leido_por, fecha.

**Alcance**
- Vista móvil (responsive): elegir la toma → escanear la etiqueta de la localización → escanear los
  bienes uno tras otro con la cámara (`@zxing/browser`) o un lector USB/Bluetooth. Sonido o
  vibración de confirmación; lista de lo leído; permitir ingreso manual del código.
- **Conciliación** al cerrar la toma, por localización:
  - Encontrados en su lugar.
  - Encontrados en otra ubicación (opción de generar un traslado automático).
  - Faltantes (estaban en el sistema y no se leyeron).
  - Sobrantes (códigos leídos que no existen en el sistema).
- Informe de conciliación PDF/Excel.

**Cómo probar**
- [ ] Desde el celular (misma red, `ng serve --host 0.0.0.0`; la cámara exige HTTPS o localhost,
      usar un túnel o certificado de desarrollo), escanear una localización y 5 bienes.
- [ ] Conciliación muestra bien los 4 grupos.
- [ ] Generar traslado desde "encontrados en otra ubicación" crea el movimiento correcto.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 9 de docs/PLAN_ETAPAS.md. Implementa el inventario físico
con escaneo desde el navegador del celular y la conciliación. La vista de escaneo debe ser
mobile-first y rápida. Explica en el plan cómo probaré la cámara en desarrollo (requiere HTTPS).
Espera aprobación. Tests de la conciliación. Actualiza docs/AVANCE.md y commit "etapa 9: ...".
```

---

## Etapa 10 — Dashboard, auditoría e importación masiva

**Objetivo:** gestión e indicadores. Ver `docs/referencia/13-graficos.png` y `14-utilitarios.png`.

**Alcance**
- **Dashboard (inicio)**: tarjetas con totales (bienes activos, valor total, en préstamo, por dar de baja,
  contratos por vencer) + gráficos (barras/torta, elegibles):
  inversión anual en equipos; valor invertido por tipo de equipo, por unidad, por servicio y por
  establecimiento; costos de mantención por tipo de equipo y por marca.
  Consultas de agregación optimizadas (no traer filas a memoria).
- **Auditoría**: informe filtrable por usuario, fecha, módulo y tipo de acción, combinando
  `evento_auditoria` y las revisiones de Envers. Solo ADMIN.
- **Importación masiva de bienes desde Excel**: plantilla descargable; validación completa antes de
  grabar (vista previa con errores por fila); catálogos que no existen se pueden crear
  automáticamente o rechazar (opción); usa las homologaciones de la Etapa 2.
  Esta es la vía para la carga inicial de datos de cada empresa.
- **Cambio de descripciones**: reemplazar un ítem de catálogo por otro en todos los bienes
  (ej. unificar marcas duplicadas "H.P." → "HP") con vista previa y auditoría.
- Exportación general de datos de la empresa (Excel con una hoja por entidad) para que el cliente
  siempre pueda llevarse sus datos.

**Cómo probar**
- [ ] Los gráficos cuadran con un cálculo manual sobre los datos demo.
- [ ] Importar 1.000 bienes con 10 filas erróneas → vista previa marca las 10; al confirmar entran 990.
- [ ] Unificar dos marcas → todos los bienes quedan con la marca correcta y queda auditado.
- [ ] Informe de auditoría muestra quién hizo cada cambio.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 10 de docs/PLAN_ETAPAS.md. Implementa el dashboard con
gráficos de gestión, el informe de auditoría, la importación masiva de bienes con vista previa, el
cambio de descripciones y la exportación general. Propón el plan y espera aprobación. Tests de
agregaciones e importación. Actualiza docs/AVANCE.md y commit "etapa 10: ...".
```

---

## Etapa 11 — Despliegue

**Objetivo:** dejar el sistema funcionando en un VPS Linux (ej. Vultr Santiago) para varias empresas.

**Alcance**
- Build de producción: backend como `.jar` (o imagen Docker) y frontend compilado servido por Nginx/Apache.
- Proxy inverso: `/` → archivos de Angular; `/api` → backend. HTTPS con Let's Encrypt.
- Servicio systemd para el backend (o `docker compose` de producción), con variables de entorno
  para credenciales (nunca en el repositorio).
- PostgreSQL en el servidor, usuario de aplicación con permisos mínimos, sin acceso externo.
- Respaldo diario con `pg_dump` (cron), retención 30 días, copia fuera del servidor (Object Storage),
  y **script de restauración probado**.
- Logs con rotación; endpoint de salud para monitoreo.
- Firewall (solo 22, 80, 443), SSH solo con llave, actualizaciones de seguridad automáticas.
- Script o guía para crear una empresa nueva y su primer administrador.
- Documento `docs/OPERACION.md`: instalación, actualización de versión, respaldo, restauración,
  qué hacer si cae el servicio.

**Cómo probar**
- [ ] Acceder por `https://` desde fuera; el certificado es válido.
- [ ] Reiniciar el servidor → el sistema vuelve solo.
- [ ] Restaurar el respaldo de ayer en una BD de prueba y verificar que los datos están.
- [ ] Desplegar una versión nueva siguiendo `OPERACION.md` sin perder datos.

**Prompt**
```
Lee CLAUDE.md, docs/AVANCE.md y la Etapa 11 de docs/PLAN_ETAPAS.md. Prepara todo lo necesario para
desplegar en un VPS Ubuntu: configuración de producción, proxy inverso con HTTPS, servicio systemd
(o Docker Compose de producción, propón cuál y por qué), respaldos con script de restauración,
endurecimiento básico del servidor y docs/OPERACION.md. No ejecutes nada contra servidores reales;
genera los archivos y la guía paso a paso. Espera aprobación del plan. Commit "etapa 11: ...".
```

---

## Consejos para trabajar con Claude en VS Code

- **Una conversación por etapa.** Al empezar la etapa nueva, Claude lee `CLAUDE.md` y `AVANCE.md`
  y parte sin el ruido de la conversación anterior.
- **Si la etapa es grande** (3, 6, 10), pídele que la divida: "haz primero el backend con sus tests;
  cuando pase, seguimos con el frontend".
- **Revisa el plan con calma.** Corregir el plan es mucho más barato que corregir el código.
- **Cuando algo falle**, pégale el error completo y dile qué esperabas que pasara.
- **No aceptes "listo" sin ver los tests pasar** y sin probar tú la lista "Cómo probar".
- Si cambias una regla de negocio, pídele que también actualice `CLAUDE.md` o este plan.
