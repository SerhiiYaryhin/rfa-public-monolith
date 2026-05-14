# Документація API: Автентифікація (/api/v1/auth/login)
Дата: 2026-05-13

Цей документ описує контракт взаємодії з ендпоінтом автентифікації для мобільного застосунку.

## 1. Запит (Request)
Ендпоінт приймає POST-запит з тілом у форматі JSON.

**URL:** `/api/v1/auth/login`
**Method:** `POST`
**Content-Type:** `application/json`

### Тіло запиту (Body):
```json
{
  "username": "ваш_логін",
  "password": "ваш_пароль"
}
```

## 2. Відповідь (Response)

### Успішна автентифікація (HTTP 200 OK):
Сервер повертає JSON-об'єкт із токеном доступу.
```json
{
  "token": "eyJhbGciOiJIUzI1..."
}
```

### Помилка автентифікації (HTTP 401 Unauthorized / 403 Forbidden):
Сервер повертає стандартну структуру помилки (відповідно до `GlobalExceptionHandler`):
```json
{
  "timestamp": "2026-05-13T14:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid user request!",
  "path": "/api/v1/auth/login"
}
```

---
*Примітка: На даному етапі реалізовано тільки `accessToken`. Механізм `refreshToken` буде додано в наступних ітераціях розвитку API.*
