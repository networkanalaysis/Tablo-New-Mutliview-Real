# Tablo Multiview for Android TV

A production-quality, native Android TV / Google TV / Fire TV application designed for **Tablo 4th Gen** and network-attached Tablo OTA DVRs. Combines a modern 10-foot leanback television interface with high-performance **multi-channel live streaming** (up to 4 channels simultaneously in sports-style quad view).

---

## Key Highlights

- **4-Channel Simultaneous Multiview**: Watch up to four live OTA broadcast or FAST streaming channels simultaneously in a 2x2 grid, 1-Up + 3 thumbnails, or full-screen solo mode.
- **Native ExoPlayer / Media3 Playback**: True hardware-accelerated HLS video decoding across 4 dedicated player slots with low-latency tuning.
- **Instant D-Pad Audio Switching**: Effortlessly toggle live audio between any grid slot with single-click D-pad focus, highlighted by an active glowing audio status indicator.
- **Strict Tablo API Documentation Alignment**: Audited and matched against the unofficial Tablo API specification ([jessedp.github.io/tablo-api-docs](https://jessedp.github.io/tablo-api-docs/)), utilizing native local REST endpoints, `POST /batch` optimization, and local HLS playback.
- **Zero-Cloud Local Operation**: Discovers and streams directly from your Tablo hardware over the local network on port 8885 / port 80 without mandatory cloud accounts.
- **Hardware Tuner Allocation Monitor**: Real-time inspection of Tablo physical tuner occupancy (`GET /server/tuners`) and hardware details (`GET /server/info`).
- **Comprehensive EPG & DVR**: Native 10-foot Electronic Program Guide and recorded series/episodes library browser.

---

## Architectural Alignment with Tablo API

This application interfaces directly with the Tablo REST and streaming stack:

```
┌─────────────────────────────────────────────────────────────┐
│                    Tablo Multiview App                      │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │   MultiviewPlaybackManager (4 ExoPlayer Slots)      │   │
│   └──────────────────────┬──────────────────────────────┘   │
│                          │                                  │
│   ┌──────────────────────▼──────────────────────────────┐   │
│   │                 TabloRepository                     │   │
│   └────┬──────────────┬──────────────┬──────────────┬───┘   │
└────────┼──────────────┼──────────────┼──────────────┼───────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
   ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
   │ Discovery │  │ Channels  │  │   Guide   │  │ Streaming │
   │  Service  │  │  Service  │  │  Service  │  │  Service  │
   └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
         │              │              │              │
═════════╪══════════════╪══════════════╪══════════════╪════════
         │              │              │              │
         ▼              ▼              ▼              ▼
 ┌───────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
 │ Assocserver / │ │ Port 8885    │ │ Port 8885    │ │ Port 80 HLS  │
 │ UDP Broadcast │ │ POST /batch  │ │ Airings EPG  │ │ /stream/*.m3u8│
 └───────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
                       Local Tablo Hardware
```

### 1. Device Discovery
The application adheres to the three documented Tablo discovery methods in priority order:
1. **Assocserver Discovery**: Queries `https://api.tablotv.com/assocserver/getipinfo/` to resolve all Tablo CPEs bound to the local public IP subnet without requiring credentials.
2. **Local UDP Broadcast**: Emits broadcast datagrams with ASCII payload `"Tablo"` on UDP port 8881/8882 to detect devices offline.
3. **Direct IP Probing**: Queries `GET http://<ip>:8885/server/info` and `GET http://<ip>:8885/ping` for instant pairing.

### 2. High-Performance Batch Processing (`POST /batch`)
Rather than issuing hundreds of individual HTTP requests for channels and guide data, the app uses the documented Tablo batch API:
```http
POST /batch HTTP/1.1
Host: 192.168.1.150:8885
Content-Type: application/json; charset=utf-8

["/guide/channels/12345", "/guide/channels/12346", "/recordings/series/episodes/9876"]
```
This reduces channel and recording load times from seconds to a single round-trip.

### 3. Native Live Video Streaming (`/watch`)
To launch live streams, the app invokes the local watch endpoint:
```http
POST /guide/channels/{channel_id}/watch HTTP/1.1
Host: 192.168.1.150:8885
Content-Type: application/json; charset=utf-8

{}
```
The Tablo allocates an internal physical tuner and responds with:
```json
{
  "playlist_url": "http://192.168.1.150:80/stream/pl.m3u8?c=...",
  "token": "a1b2c3d4e5",
  "expires": "2026-09-15T21:00:00Z",
  "keepalive": 60
}
```
The application then binds the returned HLS URL directly to the target `ExoPlayer` instance on port 80.

### 4. Tuner Allocation & Multi-Channel Constraints
- **Physical OTA Tuners**: Tablo 4th Gen units come with either 2 or 4 physical ATSC tuners. Each unique live broadcast channel requires 1 physical tuner.
- **Tuner Multiplexing**: If multiple multiview slots tune the same OTA broadcast channel, the player reuses the existing active stream, consuming only 1 physical tuner.
- **FAST Channels**: Virtual streaming/FAST channels do not consume physical ATSC tuners, enabling full 4-stream multiview even on 2-tuner hardware.
- **Real-Time Monitoring**: The Settings screen continuously displays active tuner occupancy via `GET /server/tuners`.

### 5. Authentication & HMAC-MD5 Signing
- **Local API**: Unauthenticated by default for standard queries, channel batches, and stream requests.
- **Device HMAC**: Includes full implementation of Tablo's HMAC-MD5 signing scheme (`tablo:<device_key>:<signature>`) in `TabloHmac.kt` for secured operations.
- **Cloud Account**: Supports optional sign-in via `lighthousetv.ewscloud.com` for multi-device account synchronization.

---

## Multiview Experience & Layout Modes

| Layout Mode | Description | Remote Control Shortcut |
| :--- | :--- | :--- |
| **Quad Grid (2x2)** | 4 equal-sized video slots playing simultaneously. | D-Pad navigation between slots |
| **Main + 3 (1-Up + 3)** | 1 large primary focus window on the left, 3 stacked secondary windows on the right. | Switch layout in Top Bar |
| **Solo Mode (1-Up)** | Fullscreen single-channel experience with instant return to grid. | Press **Center/Select** on any focused slot |

### Audio & Focus Management
- Only one video stream plays audible audio at a time to prevent audio cacophony.
- The active audio stream is highlighted with an **amber glowing border** and a visual **Active Audio Pill**.
- Pressing **Select / Enter** on any muted slot immediately switches audio focus to that slot.

---

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt               # Entry point, TV Scaffold, navigation, and top bar
├── data/
│   └── TabloRepository.kt        # Central data repository, state management, persistence
├── model/
│   └── TabloModels.kt            # Data models (Channels, Streams, Airings, Tuners, CPEs)
├── network/
│   ├── TabloAuthService.kt       # Tablo Cloud authentication & token management
│   ├── TabloChannelService.kt    # Channel retrieval with POST /batch support
│   ├── TabloDiscoveryService.kt  # Assocserver, UDP broadcast, and REST probing
│   ├── TabloGuideService.kt      # Electronic Program Guide (EPG) airings
│   ├── TabloHmac.kt              # HMAC-MD5 request authentication calculator
│   ├── TabloRecordingService.kt  # DVR recordings with POST /batch support
│   └── TabloStreamService.kt     # Live & recording HLS stream startup (/watch)
├── playback/
│   ├── MultiviewPlaybackManager.kt # Coordinates 4 PlayerSlots, audio routing, layout
│   └── PlayerSlot.kt             # Individual ExoPlayer instance, surface, and lifecycle
└── ui/
    ├── components/
    │   └── TvTopBar.kt           # TV navigation bar with D-pad focus indicators
    ├── guide/
    │   └── GuideScreen.kt        # 10-foot EPG grid screen
    ├── multiview/
    │   ├── MultiviewScreen.kt    # 2x2, 1-Up+3, and Solo video grid composables
    │   └── VideoPlayerSlotView.kt# AndroidView embedding ExoPlayer SurfaceView
    ├── recordings/
    │   └── RecordingsScreen.kt   # DVR recordings library browser
    ├── settings/
    │   └── SettingsScreen.kt     # Auto-discovery, manual IP, tuners, and hardware info
    └── theme/
        ├── Color.kt              # M3 TV color palette with high-contrast borders
        ├── Theme.kt              # MaterialTheme configuration
        └── Type.kt               # Typography system optimized for 10-foot viewing
```

---

## Building and Testing

### Prerequisites
- **Android SDK**: API 35 (Android 15) compile target, `minSdk 26` (Android 8.0 Oreo).
- **JDK**: Java 17+.
- **Build Tool**: Gradle with Kotlin DSL.

### Compile Debug APK
```bash
gradle assembleDebug
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

All network models, HMAC calculation algorithms, and batch parser services include comprehensive unit tests in `app/src/test/java/com/example/`.

---

## Disclaimers

- **Unofficial Application**: This project is an independent community development and is not affiliated with, endorsed by, or associated with Nuvyyo Inc. or Tablo TV.
- **Reference Material**: API behavior is developed in accordance with open community documentation at [jessedp.github.io/tablo-api-docs](https://jessedp.github.io/tablo-api-docs/).
