# Endpoint de Listado de Capacidades - Documentación

## Descripción General

El endpoint GET `/capacity` permite listar capacidades con paginación, ordenamiento y tecnologías asociadas.

## Características Implementadas

### ✅ Paginación
- Basada en offset/limit con R2DBC
- Paginación reactiva sin bloqueo
- Valores por defecto: page=0, size=10
- Tamaño máximo: 100 elementos por página

### ✅ Ordenamiento Parametrizable
- **Por nombre** (NAME): Orden alfabético
- **Por cantidad de tecnologías** (TECHNOLOGY_COUNT): Número de tecnologías asociadas
- **Dirección**: ASC (ascendente) o DESC (descendente)

### ✅ Enriquecimiento con Tecnologías
- Cada capacidad incluye el listado de tecnologías (id y nombre)
- Consulta al microservicio externo de tecnologías
- Procesamiento paralelo para mejor rendimiento

## Endpoint

### GET /capacity

**Headers:**
- `x-message-id` (requerido): UUID para trazabilidad

**Query Parameters:**

| Parámetro | Tipo | Requerido | Default | Descripción |
|-----------|------|-----------|---------|-------------|
| page | integer | No | 0 | Número de página (base 0) |
| size | integer | No | 10 | Tamaño de página (máx: 100) |
| sortBy | string | No | NAME | Campo de ordenamiento: `NAME` o `TECHNOLOGY_COUNT` |
| sortDirection | string | No | ASC | Dirección: `ASC` o `DESC` |

## Ejemplos de Uso

### 1. Listado básico (primera página, 10 elementos)

```bash
curl -X GET "http://localhost:8080/capacity" \
  -H "x-message-id: $(uuidgen)"
```

**PowerShell:**
```powershell
$headers = @{
    "x-message-id" = [guid]::NewGuid().ToString()
}

Invoke-RestMethod -Uri "http://localhost:8080/capacity" `
    -Method GET `
    -Headers $headers
```

### 2. Ordenar por nombre descendente

```bash
curl -X GET "http://localhost:8080/capacity?sortBy=NAME&sortDirection=DESC" \
  -H "x-message-id: $(uuidgen)"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/capacity?sortBy=NAME&sortDirection=DESC" `
    -Method GET `
    -Headers @{ "x-message-id" = [guid]::NewGuid().ToString() }
```

### 3. Ordenar por cantidad de tecnologías (ascendente)

```bash
curl -X GET "http://localhost:8080/capacity?sortBy=TECHNOLOGY_COUNT&sortDirection=ASC" \
  -H "x-message-id: $(uuidgen)"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/capacity?sortBy=TECHNOLOGY_COUNT&sortDirection=ASC" `
    -Method GET `
    -Headers @{ "x-message-id" = [guid]::NewGuid().ToString() }
```

### 4. Paginación - Segunda página con 20 elementos

```bash
curl -X GET "http://localhost:8080/capacity?page=1&size=20" \
  -H "x-message-id: $(uuidgen)"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/capacity?page=1&size=20" `
    -Method GET `
    -Headers @{ "x-message-id" = [guid]::NewGuid().ToString() }
```

### 5. Ordenar por tecnologías descendente con paginación

```bash
curl -X GET "http://localhost:8080/capacity?page=0&size=15&sortBy=TECHNOLOGY_COUNT&sortDirection=DESC" \
  -H "x-message-id: $(uuidgen)"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/capacity?page=0&size=15&sortBy=TECHNOLOGY_COUNT&sortDirection=DESC" `
    -Method GET `
    -Headers @{ "x-message-id" = [guid]::NewGuid().ToString() }
```

## Respuesta

**Status Code:** 200 OK

**Body:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Backend Development",
      "description": "Backend development with modern technologies",
      "technologies": [
        {
          "id": 1,
          "name": "Java"
        },
        {
          "id": 2,
          "name": "Spring Boot"
        },
        {
          "id": 3,
          "name": "PostgreSQL"
        },
        {
          "id": 4,
          "name": "Docker"
        },
        {
          "id": 5,
          "name": "Kubernetes"
        }
      ]
    },
    {
      "id": 2,
      "name": "Frontend Development",
      "description": "Modern frontend frameworks and tools",
      "technologies": [
        {
          "id": 6,
          "name": "React"
        },
        {
          "id": 7,
          "name": "TypeScript"
        },
        {
          "id": 8,
          "name": "Redux"
        }
      ]
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

### Estructura de la Respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| content | array | Lista de capacidades con sus tecnologías |
| content[].id | long | ID de la capacidad |
| content[].name | string | Nombre de la capacidad |
| content[].description | string | Descripción de la capacidad |
| content[].technologies | array | Tecnologías asociadas (id, name) |
| page | integer | Número de página actual (base 0) |
| size | integer | Tamaño de página solicitado |
| totalElements | long | Total de elementos disponibles |
| totalPages | integer | Total de páginas disponibles |
| first | boolean | Es la primera página |
| last | boolean | Es la última página |

## Casos de Error

### Error 500: Error en servicio de tecnologías

Si el microservicio de tecnologías no está disponible:

```json
{
  "code": "500",
  "message": "Something went wrong, please try again",
  "identifier": "uuid-message-id",
  "date": "2026-01-15T10:30:00Z",
  "errors": [
    {
      "code": "500",
      "message": "Error communicating with technology service",
      "param": ""
    }
  ]
}
```

### Error 400: Parámetros inválidos

Si se proporciona un valor inválido para sortBy o sortDirection:

```json
{
  "code": "500",
  "message": "Something went wrong, please try again",
  "identifier": "uuid-message-id",
  "date": "2026-01-15T10:30:00Z",
  "errors": [
    {
      "code": "500",
      "message": "Something went wrong, please try again",
      "param": ""
    }
  ]
}
```

## Arquitectura de la Implementación

### Flujo de Ejecución

1. **Request** → RouterRest → CapacityHandlerImpl
2. **Extracción de parámetros** → PaginationRequest con validaciones
3. **Consulta paginada** → DatabaseClient con SQL optimizado
4. **Conteo total** → En paralelo con la consulta paginada
5. **Enriquecimiento** → Para cada capacidad:
   - Obtener IDs de tecnologías de capacity_technology
   - Consultar microservicio externo con WebClient
   - Mapear a TechnologySummary
6. **Respuesta** → PageResponse con metadata completa

### Query SQL Generada

#### Ordenamiento por nombre (ASC):
```sql
SELECT c.id, c.name, c.description
FROM capacity c
LEFT JOIN capacity_technology ct ON c.id = ct.capacity_id
GROUP BY c.id, c.name, c.description
ORDER BY c.name ASC
LIMIT :limit OFFSET :offset
```

#### Ordenamiento por cantidad de tecnologías (DESC):
```sql
SELECT c.id, c.name, c.description
FROM capacity c
LEFT JOIN capacity_technology ct ON c.id = ct.capacity_id
GROUP BY c.id, c.name, c.description
ORDER BY COUNT(ct.technology_id) DESC, c.name ASC
LIMIT :limit OFFSET :offset
```

## Buenas Prácticas Implementadas

### ✅ Paginación Reactiva con R2DBC
- Uso de `DatabaseClient` para queries personalizadas
- LIMIT y OFFSET para eficiencia en BD
- No se carga todo en memoria

### ✅ SOLID Principles

**Single Responsibility:**
- Handler: Manejo de HTTP
- UseCase: Lógica de negocio
- Adapter: Acceso a datos

**Open/Closed:**
- Extensible mediante PaginationRequest
- Nuevos campos de ordenamiento sin modificar código existente

**Dependency Inversion:**
- Dominio no depende de infraestructura
- Puertos (interfaces) en el dominio

### ✅ Clean Code

**Nombres descriptivos:**
- `enrichCapacitiesWithTechnologies`
- `PaginationRequest.SortField`
- `CapacityWithTechnologies`

**Métodos pequeños:**
- Cada método tiene una responsabilidad
- Separación de concerns

**Validaciones:**
- Límites en PaginationRequest (max 100)
- Valores por defecto sensatos

### ✅ Programación Reactiva

**Non-blocking:**
- Operaciones de BD con R2DBC
- Llamadas HTTP con WebClient
- Todo el flujo es reactivo

**Composición:**
- `Mono.zip` para operaciones paralelas
- `flatMap` para encadenar operaciones
- `Flux` para streams de datos

**Error Handling:**
- `onErrorResume` para manejo de errores
- Logging en cada paso
- Context propagation de messageId

## Performance

### Optimizaciones Implementadas

1. **Consulta paralela**: Count y datos se obtienen en paralelo
2. **Paginación en BD**: Solo se traen los registros necesarios
3. **Batch de tecnologías**: Una sola llamada al servicio externo por capacidad
4. **Stream processing**: Procesamiento reactivo sin bloqueos

### Recomendaciones

- **Page size óptimo**: 10-50 elementos
- **Índices recomendados**:
  - `CREATE INDEX idx_capacity_name ON capacity(name)`
  - `CREATE INDEX idx_capacity_tech_capacity_id ON capacity_technology(capacity_id)`

## Testing

### Escenarios a probar

1. ✅ Paginación básica
2. ✅ Ordenamiento por nombre (ASC/DESC)
3. ✅ Ordenamiento por cantidad de tecnologías (ASC/DESC)
4. ✅ Página vacía (más allá del total)
5. ✅ Capacidades sin tecnologías
6. ✅ Servicio de tecnologías caído
7. ✅ Parámetros inválidos

### Ejemplo de Test Manual

```bash
# 1. Crear algunas capacidades primero
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-1" \
  -d '{"name": "Backend", "description": "Backend dev", "technologyIds": [1,2,3,4,5]}'

# 2. Listar capacidades ordenadas por nombre
curl -X GET "http://localhost:8080/capacity?sortBy=NAME&sortDirection=ASC" \
  -H "x-message-id: test-2"

# 3. Listar capacidades ordenadas por tecnologías
curl -X GET "http://localhost:8080/capacity?sortBy=TECHNOLOGY_COUNT&sortDirection=DESC" \
  -H "x-message-id: test-3"
```

## Microservicio de Tecnologías

### Nuevo Endpoint Agregado

**POST /technology/by-ids**

Retorna las tecnologías completas (id y nombre) para los IDs proporcionados.

**Request:**
```json
{
  "ids": [1, 2, 3]
}
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Java"
  },
  {
    "id": 2,
    "name": "Spring Boot"
  },
  {
    "id": 3,
    "name": "PostgreSQL"
  }
]
```

## Conclusión

El endpoint de listado está completamente implementado con:
- ✅ Paginación eficiente
- ✅ Ordenamiento parametrizable
- ✅ Tecnologías enriquecidas
- ✅ Programación reactiva
- ✅ Arquitectura limpia
- ✅ SOLID y Clean Code
- ✅ Manejo de errores robusto

¡Listo para usar en producción! 🚀

