<div align="center">

![Kouda Tactical Logo](kouda_logo.png)

# KOUDA TACTICAL

**Browser de servidores para CS 1.6, CS:GO, TF2 y Half-Life**  
App Android nativa · Open Source · Sin anuncios

[![Build APK](https://github.com/DOSKA-BIT/KOUDA/actions/workflows/build.yml/badge.svg)](https://github.com/DOSKA-BIT/KOUDA/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-orange)](https://github.com/DOSKA-BIT/KOUDA/releases/latest)
[![Discord](https://img.shields.io/discord/66d3bPqdwM?label=Discord&logo=discord&color=5865F2)](https://discord.gg/66d3bPqdwM)

### [⬇️ DESCARGAR APK](https://github.com/DOSKA-BIT/KOUDA/releases/latest/download/app-debug.apk) · [🌐 Página web](https://doska-bit.github.io/KOUDA/) · [💬 Discord](https://discord.gg/66d3bPqdwM)

</div>

---

## ¿Qué es Kouda Tactical?

Kouda Tactical es una app Android para monitorear servidores de juegos en tiempo real. Conecta directamente a los servidores usando el **protocolo Source Engine Query** — el mismo que usa Steam — sin intermediarios ni servidores propios.

Sabés exactamente cuántos jugadores hay, el ping, el mapa actual y si hay lugar disponible, todo desde tu celular.

---

## Instalación rápida

1. **[Descargá el APK](https://github.com/DOSKA-BIT/KOUDA/releases/latest/download/app-debug.apk)** — link directo, siempre la última versión
2. En Android: **Ajustes → Seguridad → Permitir fuentes desconocidas**
3. Abrí el APK descargado e instalá

> Primera vez instalando APKs externos? Mirá la [guía paso a paso](https://doska-bit.github.io/KOUDA/#instalacion)

---

## Features

### 🎮 Browser de Servidores
- Consulta en tiempo real via protocolo UDP Source Engine Query
- Muestra nombre, mapa, jugadores, ping y país
- Barra de ocupación con código de color verde → rojo
- Ping preciso con 3 mediciones (mediana)
- Filtros por juego: **CS 1.6 · CS:GO · TF2 · Half-Life**
- Ordenar por: Favoritos, Ping, Jugadores, Nombre

### 🌎 Explorar y Buscar
- **Mis Servidores** — los que agregaste vos, separados
- **Explorar** — top servidores activos por juego, Sudamérica primero
- **Buscar** — buscá por nombre de servidor en internet

### 🔔 Slot Watcher
- Vigilancia automática por servidor con toggle individual
- Corre en **background** — funciona con la app cerrada
- Notificación nativa cuando se libera un slot

### 📊 Historial e Inteligencia
- Historial de consultas por servidor
- Promedio 24hs y hora pico
- Mini gráfico de actividad reciente

### 🖼️ Widget de Pantalla de Inicio
- Favoritos con jugadores y ping sin abrir la app
- Compatible con cualquier launcher Android

---

## Arquitectura

```
app/src/main/java/com/kouda/tactical/
├── MainActivity.kt
├── KoudaViewModel.kt
├── MenuScreen.kt
├── ServerListScreen.kt
├── ServerCards.kt
├── Dialogs.kt
├── Components.kt
├── SlotWorker.kt
├── KoudaWidget.kt
├── data/Models.kt
└── network/
    ├── SourceQuery.kt
    ├── ServerBrowser.kt
    └── ServerDiscovery.kt
```

**Stack:** Kotlin · Jetpack Compose · WorkManager · Glance · OkHttp · Gson

---

## Comunidad

¿Preguntas, sugerencias o querés mostrar tu servidor? Unite al Discord:

**[💬 discord.gg/66d3bPqdwM](https://discord.gg/66d3bPqdwM)**

---

## Contribuir

1. Fork el repo
2. `git checkout -b feature/mi-mejora`
3. Commiteá y abrí un Pull Request

---

## Roadmap

- [ ] Soporte para Minecraft, Rust y ARK
- [ ] Modo LAN — escanear red WiFi local
- [ ] Estadísticas globales en el menú
- [ ] Publicar en F-Droid

---

## Licencia

MIT © 2025 DOSKA-BIT

---

<div align="center">

**[⬇ Descargar APK](https://github.com/DOSKA-BIT/KOUDA/releases/latest/download/app-debug.apk) · [🌐 Web](https://doska-bit.github.io/KOUDA/) · [💬 Discord](https://discord.gg/66d3bPqdwM)**

Hecho con ❤️ y mucho CS 1.6

</div>
