# Arriva — Tourist Guide App

Arriva is an Android tourist guide app for Maharashtra (Pune-focused), with AI-powered chat and itinerary planning, offline map support, and a Firebase-backed content system.

## Features

- Browse places by category (historical, nature, religious, food, shopping, adventure, etc.)
- AI travel chat and itinerary generation (Claude, via a dedicated backend)
- Google Maps + OpenStreetMap (osmdroid) support
- Offline on-device translation (ML Kit)
- Weather info (OpenWeatherMap)
- Reviews, favorites, trip planning, and push notifications
- Email/password and Google Sign-In authentication

## Tech Stack

**Android app**
- Java & Kotlin, min SDK 24 / target SDK 34
- AndroidX UI (AppCompat, Material, ConstraintLayout, RecyclerView, ViewPager2)
- Room (local DB), Retrofit2 + Gson, Volley, Glide
- Google Play Services (Maps, Location, Places), osmdroid + osmbonuspack
- ML Kit Translate, Google Generative AI SDK (Gemini)

**Firebase**
- Authentication (email/password, Google Sign-In)
- Cloud Firestore (places, reviews, phrasebook, chat, trips, analytics)
- Cloud Storage (place images)
- Cloud Messaging (push notifications)

**Backend** (`arriva-backend/`)
- Node.js + Express, deployed on Render
- Anthropic Claude API for chat and itinerary generation
- Cloudinary for image uploads
- Rate-limited API routes, shared-secret (`X-App-Token`) auth

## Project Structure

```
Tourist_Guide_App/
├── app/                  # Android app source
│   └── google-services.json
├── arriva-backend/       # Node.js/Express backend (Render)
│   └── server.js
├── locales/              # en / hi / mr translation strings
├── firestore-*.rules     # Firestore security rules
├── render.yaml           # Render deployment config
└── ASSET_LICENSES.md     # Image attributions
```

## Setup

### 1. Android app

Add your API keys to `local.properties` in the project root (never commit real values):

```properties
GOOGLE_PLACES_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
OWM_API_KEY=your_key_here
ITINERARY_BACKEND_URL=https://your-backend-url.onrender.com
```

- **Google Places API key** — Google Cloud Console, after enabling the Places SDK/API.
- **Gemini API key** — Google AI Studio (used as an optional on-device AI fallback).
- **OWM API key** — OpenWeatherMap, for weather data.
- **Itinerary backend URL** — the deployed URL of `arriva-backend`.

Firebase is already wired up via `app/google-services.json`. If you're pointing this at your own Firebase project, replace that file with one from your own Firebase Console.

### 2. Backend (`arriva-backend/`)

Create a `.env` file inside `arriva-backend/`:

```properties
ANTHROPIC_API_KEY=your_key_here
ANTHROPIC_MODEL=claude-sonnet-4-6
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_key_here
CLOUDINARY_API_SECRET=your_secret_here
APP_TOKEN=a_shared_secret_for_your_app_to_send
PORT=3000
```

- **Anthropic API key** — from the Anthropic Console. Powers `/api/chat`, `/api/plan-trip`, and `/api/generate-itinerary`.
- **Cloudinary credentials** — used by `/api/upload-image`.
- **APP_TOKEN** — the Android app must send this in the `X-App-Token` header on every `/api/*` request.

Run locally:
```bash
cd arriva-backend
npm install
npm start
```

Deploy via `render.yaml` (Render reads `ANTHROPIC_API_KEY` as a secret env var; set the rest in the Render dashboard).

## Managing Place Data

Places live in the Firestore `places` collection. Rather than editing Firestore by hand, use the CSV-based admin uploader tool (`uploader/`) to bulk-add or update places:

```csv
name,category,city,imageRef,description,rating
Shaniwar Wada,History,Pune,places/shaniwar-wada/cover.jpg,Fort in Pune,4.5
Sinhagad Fort,Adventure,Pune,places/sinhagad-fort/cover.jpg,Hill fort,4.7
```

See the uploader's own README for usage.

## License & Attributions

Third-party image sources and licenses are documented in [`ASSET_LICENSES.md`](./ASSET_LICENSES.md).
