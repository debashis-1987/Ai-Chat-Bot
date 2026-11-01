# AI Chat

An Android chat app built with Kotlin and Jetpack Compose. It talks to any OpenAI-compatible chat completions API and streams replies token by token.

## Design

The interface follows a "reading room" direction: quiet, editorial, and built for long answers.

- Authored teal-tinted neutrals instead of dynamic color; light and dark themes are tuned separately
- Serif reserved for the empty-state headline and wordmark; the rest of the product uses one tuned sans scale
- Assistant answers are prose on the canvas, not bubbles; user turns are compact tinted capsules
- A raised composer dock, a document-style header with a hairline rule, and a library-style conversation drawer
- Short functional motion: message insertion animation, pulsing typing dots, animated focus and press states, screen cross-fade

## Features

- Streaming responses over Server-Sent Events
- Multiple conversations: drawer with search, pin, rename (long-press or row menu), delete with undo
- Conversations and messages persisted in Room (schema exported, migration included)
- Markdown rendering: headings, bold, italics, inline code, lists, quotes, tables, links, and fenced code blocks with copy
- Regenerate the last reply; long-press a sent message to edit and resend it
- Inline error recovery with a Try again action, and human-readable network errors
- Thinking mode: on/off with Low, Medium, or High reasoning effort (Medium by default), sent as `reasoning_effort`
- Reasoning traces: streamed `reasoning_content` / `reasoning` shown in a collapsible "Thought for …" block and saved with the message
- Context budget: estimates tokens and drops the oldest turns from each request
- Provider presets (OpenAI, OpenRouter, Groq, Together, Ollama) and a model picker loaded from `/models`
- Per-conversation drafts, restored when you switch chats
- Share a conversation as Markdown from the header
- API key encrypted with the Android Keystore (AES/GCM)
- Scroll-to-bottom affordance when you have scrolled away from the latest turn

## Requirements

- Android Studio (or a JDK 17+ and the Android SDK)
- Android SDK platform 37, build-tools 36.0.0
- An API key for an OpenAI-compatible provider

## Build, test and install

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Testing

**Unit tests** (`app/src/test`) run on the JVM: markdown parsing, context trimming, the chat request body, and stream delta parsing.

**Instrumented tests** (`app/src/androidTest`) need a booted emulator or device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

They cover:

- `ChatFlowTest` — launches the real activity and drives the UI end to end: send with a fake `ChatService`, streamed reply, conversation actions sheet, delete with undo, model picker, and saving settings.
- `ConversationActionsTest` — rename (including persistence across a relaunch), pin and unpin, and per-conversation drafts.
- `ChatErrorRetryTest` — a failed reply shows the provider error and recovers through **Try again**.
- `ThinkingSettingsTest` — turns on thinking, picks High, saves, and confirms the setting is persisted and reaches the next request.
- `ReasoningDisplayTest` — a streamed reasoning trace renders collapsed, expands on tap, and survives switching conversations and reloading from the database.
- `ChatRepositoryTest` — real Room database (in-memory): message ordering, delete and restore, rename and pin.
- `ChatMigrationTest` — exercises both the v1 to v2 and v2 to v3 migrations, including a Room-validated v2 to v3 upgrade, and verifies existing rows survive.

The UI test swaps the network implementation through `ServiceLocator`, so no live provider is required.

CI: `.github/workflows/android.yml` runs unit tests, lint and the debug build on every push. `.github/workflows/android-instrumented.yml` runs the emulator suite on pushes to `main` or `master`, and on demand.

## Release builds

Release builds enable R8 and resource shrinking and are signed with the keystore described by
`keystore.properties` at the repository root (git-ignored):

```properties
storeFile=keystore/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Create the keystore with:

```powershell
keytool -genkeypair -keystore keystore\release.jks -alias aichat -keyalg RSA -keysize 2048 -validity 10000
```

Without `keystore.properties`, `assembleRelease` still produces an unsigned APK.

## Configuration

Open the drawer, choose Settings, then:

| Field | Example |
| --- | --- |
| Provider | tap a preset chip, or enter a custom base URL |
| API base URL | `https://api.openai.com/v1` |
| API key | your provider key |
| Model | tap **Load models** to fetch `/models`, then **Choose** |
| System prompt | `You are a helpful, concise assistant.` |
| Context budget | `12000` (tokens; `0` sends the full history) |

### Local models

For Ollama running on the development machine, use the Ollama preset (`http://10.0.2.2:11434/v1` from the emulator). Cleartext HTTP is permitted only for `10.0.2.2`, `localhost` and `127.0.0.1` (see `app/src/main/res/xml/network_security_config.xml`). Use HTTPS for anything else.

## Project layout

```
app/src/main/java/dev/androdev/aichat/
  MainActivity.kt             activity + edge-to-edge setup
  ChatViewModel.kt            UI state, streaming orchestration, conversations, models
  data/ChatClient.kt          streaming HTTP client and /models lookup
  data/ChatService.kt         network interface used by the view model
  data/ServiceLocator.kt      overridable service instance for tests
  data/ContextWindow.kt       token estimation and history trimming
  data/ChatEntities.kt        Room entities for conversations and messages
  data/ChatDao.kt             Room queries
  data/ChatDatabase.kt        database singleton
  data/ChatMigrations.kt      v1 -> v2 migration
  data/ChatRepository.kt      mapping and persistence operations
  data/AiSettings.kt          provider configuration
  data/SettingsStore.kt       settings persistence
  data/SecretStore.kt         Keystore-backed API key encryption
  data/DraftStore.kt          per-conversation composer drafts
  ui/theme/Theme.kt           authored color schemes, type scale, shapes
  ui/ChatApp.kt               drawer navigation, screen transitions, sharing
  ui/ChatScreen.kt            conversation surface, snackbars, edit dialog
  ui/ConversationDrawer.kt    library-style list with search, sections, actions sheet
  ui/SettingsScreen.kt        grouped provider/model/behavior configuration
  ui/components/ChatHeader.kt document-style header
  ui/components/MessageTurn.kt prose answers, user capsules, typing indicator
  ui/components/Composer.kt   raised composer dock
  ui/components/EmptyChatState.kt empty state with suggestions
  ui/markdown/MarkdownParser.kt  pure markdown parser
  ui/markdown/Markdown.kt        Compose renderer
  ui/util/TimeFormat.kt       relative timestamps
app/src/test/java/...         JVM unit tests for the parser and context window
app/src/androidTest/java/...  instrumented UI, repository and migration tests
app/schemas/                  exported Room schemas
.github/workflows/android.yml CI: tests, lint, debug APK
.github/workflows/android-instrumented.yml  on-demand emulator test run
```

## Build notes

AGP 9 compiles Kotlin itself (built-in Kotlin), so the project does not apply `org.jetbrains.kotlin.android`. Room code generation uses KSP 2.3.x, which is compatible with built-in Kotlin.
