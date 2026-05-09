# PhishGuard AI - Mobile Front-end

A modern Android application built with **Kotlin** and **Jetpack Compose** for phishing detection. This app serves as the mobile interface for the PhishGuard AI system, communicating with a Python FastAPI backend to analyse URLs for potential phishing threats.

## 🚀 Key Features

*   **Real-time URL Scanning**: Paste or share a URL to immediately check its safety status.
*   **Deep Link / Share Intent Integration**: Scan URLs directly from mobile browsers (like Chrome) via the Android Share Sheet.
*   **Local Scan History**: Automatically saves all scan results locally using a **Room Database** for offline viewing.
*   **Advanced UI Feedback**: 
    *   **Material 3 ElevatedCards** for clear result grouping.
    *   **Haptic Feedback (Vibration)** to alert users of phishing threats.
    *   **Smooth Animations** for success states.
*   **Dynamic Results**: Displays prediction (Safe/Phishing), confidence percentage, and detailed analyst notes.

## 🏗️ System Architecture

The application follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** pattern:

*   **View (Jetpack Compose)**: Declarative UI components that observe state from the ViewModel.
*   **ViewModel**: Manages UI state and handles business logic using Kotlin Coroutines for asynchronous operations.
*   **Repository Pattern**: Acts as the single source of truth, coordinating data flow between the **Retrofit API** and the **Room Database**.
*   **Data Layer**:
    *   **Retrofit**: Handles HTTP communication with the FastAPI backend.
    *   **Room**: Manages local SQLite persistence for scan history.

## 🛠️ Technical Stack

*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **Networking**: Retrofit 2 & OkHttp
*   **Database**: Room Persistence Library
*   **Dependency Injection**: Factory Pattern
*   **Async Processing**: Kotlin Coroutines & Flow

## 📋 Requirements

*   **Android Device/Emulator**: API Level 26 (Android 8.0) or higher.
*   **Backend**: Requires the [PhishGuard Backend](https://github.com/LizTip/PhishGuard_Backend) (FastAPI) to be running.
    *   *Note: For physical devices, ensure the backend is started with `--host 0.0.0.0` and update `RetrofitClient.kt` with your local IP.*

## 🧑‍💻 Academic Context
This project was developed for a Mobile Development module, demonstrating proficiency in:
*   Inter-process communication (Intents).
*   Local data persistence and lifecycle management.
*   Modern declarative UI design.
*   Network communication and JSON parsing.
