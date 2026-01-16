# Ejemplos de Requests para API de Capacidades

## 1. Crear Capacidad - Caso Exitoso

```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: $(uuidgen)" \
  -d '{
    "name": "Backend Development",
    "description": "Backend development with modern technologies and best practices",
    "technologyIds": [1, 2, 3, 4, 5]
  }'
```

### PowerShell
```powershell
$headers = @{
    "Content-Type" = "application/json"
    "x-message-id" = [guid]::NewGuid().ToString()
}

$body = @{
    name = "Backend Development"
    description = "Backend development with modern technologies and best practices"
    technologyIds = @(1, 2, 3, 4, 5)
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/capacity" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

## 2. Verificar Existencia de Capacidades

```bash
curl -X POST http://localhost:8080/capacity/checking \
  -H "Content-Type: application/json" \
  -H "x-message-id: $(uuidgen)" \
  -d '{
    "ids": [1, 2, 3]
  }'
```

### PowerShell
```powershell
$headers = @{
    "Content-Type" = "application/json"
    "x-message-id" = [guid]::NewGuid().ToString()
}

$body = @{
    ids = @(1, 2, 3)
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/capacity/checking" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

## 3. Casos de Error - Validaciones

### Error: Menos de 3 tecnologías
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-min-tech" \
  -d '{
    "name": "Mobile Development",
    "description": "Mobile app development",
    "technologyIds": [1, 2]
  }'
```

**Respuesta esperada:**
```json
{
  "code": "400",
  "message": "Bad Parameters, please verify data",
  "identifier": "test-min-tech",
  "date": "2026-01-15T10:30:00Z",
  "errors": [
    {
      "code": "400",
      "message": "Capacity must have at least 3 technologies",
      "param": "technologyIds"
    }
  ]
}
```

### Error: Más de 20 tecnologías
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-max-tech" \
  -d '{
    "name": "Full Stack Development",
    "description": "Full stack development",
    "technologyIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21]
  }'
```

### Error: Tecnologías duplicadas
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-dup-tech" \
  -d '{
    "name": "Frontend Development",
    "description": "Frontend development with modern frameworks",
    "technologyIds": [1, 2, 3, 1, 4]
  }'
```

**Respuesta esperada:**
```json
{
  "code": "400",
  "message": "Bad Parameters, please verify data",
  "identifier": "test-dup-tech",
  "date": "2026-01-15T10:30:00Z",
  "errors": [
    {
      "code": "400",
      "message": "Capacity cannot have duplicate technologies",
      "param": "technologyIds"
    }
  ]
}
```

### Error: Nombre vacío
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-empty-name" \
  -d '{
    "name": "",
    "description": "Some description",
    "technologyIds": [1, 2, 3]
  }'
```

### Error: Nombre demasiado largo
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-long-name" \
  -d '{
    "name": "This is a very long name that exceeds the maximum allowed length of fifty characters",
    "description": "Some description",
    "technologyIds": [1, 2, 3]
  }'
```

### Error: Capacidad ya existe
```bash
# Primer request - exitoso
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-create-1" \
  -d '{
    "name": "DevOps",
    "description": "DevOps practices and tools",
    "technologyIds": [1, 2, 3]
  }'

# Segundo request - error (nombre duplicado)
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-create-2" \
  -d '{
    "name": "DevOps",
    "description": "Different description",
    "technologyIds": [4, 5, 6]
  }'
```

### Error: Tecnologías no existen
```bash
curl -X POST http://localhost:8080/capacity \
  -H "Content-Type: application/json" \
  -H "x-message-id: test-invalid-tech" \
  -d '{
    "name": "Cloud Computing",
    "description": "Cloud computing platforms and services",
    "technologyIds": [999, 998, 997]
  }'
```

**Respuesta esperada:**
```json
{
  "code": "400",
  "message": "Bad Parameters, please verify data",
  "identifier": "test-invalid-tech",
  "date": "2026-01-15T10:30:00Z",
  "errors": [
    {
      "code": "400",
      "message": "Some technologies do not exist",
      "param": "technologyIds"
    }
  ]
}
```

## 4. Ejemplos de Capacidades Válidas

### Backend Development
```json
{
  "name": "Backend Development",
  "description": "Development of server-side applications and APIs",
  "technologyIds": [1, 2, 3, 4, 5]
}
```

### Frontend Development
```json
{
  "name": "Frontend Development",
  "description": "Development of client-side web applications",
  "technologyIds": [6, 7, 8, 9, 10]
}
```

### DevOps
```json
{
  "name": "DevOps Engineering",
  "description": "Infrastructure automation and continuous delivery",
  "technologyIds": [11, 12, 13, 14, 15]
}
```

### Mobile Development
```json
{
  "name": "Mobile Development",
  "description": "Native and cross-platform mobile app development",
  "technologyIds": [16, 17, 18, 19, 20]
}
```

### Data Engineering
```json
{
  "name": "Data Engineering",
  "description": "Big data processing and analytics pipelines",
  "technologyIds": [21, 22, 23, 24, 25]
}
```

## Notas

1. **x-message-id**: Es obligatorio para el tracking de requests
2. **technologyIds**: Debe ser un array de IDs válidos (entre 3 y 20)
3. **name**: Máximo 50 caracteres, único
4. **description**: Máximo 90 caracteres
5. **BASE_URL_TECH**: Configurar la variable de entorno con la URL del microservicio de tecnologías

## Configuración del Microservicio Externo

Asegúrate de que el microservicio de tecnologías esté corriendo y configurado:

```bash
# Variable de entorno
export BASE_URL_TECH=http://localhost:8081

# O en application.yaml
external:
  technology:
    base-url: http://localhost:8081
```

