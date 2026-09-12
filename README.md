# Sistema de Gestión Hospitalaria — Backend

Servidor de una aplicación hospitalaria cliente–servidor: expone la lógica de negocio sobre un
**protocolo propio de sockets TCP con mensajes JSON**, persiste en MySQL con Hibernate y notifica
en tiempo real a los clientes conectados por WebSocket.

Cubre cuatro roles —administrador, médico, farmacéutico y paciente— sobre el flujo completo de
medicamentos: catálogo, prescripción, despacho en farmacia e histórico de recetas.

Proyecto académico desarrollado en la Universidad Nacional de Costa Rica (UNA).

El cliente de escritorio vive en un repositorio aparte:
[ProyectoPrograFrontEnd1](https://github.com/Killazz17/ProyectoPrograFrontEnd1).

## 🏛️ Arquitectura

El servidor no usa un framework web: implementa su propio servidor de sockets con un hilo por
cliente y un protocolo de petición/respuesta en JSON sobre líneas de texto.

```
Cliente Swing ──JSON por socket (7070)──► SocketServer ──► ClientHandler ──► Controller ──► Service ──► Hibernate ──► MySQL
      ▲                                                          │
      └────────────notificaciones (WebSocket, 7001)───────── MessageBroadcaster
```

| Puerto | Canal | Uso |
|---|---|---|
| `7070` | Socket TCP | Peticiones y respuestas de la aplicación |
| `7001` | WebSocket | Notificaciones difundidas a los clientes conectados |

**Concurrencia.** `SocketServer` acepta conexiones en el hilo principal y entrega cada socket a un
`ClientHandler` que corre en su propio hilo (`ClientHandler-N`). La lista de clientes activos es un
`CopyOnWriteArrayList`, de modo que difundir un mensaje no bloquea a quien está aceptando
conexiones.

**Persistencia.** `HibernateUtil` construye la `SessionFactory` a partir de `hibernate.properties`.
El esquema **no** lo genera Hibernate (`hibernate.hbm2ddl.auto=none`): lo versionan las migraciones
de Flyway en `src/main/resources/db/migrations`, que es la única fuente de verdad de la estructura
de la base.

### Estructura del proyecto

```
src/main/java/hospital/example/
├── Main.java                      # Arranca el SocketServer (7070) y el MessageBroadcaster (7001)
├── API/controllers/               # Un controlador por entidad del dominio
│   ├── AuthController.java        # Autenticación
│   ├── UsuarioController.java     # Usuarios y cambio de contraseña
│   ├── PacienteController.java
│   ├── MedicoController.java
│   ├── FarmaceutaController.java
│   ├── AdminController.java
│   ├── MedicamentoController.java # Catálogo de medicamentos
│   ├── RecetaController.java      # Recetas y su flujo de estados
│   └── MedicamentoPrescritoController.java
├── Server/
│   ├── SocketServer.java          # Acepta conexiones y administra los clientes activos
│   ├── ClientHandler.java         # Un hilo por cliente: lee, enruta y responde
│   └── MessageBroadcaster.java    # Difusión por WebSocket
├── WebSocket/                     # Endpoint y manejo de sesiones WebSocket
├── Domain/
│   ├── models/                    # Entidades mapeadas con Hibernate
│   │   ├── Usuario.java           # Entidad base de la que heredan los roles
│   │   ├── Admin.java · Medico.java · Farmaceuta.java · Paciente.java
│   │   ├── Medicamento.java · MedicamentoPrescrito.java · Receta.java
│   │   └── Mensaje.java           # Mensajería entre usuarios
│   └── dtos/                      # RequestDto y ResponseDto del protocolo
├── DataAccess/
│   ├── HibernateUtil.java         # SessionFactory
│   └── services/                  # Acceso a datos por entidad
└── Utilities/
```

### Modelo de datos

`Usuario` es la entidad base de la que heredan `Admin`, `Medico`, `Farmaceuta` y `Paciente`. Una
`Receta` pertenece a un paciente, la emite un médico y agrupa varios `MedicamentoPrescrito`
(medicamento + indicaciones + cantidad), cada uno referido al catálogo de `Medicamento`. `Mensaje`
soporta la comunicación entre usuarios del sistema.

## 📡 Protocolo

Cada petición es **una línea de JSON** terminada en salto de línea; la respuesta es otra línea de
JSON. El cliente abre el socket, envía una petición, lee la respuesta y cierra la conexión.

Petición (`RequestDto`): indica el **controlador** destino y la **operación** solicitada, más los
datos que esa operación necesite.

Respuesta (`ResponseDto`): bandera de éxito, mensaje y, cuando aplica, los datos serializados.

Controladores direccionables por nombre:

| Valor | Responsabilidad |
|---|---|
| `Auth` | Inicio de sesión |
| `Usuarios` | Gestión de usuarios y contraseñas |
| `Pacientes` | Expedientes de pacientes |
| `Medicos` | Médicos |
| `Farmaceutas` | Farmacéuticos |
| `Admins` | Administradores |
| `Medicamentos` | Catálogo de medicamentos |
| `Recetas` | Recetas y su histórico |
| `MedicamentosPrescritos` | Líneas de medicamento de cada receta |

Un inicio de sesión exitoso dispara además una notificación difundida por WebSocket con el nombre y
el rol del usuario que entró, que es lo que permite a los clientes reaccionar en vivo.

La serialización en ambos extremos es con **Gson**, de modo que el cliente y el servidor comparten
la forma de los DTO.

## 🚀 Puesta en marcha

Requisitos: **JDK 17+**, **Maven 3.8+** y **MySQL 8+**.

```bash
git clone https://github.com/Killazz17/ProyectoPrograBackEnd.git
cd ProyectoPrograBackEnd
```

1. Creá la base de datos vacía:

   ```sql
   CREATE DATABASE Proyecto2;
   ```

2. Configurá la conexión en `src/main/resources/hibernate.properties`. Se recomienda **no dejar
   credenciales en el repositorio** y leerlas del entorno:

   ```properties
   hibernate.connection.url=jdbc:mysql://localhost:3306/Proyecto2
   hibernate.connection.username=${DB_USER}
   hibernate.connection.password=${DB_PASSWORD}
   hibernate.hbm2ddl.auto=none
   ```

3. Aplicá las migraciones de Flyway (`src/main/resources/db/migrations`) y levantá el servidor:

   ```bash
   mvn clean install
   mvn exec:java -Dexec.mainClass="hospital.example.Main"
   ```

El servidor queda escuchando peticiones en el puerto `7070` y notificaciones en el `7001`. Con eso
arriba, ya se puede iniciar el cliente de escritorio.

## 🧰 Dependencias principales

| Dependencia | Uso |
|---|---|
| `hibernate-core` | Mapeo objeto–relacional |
| `mysql-connector-j` | Driver de MySQL |
| `flyway-mysql` | Migraciones versionadas del esquema |
| `jakarta.transaction-api` | API de transacciones |
| `gson` | Serialización JSON del protocolo |
| `Java-WebSocket` | Notificaciones en tiempo real |

## 👥 Autores

[@Killazz17](https://github.com/Killazz17) — Sebastián Benavides Madrigal ·
[@FSPABLO](https://github.com/FSPABLO) · [@carta01](https://github.com/carta01)
