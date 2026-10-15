# AudiusDesk

A lightweight desktop client for the [Audius](https://audius.co) music network, built with Kotlin and Jetpack Compose for Desktop.

<div style="display:flex; gap:12px; overflow-x:auto;">
<img src="screenshots/Screenshot%202026-10-15%20184631.png" width="500"/>
<img src="screenshots/Screenshot%202026-10-15%20184739.png" width="500"/>
</div>

## Features

- **Trending & Search:** Browse the weekly charts or search tracks with live debounce.
- **Audio Playback:** Gapless queue auto-advance, shuffle, repeat modes, and seeking.
- **Network Resilience:** Dynamic Audius discovery node rotation, auto-failover, and retry policies.
- **Desktop First:** Dark Material 3 UI with local caching for low latency.

## Tech Stack

- UI: JetBrains Compose for Desktop (Material 3) + Coil 3 for artwork.
- Audio Engine: Judio (JavaX Sound / JLayer MP3 streaming).
- Networking: Ktor Client (Java engine) talking directly to decentralized Audius discovery/content nodes.
- State Management: Kotlin Coroutines (StateFlow / SharedFlow).