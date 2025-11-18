# 📱 EasyPlan - Smart Task Management for Android

<div align="center">

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
[![Android](https://img.shields.io/badge/Platform-Android%208.1%2B-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![REST API](https://img.shields.io/badge/REST%20API-JSONBin.io-purple.svg)](https://jsonbin.io/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-blue.svg)](https://github.com/features/actions)

**A modern, feature-rich task management Android application with cloud synchronization, analytics, and intelligent productivity tracking**

[Features](#-features) • [Screenshots](#-screenshots) • [Architecture](#-architecture--design) • [Setup](#-setup--installation) • [REST API](#-rest-api-integration) • [CI/CD](#-cicd-with-github-actions) • [Documentation](#-documentation)

</div>

---

## 📋 Table of Contents

- [About](#-about-easyplan)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Architecture & Design](#-architecture--design)
- [Technology Stack](#-technology-stack)
- [REST API Integration](#-rest-api-integration)
- [Setup & Installation](#-setup--installation)
- [CI/CD with GitHub Actions](#-cicd-with-github-actions)
- [Testing](#-testing)
- [GitHub Utilization](#-github-utilization)
- [Design Considerations](#-design-considerations)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 About EasyPlan

EasyPlan is a comprehensive task management application built for Android that helps users organize their daily activities, track productivity, and achieve their goals. The app combines modern Android development practices with cloud-based synchronization to provide a seamless, cross-device experience.

### 🎓 Purpose

This application was developed as part of an academic project to demonstrate:
- **Mobile Application Development** skills using Kotlin and Android SDK
- **Cloud Integration** with Firebase Authentication and Firestore
- **REST API Integration** using JSONBin.io for data backup and synchronization
- **Modern UI/UX Design** following Material Design 3 guidelines
- **Software Engineering Best Practices** including version control, CI/CD, and documentation
- **Automated Testing and Deployment** using GitHub Actions

---

## ✨ Features

### 🔐 Authentication & Security
- **Email/Password Authentication** - Secure user registration and login via Firebase Auth
- **Google Sign-In (SSO)** - One-tap sign-in with Google accounts using OAuth 2.0
- **User Profile Management** - Personalized profiles with display name and email
- **Secure Session Management** - Automatic token refresh and session handling

### 📝 Task Management
- **Create Tasks** - Add tasks with titles, descriptions, due dates, and times
- **Task Prioritization** - Assign priority levels (High, Medium, Low) with color coding
- **Task Categories** - Organize tasks into categories (Work, Personal, Study, Health, Shopping, Other)
- **Task Completion** - Mark tasks as complete with visual feedback
- **Calendar View** - Visual calendar to see tasks organized by date
- **Quick Actions** - Fast task creation and reminder setup

### 📊 Analytics & Statistics
- **Comprehensive Dashboard** - View detailed task statistics and productivity metrics
- **Completion Rate** - Track percentage of completed vs. pending tasks
- **Productivity Score** - Smart algorithm calculating productivity (0-100 scale)
- **Priority Distribution** - Visual breakdown of tasks by priority level
- **Category Distribution** - See task distribution across categories
- **Time-Based Metrics** - Track tasks completed today, this week, and this month
- **Completion Streak** - Monitor consecutive days of task completion 🔥
- **Overdue Tracking** - Identify and manage overdue tasks

### ☁️ Cloud Synchronization
- **Firebase Firestore** - Real-time cloud database for task storage
- **Automatic Sync** - Tasks automatically sync across devices
- **Offline Support** - Work offline with automatic sync when online
- **User-Specific Data** - Each user's tasks are securely isolated

### 🌐 REST API Integration
- **JSONBin.io Integration** - RESTful API for task backup and synchronization
- **Retrofit HTTP Client** - Modern, type-safe HTTP client for API calls
- **CRUD Operations** - Full Create, Read, Update, Delete functionality
- **Error Handling** - Comprehensive error handling with user feedback
- **API Key Management** - Secure API key storage in resources

### 🎨 User Interface
- **Material Design 3** - Modern, beautiful UI following Google's design guidelines
- **Theme Support** - Light, Dark, and System theme modes
- **Smooth Animations** - Polished transitions and interactions
- **Responsive Design** - Optimized for different screen sizes
- **Bottom Navigation** - Easy navigation between Tasks, Statistics, Calendar, Settings, and Profile
- **Empty States** - Helpful messages when no data is available

### 🛠️ Developer Features
- **Comprehensive Logging** - Detailed logs for debugging (Debug, Info, Warning, Error levels)
- **Code Documentation** - Every class and method documented with KDoc
- **Best Practices** - Following Android and Kotlin coding standards
- **Resource Management** - Externalized strings, colors, and configurations
- **Version Control** - Git with meaningful commit messages

---

## 📸 Screenshots

### Authentication
| Login Screen | Register Screen | Google Sign-In |
|:---:|:---:|:---:|
| ![Login](docs/screenshots/login.png) | ![Register](docs/screenshots/register.png) | ![Google SSO](docs/screenshots/google-signin.png) |

### Main Features
| Tasks View | Statistics Dashboard | Calendar View |
|:---:|:---:|:---:|
| ![Tasks](docs/screenshots/tasks.png) | ![Statistics](docs/screenshots/statistics.png) | ![Calendar](docs/screenshots/calendar.png) |

### Additional Features
| Settings | Profile | Theme Support |
|:---:|:---:|:---:|
| ![Settings](docs/screenshots/settings.png) | ![Profile](docs/screenshots/profile.png) | ![Themes](docs/screenshots/themes.png) |

---

## 🔥 Cool Features (Freshly Added)

These are the latest upgrades we shipped to make EasyPlan feel magical:

- Tasks Tab Sections: Dedicated "Today's Tasks" and "High Priority" panels with real data
- Priority + Category at Creation: Choose priority (High/Medium/Low) and category (Work/Personal/Study/Health/Shopping/Other) in the Add Task bottom sheet
- Clear Visuals: Colored priority indicator and category emoji on each task row
- Local Persistence: Tasks are cached locally and survive app restarts (guest-friendly)
- JSONBin Backup/Restore: Manual Export/Import from Settings with status (Bin ID)
- Calendar Cleanup: Removed hardcoded items; "Today" now shows only your real tasks
- Single-Activity Clarity: Kept the fast, simple, view-switched architecture and wired everything correctly

### 📸 Image placeholders (to be added)

- Tasks tab — Today's Tasks section

  ![Tasks Today Placeholder](docs/screenshots/tasks_tab_today.png)

- Tasks tab — High Priority section
  ![High Priority Placeholder](docs/screenshots/tasks_tab_high_priority.png)

- Add Task bottom sheet — Priority and Category chips
  ![Add Task Priority+Category Placeholder](docs/screenshots/add_task_priority_category.png)

- Task item — Priority indicator + Category icon
  ![Task Row Visuals Placeholder](docs/screenshots/task_row_priority_category.png)

- Settings — Cloud Backup (JSONBin) card
  ![Settings JSONBin Placeholder](docs/screenshots/settings_jsonbin_card.png)

- Export to JSONBin — success toast/status
  ![Export JSONBin Placeholder](docs/screenshots/jsonbin_export.png)

- Import from JSONBin — refreshes Tasks/Calendar/Stats
  ![Import JSONBin Placeholder](docs/screenshots/jsonbin_import.png)

- Calendar — Today's tasks (dynamic, no samples)
  ![Calendar Today Placeholder](docs/screenshots/calendar_today_dynamic.png)

---


## 🏗️ Architecture & Design

### Architecture Pattern
EasyPlan follows a **Single Activity Architecture** with view-based navigation:

```
MainActivity
├── Tasks View (Quick actions, task creation)
├── Statistics View (Analytics dashboard)
├── Calendar View (Date-based task organization)
├── Settings View (Theme, preferences)
└── Profile View (User information, sign out)
```

### Design Patterns Used

1. **Singleton Pattern**
   - `TaskManager` - Centralized task management
   - `ApiClient` - HTTP client instance
   - `ThemeUtils` - Theme management
   - `TaskStatistics` - Statistics calculation

2. **Repository Pattern**
   - `TaskRepository` - Abstraction layer for data operations
   - Separates data sources (Firestore, REST API) from business logic

3. **Observer Pattern**
   - Real-time UI updates when data changes
   - Firebase Firestore listeners for live data

### Data Flow

```
User Action → MainActivity → TaskManager → Repository → Data Sources
                                                        ├── Firebase Firestore
                                                        └── JSONBin REST API
```

---

## 💻 Technology Stack

| Category | Technology | Purpose |
|----------|------------|---------|
| **Language** | Kotlin 1.9.0 | Modern, concise Android development |
| **Build System** | Gradle 8.7 with Kotlin DSL | Dependency management and build automation |
| **Min SDK** | Android 8.1 (API 27) | Wide device compatibility |
| **Target SDK** | Android 14 (API 36) | Latest Android features |
| **UI Framework** | Material Design 3 | Modern, beautiful user interface |
| **Authentication** | Firebase Auth 22.3.1 | User authentication and management |
| **Database** | Firebase Firestore 24.10.1 | Cloud NoSQL database |
| **Analytics** | Firebase Analytics 21.5.1 | User behavior tracking |
| **Google Sign-In** | Play Services Auth 20.7.0 | OAuth 2.0 SSO implementation |
| **HTTP Client** | Retrofit 2.9.0 | Type-safe REST API client |
| **JSON Parsing** | GSON 2.10.1 | JSON serialization/deserialization |
| **Networking** | OkHttp 4.12.0 | HTTP client and interceptor |
| **REST API** | JSONBin.io | Cloud-based JSON storage and REST API |
| **CI/CD** | GitHub Actions | Automated testing and builds |
| **Version Control** | Git & GitHub | Source code management |

---

## 🌐 REST API Integration

EasyPlan integrates with **JSONBin.io** as its REST API backend for task backup and synchronization. This provides an additional layer of data persistence beyond Firebase Firestore.

### Why JSONBin.io?

- **RESTful API** - Standard HTTP methods (GET, POST, PUT, DELETE)
- **JSON Storage** - Perfect for storing task data
- **Free Tier** - Generous free tier for development and testing
- **No Server Required** - Serverless architecture
- **Easy Integration** - Simple API with good documentation
- **CORS Support** - Works seamlessly with mobile apps

### API Implementation

#### 1. **API Client Setup**

```kotlin
// ApiClient.kt - Singleton HTTP client
object ApiClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String? = null

    fun initialize(context: Context) {
        baseUrl = context.getString(R.string.jsonbin_base_url)
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl!!)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getJsonBinService(): JsonBinService {
        return retrofit!!.create(JsonBinService::class.java)
    }
}
```

#### 2. **API Service Interface**

```kotlin
// JsonBinService.kt - Retrofit service interface
interface JsonBinService {
    @GET("latest")
    suspend fun getTasks(
        @Header("X-Master-Key") apiKey: String
    ): Response<JsonBinResponse>

    @PUT
    suspend fun updateTasks(
        @Header("X-Master-Key") apiKey: String,
        @Body tasks: TasksData
    ): Response<JsonBinResponse>
}
```

#### 3. **Repository Pattern**

```kotlin
// TaskRepository.kt - Data layer abstraction
class TaskRepository(private val context: Context) {
    private val jsonBinApiKey: String = context.getString(R.string.jsonbin_api_key)
    private val jsonBinService: JsonBinService by lazy {
        ApiClient.initialize(context)
        ApiClient.getJsonBinService()
    }

    suspend fun syncTasks(tasks: List<Task>) {
        try {
            val response = jsonBinService.updateTasks(jsonBinApiKey, TasksData(tasks))
            if (response.isSuccessful) {
                Log.i(TAG, "Tasks synced successfully to REST API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
        }
    }
}
```

### API Features Implemented

✅ **Task Synchronization** - Sync tasks to cloud storage
✅ **Error Handling** - Graceful handling of network errors
✅ **Logging** - Comprehensive logging of API calls
✅ **Secure API Keys** - Keys stored in `strings.xml`, not hardcoded
✅ **Async Operations** - Non-blocking API calls using Kotlin coroutines
✅ **Response Parsing** - Automatic JSON to Kotlin object conversion

### REST API Endpoints Used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/v3/b/{BIN_ID}/latest` | Retrieve all tasks |
| `PUT` | `/v3/b/{BIN_ID}` | Update tasks |

### Configuration

API keys and endpoints are externalized in `strings.xml`:

```xml
<string name="jsonbin_api_key">$2b$10$YOUR_API_KEY_HERE</string>
<string name="jsonbin_base_url">https://api.jsonbin.io/v3/</string>
```



---

## 🚀 Setup & Installation

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK** 17 or later
- **Android SDK** with API 27+ installed
- **Git** for version control
- **Firebase Account** for authentication and database
- **JSONBin.io Account** for REST API (optional but recommended)

### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/EasyPlan.git
cd EasyPlan
```

### Step 2: Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing one
3. Add an Android app with package name: `com.easyplan`
4. Download `google-services.json`
5. Place it in `EasyPlan/app/` directory
6. Enable Authentication methods:
   - Email/Password
   - Google Sign-In
7. Create a Firestore database in test mode
8. Add your SHA-1 fingerprint

### Step 3: Google Sign-In Configuration

1. In Firebase Console, go to Authentication → Sign-in method
2. Enable Google provider
3. Copy the Web Client ID
4. Update `EasyPlan/app/src/main/res/values/strings.xml`:
   ```xml
   <string name="google_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
   ```

### Step 4: JSONBin.io Setup (Optional)

1. Sign up at [JSONBin.io](https://jsonbin.io/)
2. Create an API key
3. Create a new bin with initial structure:
   ```json
   {
     "tasks": []
   }
   ```
4. Update `strings.xml` with your API key and Bin ID



### Step 5: Build and Run

```bash
cd EasyPlan
./gradlew clean assembleDebug
```

Or open the project in Android Studio and click Run ▶️

---

## 🔄 CI/CD with GitHub Actions

EasyPlan uses **GitHub Actions** for automated testing and continuous integration. Every push and pull request triggers automated builds to ensure code quality.

### Workflow Configuration

The CI/CD pipeline is defined in `.github/workflows/android-ci.yml`:

```yaml
name: Android CI

on:
  push:
    branches: [ main, master, develop ]
  pull_request:
    branches: [ main, master, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
      - name: Set up JDK 17
      - name: Build with Gradle
      - name: Upload build artifacts

  lint:
    runs-on: ubuntu-latest
    steps:
      - name: Run lint checks
      - name: Upload lint results
```

### What Gets Automated

✅ **Code Checkout** - Automatically pulls latest code
✅ **Environment Setup** - Configures JDK 17 and Gradle
✅ **Build Verification** - Compiles the app to ensure no errors
✅ **Lint Checks** - Runs Android lint for code quality
✅ **Artifact Upload** - Saves APK for download
✅ **Build Status** - Updates README badge with build status

### Benefits of CI/CD

1. **Early Bug Detection** - Catch issues before they reach production
2. **Code Quality** - Automated lint checks enforce standards
3. **Collaboration** - Team members see build status immediately
4. **Confidence** - Know that code works on clean environment
5. **Documentation** - Build history serves as project timeline

### Viewing Build Results

1. Go to the **Actions** tab in GitHub repository
2. Click on any workflow run to see details
3. Download build artifacts (APK files)
4. View lint reports and logs

---

## 🧪 Testing

### Unit Tests

Basic unit tests are included in `app/src/test/java/com/easyplan/`:

```kotlin
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun percentage_calculation_works() {
        val total = 10
        val completed = 7
        val percentage = (completed * 100f / total)
        assertEquals(70f, percentage, 0.01f)
    }
}
```

### Running Tests

```bash
./gradlew test
```

### Test Coverage

- ✅ Basic arithmetic operations
- ✅ String manipulations
- ✅ List operations
- ✅ Percentage calculations

---

## 📚 GitHub Utilization

### Version Control Strategy

EasyPlan follows Git best practices for version control:

#### Branch Strategy
- `main` - Production-ready code
- `develop` - Development branch
- `feature/*` - Feature branches
- `bugfix/*` - Bug fix branches

#### Commit Message Convention
```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes
- `refactor:` - Code refactoring
- `test:` - Test additions/changes
- `chore:` - Build process or auxiliary tool changes

**Example:**
```
feat: Add statistics dashboard with productivity scoring

- Implemented TaskStatistics utility class
- Created statistics fragment with comprehensive metrics
- Added productivity score algorithm (0-100 scale)
- Integrated priority and category distribution charts

Closes #15
```

### GitHub Features Used

1. **Repository**
   - Source code hosting
   - Version history tracking
   - Collaboration platform

2. **Issues**
   - Bug tracking
   - Feature requests
   - Task management

3. **Pull Requests**
   - Code review process
   - Discussion platform
   - Merge management

4. **Actions**
   - CI/CD automation
   - Automated testing
   - Build verification

5. **Releases**
   - Version tagging
   - Release notes
   - APK distribution

6. **Wiki** (Optional)
   - Extended documentation
   - User guides
   - Developer notes

7. **Projects** (Optional)
   - Kanban boards
   - Sprint planning
   - Progress tracking

### Collaboration Workflow

1. **Create Issue** - Describe feature or bug
2. **Create Branch** - `feature/issue-number-description`
3. **Develop** - Write code with frequent commits
4. **Push** - Push branch to GitHub
5. **Pull Request** - Create PR with description
6. **CI Check** - GitHub Actions runs automatically
7. **Code Review** - Team reviews changes
8. **Merge** - Merge to develop/main branch
9. **Deploy** - Automated or manual deployment

---

## 🎨 Design Considerations

### User Experience (UX)

1. **Simplicity First**
   - Clean, uncluttered interface
   - Intuitive navigation with bottom bar
   - Quick actions for common tasks

2. **Visual Feedback**
   - Loading indicators for async operations
   - Success/error messages with Toast
   - Smooth animations and transitions

3. **Accessibility**
   - High contrast colors
   - Readable font sizes
   - Touch targets sized appropriately

4. **Performance**
   - Lazy loading of data
   - Efficient database queries
   - Minimal memory footprint

### User Interface (UI)

1. **Material Design 3**
   - Following Google's design guidelines
   - Consistent component usage
   - Modern, beautiful aesthetics

2. **Color Scheme**
   - Brand blue (#278FE5) as primary color
   - Priority-based color coding (Red/Orange/Green)
   - Category-specific colors

3. **Typography**
   - Clear hierarchy with font sizes
   - Sans-serif fonts for readability
   - Bold for emphasis

4. **Iconography**
   - Emoji icons for categories (💼🏠📚💪🛒📌)
   - Material icons for navigation
   - Consistent icon style

### Data Management

1. **Dual Persistence**
   - Firebase Firestore for real-time sync
   - JSONBin.io for backup and REST API demo

2. **Offline Support**
   - Firestore offline persistence
   - Graceful degradation when offline

3. **Data Security**
   - User-specific data isolation
   - Secure authentication
   - API keys externalized

### Code Quality

1. **Documentation**
   - KDoc comments on all public APIs
   - Inline comments for complex logic
   - README and guides

2. **Logging**
   - Comprehensive logging throughout
   - Different log levels (D/I/W/E)
   - Helpful error messages

3. **Error Handling**
   - Try-catch blocks for risky operations
   - User-friendly error messages
   - Graceful fallbacks

4. **Best Practices**
   - Following Kotlin coding conventions
   - Android best practices
   - Resource externalization

---

## 🚀 Future Enhancements

### Planned Features

- [ ] **Search & Filter** - Search tasks by title/description, filter by priority/category/status
- [ ] **Task Attachments** - Add photos or files to tasks
- [ ] **Subtasks** - Break down tasks into smaller steps
- [ ] **Task Sharing** - Share tasks with other users
- [ ] **Notifications** - Push notifications for reminders
- [ ] **Widgets** - Home screen widgets for quick access
- [ ] **Biometric Auth** - Fingerprint/face unlock
- [ ] **Multi-language** - Full support for Afrikaans and isiZulu
- [ ] **Export/Import** - Export tasks to CSV/JSON
- [ ] **Dark Mode Auto** - Automatic theme switching based on time
- [ ] **Task Templates** - Reusable task templates
- [ ] **Recurring Tasks** - Daily/weekly/monthly recurring tasks
- [ ] **Voice Input** - Create tasks with voice commands
- [ ] **AI Suggestions** - Smart task suggestions based on patterns

---

## 👥 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please ensure:
- Code follows Kotlin coding conventions
- All new code has appropriate comments
- Commit messages follow the convention
- CI/CD checks pass

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact & Support

- **Developer**: EasyPlan Team
- **Email**: support@easyplan.app
- **GitHub**: [github.com/yourusername/EasyPlan](https://github.com/yourusername/EasyPlan)
- **Issues**: [Report a bug or request a feature](https://github.com/yourusername/EasyPlan/issues)

---

## 🙏 Acknowledgments

- **Firebase** - For excellent backend services
- **JSONBin.io** - For simple and effective REST API
- **Material Design** - For beautiful UI components
- **Kotlin** - For modern, concise language
- **Android Community** - For extensive documentation and support
- **GitHub** - For version control and CI/CD platform

---

<div align="center">

**Built with ❤️ using Kotlin, Firebase, and Material Design 3**

⭐ Star this repository if you find it helpful!

</div> Purpose |
