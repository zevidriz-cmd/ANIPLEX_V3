# AniStream — Android Anime Streaming App
## Complete Build Plan for Antigravity Coding Agent

---

## AGENT RULES (READ FIRST — MUST FOLLOW ALWAYS)

These rules apply to every single step of this build. No exceptions.

### Rule 1 — MCP Setup
Before writing any code, check if the following MCPs are installed and active:
- **Google UI Designer MCP** — for all UI components and Material 3 design
- **Cloudflare MCP** — for deploying and managing Cloudflare Workers
- **Firebase MCP** — for Firestore, Auth, and Firebase project setup

If any of these MCPs are missing, **install them automatically** before proceeding. Do not skip this check.

### Rule 2 — Quality Check Before Every Deliverable
Before presenting any completed feature, screen, or milestone to the user, run through this checklist:
- [ ] No compilation errors
- [ ] No runtime crashes
- [ ] No null pointer exceptions or unhandled nulls
- [ ] No logic errors (does the feature actually do what it is supposed to do?)
- [ ] No broken navigation (all buttons and links go to the right place)
- [ ] No empty/blank screens on any state (loading, error, empty data)
- [ ] No hardcoded values that should be dynamic
- [ ] API calls have proper error handling and retry logic
- [ ] All UI states covered: loading shimmer, success, error, empty
- [ ] Code follows MVVM architecture consistently
- [ ] No memory leaks (ViewModels properly scoped, coroutines cancelled)
- [ ] Dark theme applied correctly on every screen

Only deliver results that pass every single item on this checklist.

### Rule 3 — Ask Before Acting on Uncertainty
If at any point you are unsure about:
- What the user wants a feature to look like
- Which of two or more approaches to take
- Whether a design decision matches the premium feel goal
- Any technical decision that could affect the whole project

**STOP and ask the user first.** Do not guess and build the wrong thing.

### Rule 4 — User Has Zero Kotlin Knowledge
The user does not know Kotlin. This means:
- Never ask the user to write or edit any code manually
- Never give instructions that require the user to understand Kotlin syntax
- Handle 100% of all code, configuration, dependencies, and setup yourself
- The only things the user should do are: answer questions, test the app on their device, and give feedback
- When explaining things to the user, use plain simple English — no technical jargon

### Rule 5 — Mobile First, Premium Always
Every screen must look and feel like a premium streaming service (Netflix / Crunchyroll level). Never ship a screen that looks unfinished, plain, or like a default template. Always use:
- Smooth animations and transitions
- Proper loading states (shimmer skeletons, not spinners where possible)
- Consistent dark theme throughout
- High quality typography and spacing
- Proper image loading with placeholders

---

## PROJECT OVERVIEW

**App Name:** AniStream  
**Platform:** Android (native)  
**Language:** Kotlin + Jetpack Compose  
**Architecture:** MVVM + Clean Architecture  
**Theme:** Dark only (deep black background like Netflix)  
**Target Device:** Samsung Galaxy A06 (and all modern Android phones)  
**Minimum SDK:** Android 8.0 (API 26)  
**Target SDK:** Android 15 (API 35)

---

## COMPLETE TECH STACK

| Layer | Technology | Purpose |
|---|---|---|
| UI | Jetpack Compose + Material 3 | All screens and components |
| UI Design | Google UI Designer MCP | Premium component generation |
| Language | Kotlin | All app code |
| Architecture | MVVM + Clean Architecture | Code organization |
| Async | Kotlin Coroutines + Flow | Background operations |
| Navigation | Jetpack Navigation Compose | Screen routing |
| Networking | Retrofit + OkHttp | API calls |
| Image Loading | Coil | Poster and banner images |
| Video Player | ExoPlayer (Media3) | Native video playback |
| Local Cache | Room Database | Offline data caching |
| Auth | Firebase Authentication | User login/signup |
| Database | Cloud Firestore | User data storage |
| Analytics | Firebase Analytics | Usage tracking |
| Crash Reporting | Firebase Crashlytics | Error monitoring |
| API Proxy | Cloudflare Workers | Proxy for hianime-api |
| Dependency Injection | Hilt | Clean dependency management |
| Animations | Lottie + Compose Animations | Premium transitions |
| Shimmer | Compose Shimmer | Loading skeletons |

---

## STEP 1 — MCP SETUP AND VERIFICATION

### 1.1 Check and Install MCPs

Run this check at the very start before any code:

```
CHECK: Is Google UI Designer MCP installed and active?
  → YES: Proceed
  → NO: Install it automatically, verify it is working, then proceed

CHECK: Is Cloudflare MCP installed and active?
  → YES: Proceed
  → NO: Install it automatically, verify it is working, then proceed

CHECK: Is Firebase MCP installed and active?
  → YES: Proceed
  → NO: Install it automatically, verify it is working, then proceed
```

### 1.2 Verify MCP Functionality
After installing each MCP, run a test call to confirm it responds correctly before moving on.

---

## STEP 2 — PROJECT SETUP

### 2.1 Create Android Project
- Package name: `com.anistream.app`
- Language: Kotlin
- Min SDK: API 26
- Build system: Gradle (Kotlin DSL)
- Enable Jetpack Compose from the start

### 2.2 Add All Dependencies to build.gradle.kts

```kotlin
// UI
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.animation:animation")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Architecture
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.51")
kapt("com.google.dagger:hilt-android-compiler:2.51")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Image Loading
implementation("io.coil-kt:coil-compose:2.6.0")

// Video Player
implementation("androidx.media3:media3-exoplayer:1.3.1")
implementation("androidx.media3:media3-ui:1.3.1")
implementation("androidx.media3:media3-exoplayer-hls:1.3.1")

// Local Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
implementation("com.google.firebase:firebase-crashlytics-ktx")

// Animations
implementation("com.airbnb.android:lottie-compose:6.4.0")
implementation("com.valentinilk.shimmer:compose-shimmer:1.3.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
```

### 2.3 Configure Firebase via Firebase MCP
Use the Firebase MCP to:
1. Create a new Firebase project named `anistream-app`
2. Register the Android app with package `com.anistream.app`
3. Download and place `google-services.json` in the app directory
4. Enable Firebase Authentication (Email/Password + Google Sign-In)
5. Create Firestore database in production mode
6. Set up Firestore security rules (see Step 4)
7. Enable Firebase Analytics and Crashlytics

### 2.4 App Theme Setup
Define the global dark theme using Google UI Designer MCP:
- Background: `#0A0A0A` (deep black)
- Surface: `#141414` (slightly lighter black — card backgrounds)
- Primary: `#E50914` (Netflix red — main accent color)
- Secondary: `#F5A623` (warm orange — secondary accent)
- Text Primary: `#FFFFFF`
- Text Secondary: `#B3B3B3`
- Typography: Use `Inter` font family throughout

---

## STEP 3 — CLOUDFLARE WORKER SETUP

### 3.1 What the Worker Does
The Cloudflare Worker acts as a proxy between the Android app and the hianime-api public instance at `4animo.xyz`. This protects the app from API changes — if the base URL changes, only the Worker needs updating, not the app.

The Worker also handles:
- CORS headers
- Request rate limiting per IP
- Response caching (cache anime data for 10 minutes to reduce upstream calls)
- Error normalization (always return consistent error format)

### 3.2 Deploy Worker via Cloudflare MCP

Use Cloudflare MCP to create and deploy this Worker:

```javascript
// Worker name: anistream-proxy
// Route: anistream-api.YOUR_SUBDOMAIN.workers.dev/*

const UPSTREAM = "https://4animo.xyz";
const CACHE_TTL = 600; // 10 minutes

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    
    // Build upstream URL
    const upstreamUrl = UPSTREAM + url.pathname + url.search;
    
    // Check cache
    const cache = caches.default;
    const cacheKey = new Request(upstreamUrl);
    const cached = await cache.match(cacheKey);
    if (cached) return cached;
    
    // Fetch from upstream
    try {
      const response = await fetch(upstreamUrl, {
        headers: {
          "User-Agent": "Mozilla/5.0 (Android)",
          "Accept": "application/json",
        }
      });
      
      if (!response.ok) {
        return new Response(
          JSON.stringify({ success: false, error: "Upstream error", status: response.status }),
          { status: response.status, headers: { "Content-Type": "application/json" } }
        );
      }
      
      const newResponse = new Response(response.body, {
        status: response.status,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
          "Cache-Control": `public, max-age=${CACHE_TTL}`,
        }
      });
      
      // Cache only successful GET requests
      if (request.method === "GET" && response.status === 200) {
        ctx.waitUntil(cache.put(cacheKey, newResponse.clone()));
      }
      
      return newResponse;
    } catch (error) {
      return new Response(
        JSON.stringify({ success: false, error: "Worker error" }),
        { status: 500, headers: { "Content-Type": "application/json" } }
      );
    }
  }
};
```

### 3.3 Save Worker URL
After deployment, save the Worker URL. It will look like:
`https://anistream-proxy.YOUR_SUBDOMAIN.workers.dev`

This URL goes into the Android app as the base API URL in the Retrofit config.

### 3.4 Worker Quality Check
Before proceeding, test these endpoints through the Worker and confirm they return valid data:
- `GET /api/v2/home`
- `GET /api/v2/anime/one-piece-100`
- `GET /api/v2/episodes/steins-gate-3`
- `GET /api/v2/search?keyword=naruto`

---

## STEP 4 — FIRESTORE DATABASE SCHEMA

### Collections Structure

```
users/
  {userId}/
    profile:
      uid: string
      email: string
      displayName: string
      photoUrl: string
      createdAt: timestamp
      
    watchlist/
      {animeId}:
        animeId: string
        title: string
        poster: string
        type: string          // TV, Movie, OVA etc
        totalEpisodes: number
        addedAt: timestamp
        
    history/
      {animeId}:
        animeId: string
        title: string
        poster: string
        lastEpisodeId: string
        lastEpisodeNumber: number
        watchedAt: timestamp
        progressSeconds: number   // how far into the episode
        
    ratings/
      {animeId}:
        animeId: string
        rating: number            // 1-5 stars
        ratedAt: timestamp
        
    settings:
      defaultLanguage: string     // "sub" or "dub"
      autoPlay: boolean
      skipIntro: boolean
      skipOutro: boolean
      dataWarning: boolean        // warn on mobile data
```

### Firestore Security Rules
Deploy these rules via Firebase MCP:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null 
                         && request.auth.uid == userId;
    }
  }
}
```

---

## STEP 5 — APP ARCHITECTURE

### 5.1 Package Structure
```
com.anistream.app/
├── data/
│   ├── remote/
│   │   ├── api/           → Retrofit service interfaces
│   │   ├── dto/           → API response data classes
│   │   └── mapper/        → Map DTOs to domain models
│   ├── local/
│   │   ├── database/      → Room database
│   │   ├── dao/           → Room DAOs
│   │   └── entity/        → Room entities (cache tables)
│   └── repository/        → Repository implementations
├── domain/
│   ├── model/             → Pure data classes (Anime, Episode, etc.)
│   ├── repository/        → Repository interfaces
│   └── usecase/           → One class per business operation
├── presentation/
│   ├── screens/           → One folder per screen
│   │   ├── home/
│   │   ├── search/
│   │   ├── detail/
│   │   ├── player/
│   │   ├── watchlist/
│   │   ├── history/
│   │   ├── profile/
│   │   └── auth/
│   ├── components/        → Shared reusable Compose components
│   ├── navigation/        → Navigation graph
│   └── theme/             → Colors, typography, shapes
├── di/                    → Hilt dependency injection modules
└── util/                  → Extension functions, constants
```

### 5.2 Data Flow (MVVM)
```
Screen (Compose UI)
    ↕ observes StateFlow
ViewModel
    ↕ calls
UseCase
    ↕ calls
Repository
    ↕ fetches from
Remote API (Cloudflare Worker) + Local Room Cache
    ↕ user data from
Firestore
```

### 5.3 Caching Strategy
- **Anime home page data** → Cache in Room for 10 minutes
- **Anime detail data** → Cache in Room for 30 minutes
- **Episode list** → Cache in Room for 1 hour
- **Search results** → No cache (always fresh)
- **User watchlist/history** → Firestore only (no local cache needed)
- **Images** → Coil handles image caching automatically

---

## STEP 6 — SCREENS AND FEATURES

### Screen 6.1 — Splash Screen
**What it does:** Shows app logo with animation, checks auth state, routes to Home or Login.

**Design:**
- Deep black background
- AniStream logo fades in with scale animation
- Small loading indicator at bottom
- Smooth transition to next screen

**Logic:**
- Check if user is logged in via Firebase Auth
- If logged in → go to Home
- If not logged in → go to Login

---

### Screen 6.2 — Login / Signup Screen
**What it does:** Lets users create an account or sign in.

**Design:**
- Full screen dark background with subtle anime-themed gradient overlay
- App logo at top
- Email + Password fields with proper styling
- "Sign In" button (red accent)
- "Create Account" toggle
- "Continue with Google" button
- Proper keyboard handling (fields scroll up when keyboard opens)
- Loading state on buttons while auth is in progress

**Logic:**
- Email/password validation before submission
- Firebase Auth for both sign in and sign up
- On success → navigate to Home and clear back stack
- On error → show specific error message (wrong password, email exists, etc.)
- Store user profile in Firestore on first signup

---

### Screen 6.3 — Home Screen
**What it does:** Main screen users see after login. Rich content discovery.

**API Calls:**
- `GET /api/v2/home` → all home page data

**Design (top to bottom):**
1. **Top App Bar** — AniStream logo left, search icon + profile avatar right
2. **Featured Spotlight Banner** — Full width auto-scrolling carousel, 3-5 spotlight anime, each with:
   - Full bleed poster image
   - Gradient overlay at bottom
   - Anime title (large bold)
   - Genre tags
   - "Watch Now" button + "Add to Watchlist" button
   - Dot indicators at bottom
3. **Section: Trending Now** — Horizontal scrolling row with ranking numbers (1, 2, 3...) overlaid on posters
4. **Section: Top Airing** — Horizontal scrolling cards
5. **Section: Recently Added** — Horizontal scrolling cards
6. **Section: Most Popular** — Horizontal scrolling cards
7. **Section: Top Upcoming** — Horizontal scrolling cards
8. **Browse by Genre** — Horizontal pill buttons for each genre

**Loading State:** Full screen shimmer skeleton matching the layout above

**Each Anime Card Shows:**
- Poster image
- Title (truncated to 2 lines)
- Episode count (sub/dub)
- Type badge (TV, Movie, OVA)

---

### Screen 6.4 — Search Screen
**What it does:** Search for any anime with live suggestions and advanced filters.

**API Calls:**
- `GET /api/v2/suggestion?keyword={query}` → live suggestions (debounced 400ms)
- `GET /api/v2/search?keyword={query}&page={page}` → full results
- `GET /api/v2/filter?...` → filtered results
- `GET /api/v2/filter/options` → load filter options

**Design:**
- Search bar at top (auto-focused when screen opens)
- While typing → suggestion dropdown appears below search bar
- After submitting → results grid (2 columns)
- Filter icon opens bottom sheet with filter options:
  - Type (TV, Movie, OVA, ONA, Special)
  - Status (Airing, Completed, Upcoming)
  - Genre (multi-select chips)
  - Season (Spring, Summer, Fall, Winter)
  - Sort by (Score, Recently Added, Name A-Z)
- Pagination: load more as user scrolls to bottom (infinite scroll)

**Empty State:** Friendly message with anime character illustration when no results

---

### Screen 6.5 — Anime Detail Screen
**What it does:** Full info page for a specific anime.

**API Calls:**
- `GET /api/v2/anime/{id}` → anime details
- `GET /api/v2/episodes/{id}` → episode list
- `GET /api/v2/characters/{id}` → character list

**Navigation:** Receives anime ID from any card tap anywhere in the app

**Design (scrollable):**
1. **Hero Section** — Full width banner image with parallax scroll effect, gradient overlay, back button top-left
2. **Info Section** — Poster thumbnail left, title + metadata right (type, year, rating, status, score)
3. **Action Buttons Row** — "Watch Now" (plays first/last watched episode), "Add to Watchlist" toggle, "Rate" button
4. **Synopsis** — Expandable text (collapsed by default, "Read More" button)
5. **Details Grid** — Aired dates, studio, genres, producers
6. **Episodes Section:**
   - Sub/Dub tab selector
   - Horizontal scrollable episode number grid
   - Tap episode → goes to Player screen
   - Watched episodes shown with checkmark/different color
7. **Characters Section** — Horizontal scrolling character cards
8. **More Like This** — Horizontal row of recommended anime cards
9. **Related Anime** — Horizontal row of related seasons/sequels

**Continue Watching Banner:** If user has watch history for this anime, show a prominent "Continue from Episode X" banner below the hero section

---

### Screen 6.6 — Video Player Screen
**What it does:** Full screen video player for watching episodes.

**API Call:**
- The embed URL format: `https://cdn.4animo.xyz/api/embed/hd-1/{episodeId}/sub?k=1&autoPlay=1&skipIntro=1&skipOutro=1`

**Implementation:**
- Use ExoPlayer (Media3) with a WebView fallback
- First attempt: load the embed URL in a WebView in full screen mode
- The WebView must have JavaScript enabled and handle media playback
- If ExoPlayer HLS extraction is possible via the Worker, use that for native playback

**Design:**
- Pure full screen — no status bar, no navigation bar (immersive mode)
- Auto-hide controls after 3 seconds of inactivity
- Controls overlay:
  - Top: anime title + episode number + back button + settings icon
  - Center: previous episode, rewind 10s, play/pause, forward 10s, next episode
  - Bottom: progress bar with scrubbing, current time / total time, sub/dub toggle, fullscreen toggle
- Skip Intro button appears at start of episode (timed)
- Skip Outro button appears near end of episode (timed)
- Next episode countdown overlay in last 30 seconds (like Netflix)
- Brightness control: swipe up/down on left side of screen
- Volume control: swipe up/down on right side of screen
- Seek: double tap left side = back 10s, double tap right side = forward 10s

**Progress Saving:**
- Save progress to Firestore every 30 seconds while watching
- Save progress when user exits the player
- Update history in Firestore with last episode and timestamp

**Sub/Dub Toggle:**
- Changes the episode ID in the embed URL between sub and dub
- Remembers user preference from settings

---

### Screen 6.7 — Schedule Screen
**What it does:** Shows upcoming episode release schedule for the week.

**API Call:**
- `GET /api/v2/schedules` → 7-day schedule

**Design:**
- Horizontal day selector at top (Today, Tomorrow, then day names)
- List of anime airing that day, each showing:
  - Anime poster thumbnail
  - Title
  - Episode number airing
  - Air time
- Countdown timer for next airing episode

---

### Screen 6.8 — Browse Screen
**What it does:** Browse anime by category, genre, or producer.

**API Calls:**
- Various category endpoints: `/api/v2/animes/top-airing`, `/api/v2/animes/movie`, etc.
- `/api/v2/animes/genre/{genre}`
- `/api/v2/genres`

**Design:**
- Top tabs: All, Movies, TV, OVA, ONA, Dubbed
- Genre filter chips below tabs (horizontal scroll)
- Infinite scroll grid (2 columns) of anime cards
- Sort options in top right

---

### Screen 6.9 — Watchlist Screen
**What it does:** Shows all anime the user has saved to their watchlist.

**Data Source:** Firestore `users/{uid}/watchlist`

**Design:**
- Header with count ("X Anime Saved")
- Toggle between Grid view and List view
- Each item shows poster, title, episode progress if any
- Swipe left to remove from watchlist
- Tap to go to Detail screen
- Empty state: friendly message + "Browse Anime" button

---

### Screen 6.10 — History Screen
**What it does:** Shows the user's watch history with resume capability.

**Data Source:** Firestore `users/{uid}/history`

**Design:**
- List sorted by most recently watched
- Each item shows:
  - Poster + title
  - "Last watched: Episode X"
  - Progress bar showing how far through the episode
  - "Continue" button
- Swipe left to remove from history
- Clear All button in top right (with confirmation dialog)

---

### Screen 6.11 — Profile Screen
**What it does:** User account settings and preferences.

**Design:**
- Profile avatar (from Google account or initials)
- Display name + email
- Stats row: X Watching, X Completed, X Total Episodes
- Settings sections:
  - **Playback:** Default language (Sub/Dub), Auto-play next episode, Skip intro, Skip outro
  - **Data:** Warn when using mobile data
  - **Account:** Change display name, Sign out
- Sign out shows confirmation dialog

---

### Screen 6.12 — Bottom Navigation Bar
Persistent across all main screens:
- **Home** (house icon)
- **Search** (magnifying glass icon)
- **Browse** (grid icon)
- **Schedule** (calendar icon)
- **Profile** (person icon)

Watchlist and History are accessible from the Profile screen or from the Home screen header.

---

## STEP 7 — SHARED UI COMPONENTS

Build these as reusable Compose components via Google UI Designer MCP:

### AnimeCard
- Poster image with Coil loading + shimmer placeholder
- Title text (2 lines max)
- Episode count badge
- Type badge (TV/Movie/OVA)
- Rating badge (optional)
- Ripple effect on tap

### SectionRow
- Section title with "See All" button
- Horizontal LazyRow of AnimeCards
- Proper padding and spacing

### ShimmerCard
- Same dimensions as AnimeCard
- Animated shimmer effect for loading state

### EpisodeGrid
- Grid of episode number buttons
- Watched state styling
- Current episode highlight

### RatingBar
- 5-star interactive rating component
- Animated fill on selection

### GenreChip
- Pill-shaped chip with genre name
- Selected/unselected states

### ErrorState
- Error illustration
- Error message text
- Retry button

### EmptyState
- Empty state illustration
- Message text
- Optional action button

---

## STEP 8 — API SERVICE LAYER

### Retrofit API Interface

```kotlin
interface HiAnimeApiService {

    // Home
    @GET("api/v2/home")
    suspend fun getHomePage(): HomeResponse

    // Anime Detail
    @GET("api/v2/anime/{id}")
    suspend fun getAnimeDetail(@Path("id") id: String): AnimeDetailResponse

    // Episodes
    @GET("api/v2/episodes/{id}")
    suspend fun getEpisodes(@Path("id") id: String): EpisodesResponse

    // Search
    @GET("api/v2/search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1
    ): SearchResponse

    // Suggestions
    @GET("api/v2/suggestion")
    suspend fun getSuggestions(@Query("keyword") keyword: String): SuggestionsResponse

    // Filter
    @GET("api/v2/filter")
    suspend fun filterAnime(
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("genres") genres: String? = null,
        @Query("season") season: String? = null,
        @Query("language") language: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1
    ): FilterResponse

    // Categories
    @GET("api/v2/animes/{category}")
    suspend fun getAnimeByCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 1
    ): AnimeListResponse

    // Genre
    @GET("api/v2/animes/genre/{genre}")
    suspend fun getAnimeByGenre(
        @Path("genre") genre: String,
        @Query("page") page: Int = 1
    ): AnimeListResponse

    // Schedule
    @GET("api/v2/schedules")
    suspend fun getSchedules(@Query("date") date: String? = null): ScheduleResponse

    // Characters
    @GET("api/v2/characters/{id}")
    suspend fun getCharacters(
        @Path("id") id: String,
        @Query("page") page: Int = 1
    ): CharactersResponse

    // Random
    @GET("api/v2/random")
    suspend fun getRandom(): RandomResponse
}
```

### Error Handling
Every repository function must wrap calls in try-catch and return a sealed class:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

---

## STEP 9 — OFFLINE SUPPORT

### What Works Offline
- Browse previously loaded home page (from Room cache)
- View anime details that were previously opened
- View watchlist (from Firestore offline persistence)
- View watch history

### What Requires Internet
- Fresh search
- Loading new episodes
- Playing video
- Syncing new watchlist/history changes

### Implementation
- Enable Firestore offline persistence (built into Firebase SDK)
- Room cache for API responses with timestamps
- Show "You're offline" banner when no internet detected
- Retry button on error states

---

## STEP 10 — DEEP LINKING

Set up Android deep links so anime can be shared:
- `anistream://anime/{id}` → opens Anime Detail screen
- `anistream://episode/{id}` → opens Player screen directly
- Share button on Detail screen generates a share sheet with deep link

---

## STEP 11 — PERFORMANCE REQUIREMENTS

- App cold start: under 3 seconds on Galaxy A06
- Home screen fully loaded: under 5 seconds on WiFi
- Image loading: placeholder shown instantly, image loaded within 2 seconds
- Navigation transitions: smooth 60fps at all times
- No janky scroll in any list or grid
- Video player ready to play: under 5 seconds on WiFi

### Optimizations to implement:
- LazyColumn and LazyRow for all lists (never use Column with forEach for large lists)
- Coil image caching with disk cache enabled
- Pagination for all list screens (never load everything at once)
- Coroutine scoping — cancel jobs when ViewModel is cleared
- Minimize recompositions — use `remember` and `key` properly in Compose

---

## STEP 12 — BUILD MILESTONES

Complete these in order. Do not start the next milestone until the current one passes the quality checklist.

### Milestone 1 — Foundation
- [ ] MCPs installed and verified
- [ ] Android project created with all dependencies
- [ ] Firebase project configured
- [ ] Cloudflare Worker deployed and tested
- [ ] App theme and typography defined
- [ ] Hilt dependency injection set up
- [ ] Retrofit configured pointing to Cloudflare Worker URL
- [ ] Base navigation graph created
- [ ] Room database set up with cache tables

**Quality Check:** App launches without crash. Navigation graph compiles. API proxy returns data.

### Milestone 2 — Authentication
- [ ] Splash screen with animation
- [ ] Login screen (email/password)
- [ ] Signup screen
- [ ] Google Sign-In
- [ ] Firebase Auth integration
- [ ] User profile creation in Firestore on signup
- [ ] Auth state persistence (stay logged in between app launches)

**Quality Check:** Full auth flow works. User stays logged in after app restart. Errors show correctly.

### Milestone 3 — Home Screen
- [ ] Home API integrated
- [ ] Spotlight carousel auto-scrolling
- [ ] All section rows rendering
- [ ] Shimmer loading state
- [ ] Error state with retry
- [ ] Anime card navigation to Detail screen

**Quality Check:** Home loads correctly. Carousel scrolls smoothly. All sections show data. Loading and error states work.

### Milestone 4 — Anime Detail Screen
- [ ] Detail API integrated
- [ ] Episodes API integrated
- [ ] Full detail layout implemented
- [ ] Episode grid with sub/dub tabs
- [ ] Characters section
- [ ] Recommendations section
- [ ] Add/Remove watchlist (Firestore)
- [ ] Rating feature (Firestore)
- [ ] Continue watching banner from history

**Quality Check:** All detail data shows correctly. Watchlist toggle works and persists. Episodes load correctly.

### Milestone 5 — Video Player
- [ ] Player screen with full screen immersive mode
- [ ] WebView loading embed URL
- [ ] Custom controls overlay
- [ ] Auto-hide controls
- [ ] Progress saving to Firestore every 30s
- [ ] Sub/dub toggle
- [ ] Next episode auto-play countdown
- [ ] Gesture controls (brightness, volume, seek)

**Quality Check:** Video plays correctly. Controls work. Progress saves and resumes. Back navigation works properly.

### Milestone 6 — Search and Browse
- [ ] Search screen with suggestions
- [ ] Filter bottom sheet
- [ ] Infinite scroll results
- [ ] Browse screen with categories
- [ ] Genre browsing

**Quality Check:** Search returns correct results. Filters work. Infinite scroll loads more pages.

### Milestone 7 — Schedule Screen
- [ ] Schedule API integrated
- [ ] Day selector tabs
- [ ] Schedule list per day
- [ ] Countdown timer

**Quality Check:** Schedule shows correct data. Day navigation works.

### Milestone 8 — User Screens
- [ ] Watchlist screen from Firestore
- [ ] History screen from Firestore
- [ ] Profile screen
- [ ] Settings (sub/dub preference, autoplay, etc.)
- [ ] Sign out
- [ ] Swipe to remove from watchlist/history

**Quality Check:** All user data loads correctly from Firestore. Settings persist. Sign out works cleanly.

### Milestone 9 — Polish and Final Checks
- [ ] All animations and transitions smooth
- [ ] Deep linking works
- [ ] Offline mode tested
- [ ] App tested on Galaxy A06 specifically
- [ ] Cold start time under 3 seconds
- [ ] No memory leaks (use LeakCanary to verify)
- [ ] Firebase Crashlytics integrated and reporting
- [ ] All error states tested by simulating no internet
- [ ] All loading states tested by simulating slow network
- [ ] App icon and splash screen finalized
- [ ] Final full run-through of every screen and feature

**Quality Check:** Full app works end to end with no issues. Ready for personal use.

---

## THINGS TO ASK THE USER BEFORE BUILDING

Before starting Milestone 3 (Home Screen):
1. What should the app be called? (default: AniStream)
2. Do you want a custom app icon or use a default one?
3. Preferred accent color — red like Netflix or a different color?

Before starting Milestone 5 (Video Player):
1. Should episodes default to Sub or Dub?
2. Should auto-play next episode be on or off by default?

---

## ADDITIONAL NOTES FOR THE AGENT

- This app is for personal use — no need to handle millions of users
- The hianime-api is an unofficial scraper — it may occasionally return errors. Always handle this gracefully with retry logic
- The video embed from cdn.4animo.xyz may have ads — this is expected behavior from the upstream source
- Always test on the actual Galaxy A06 device via ADB, not just the emulator, since the A06 has a lower-end processor
- Keep APK size reasonable — use R8 code shrinking in the release build
- The user cannot help debug Kotlin code — if something does not work, fix it yourself without asking the user to edit code

---

*Plan version 1.0 — Ready for Antigravity build*
