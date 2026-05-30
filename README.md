<div align="center">

![Kouda Tactical Logo](kouda_logo.png)

# KOUDA TACTICAL

**Browser de servidores para CS 1.6, CS:GO, TF2 y Half-Life**  
App Android nativa · Open Source · Sin anuncios

[![Build APK](https://github.com/TU_USUARIO/KOUDA/actions/workflows/build.yml/badge.svg)](https://github.com/TU_USUARIO/KOUDA/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%206.0%2B-orange)](https://github.com/TU_USUARIO/KOUDA/releases)

### [⬇️ DESCARGAR APK](https://github.com/TU_USUARIO/KOUDA/actions) · [📸 Capturas](#capturas) · [🚀 Features](#features)

</div>

---

## ¿Qué es Kouda Tactical?

Kouda Tactical es una app Android para monitorear servidores de juegos en tiempo real. Conecta directamente a los servidores usando el **protocolo Source Engine Query** — el mismo que usa Steam — sin intermediarios ni servidores propios.

Sabés exactamente cuántos jugadores hay, el ping, el mapa actual y si hay lugar disponible, todo desde tu celular.

---

## Features

### 🎮 Browser de Servidores
- Consulta en tiempo real via protocolo UDP Source Engine Query
- Muestra nombre, mapa, jugadores, ping y país
- Barra de ocupación con código de color verde → rojo
- Ping preciso con 3 mediciones (mediana)
- Filtros por juego: **CS 1.6 · CS:GO · TF2 · Half-Life**
- Ordenar por: Favoritos, Ping, Jugadores, Nombre
- Búsqueda en tiempo real por nombre, mapa o IP
- Descubrimiento automático via Gametracker

### 👥 Jugadores
- Escaneo de jugadores activos con nombre y puntuación
- Sistema de fallback: UDP directo → Gametracker → Steam API
- Top 1 resaltado con trofeo 🏆

### ⭐ Gestión de Servidores
- Favoritos con estrella
- Agregar servidores manualmente por IP:Puerto
- Eliminar servidores
- Tap largo copia la IP al portapapeles
- Compartir servidor por WhatsApp, Telegram, Discord, etc.

### 🔔 Slot Watcher
- Vigilancia manual: activás y te avisa cuando hay lugar
- **Vigilancia automática por servidor** con toggle individual
- Corre en **background con WorkManager** — funciona con la app cerrada
- Notificación nativa con vibración cuando se libera un slot

### 📊 Historial e Inteligencia
- Guarda el historial de cada servidor automáticamente
- Muestra consultas totales, promedio 24hs y **hora pico**
- Mini gráfico de barras con actividad reciente

### 🖼️ Widget de Pantalla de Inicio
- Muestra favoritos con jugadores y ping sin abrir la app
- Se actualiza automáticamente cada 30 minutos
- Compatible con launchers de Samsung, Motorola, Pixel y más

### 🎨 UI/UX
- Diseño dark con naranja neón
- Ícono adaptativo que se amolda a cada launcher (redondo, cuadrado, etc.)
- Pantalla de menú con animaciones (glow rotativo, grid de puntos)
- Animaciones de entrada escalonadas

---

## Instalación

### Opción A — Descargar APK directo
1. Entrá a [**Actions**](https://github.com/DOSKA-BIT/actions)
2. Tocá el último build exitoso (tilde verde ✅)
3. Bajá el artifact **Kouda-Debug-APK**
4. Instalá en tu Android (necesitás permitir instalar de fuentes desconocidas)

### Opción B — Compilar vos mismo
```bash
git clone https://github.com/DOSKA-BIT/KOUDA.git
cd KOUDA
./gradlew assembleDebug
```
El APK queda en `app/build/outputs/apk/debug/`

**Requisitos:** Android 8.0+ (API 26) · Gradle 8.9 · JDK 17

---

## Capturas

> *Coming soon — se agregan con el primer release público*

---

## Arquitectura

```
app/src/main/java/com/kouda/tactical/
├── MainActivity.kt              # Entry point
├── KoudaViewModel.kt            # Estado y lógica de negocio
├── MenuScreen.kt                # Pantalla de inicio con animaciones
├── ServerListScreen.kt          # Lista principal de servidores
├── ServerCards.kt               # Cards con barra de ocupación
├── Dialogs.kt                   # Dialogs de opciones, scan, agregar
├── Components.kt                # Componentes reutilizables
├── SlotWorker.kt                # WorkManager para notificaciones
├── KoudaWidget.kt               # Widget de pantalla de inicio
├── data/
│   └── Models.kt                # ServerInfo, PlayerInfo, historial
└── network/
    ├── SourceQuery.kt           # Protocolo UDP Source Engine Query
    ├── MasterServer.kt          # Consulta Master Server List de Valve
    └── ServerDiscovery.kt       # Descubrimiento via Gametracker
```

**Stack:** Kotlin · Jetpack Compose · WorkManager · Glance (widgets) · OkHttp · Gson

---

## Contribuir

1. Fork el repo
2. Creá una branch: `git checkout -b feature/mi-mejora`
3. Commiteá tus cambios
4. Push y abrí un Pull Request

Toda contribución es bienvenida — bugs, features, servidores para agregar al fallback, traducciones.

---

## Roadmap

- [ ] Soporte para Minecraft, Rust y ARK
- [ ] Modo LAN — escanear red WiFi local
- [ ] Estadísticas globales en la pantalla de menú
- [ ] Publicar en F-Droid
- [ ] Tema claro

---

## Licencia

MIT © 2025 — Libre para usar, modificar y distribuir.

---

<div align="center">

Hecho con ❤️ y mucho CS 1.6

**[⬆ Volver arriba](#kouda-tactical)**

</div>
