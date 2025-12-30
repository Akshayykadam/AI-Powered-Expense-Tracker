# 💰 AI-Powered Expense Tracker (GenZ Edition)

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=for-the-badge&logo=android&logoColor=white)
![AI](https://img.shields.io/badge/AI-Gemini%20Cloud-FF6F00?style=for-the-badge&logo=google-gemini&logoColor=white)
![Design](https://img.shields.io/badge/Design-GenZ%20Aesthetic-FF1493?style=for-the-badge&logo=visual-studio-code&logoColor=white)

A **smart, visually stunning** personal finance app for Android that automatically tracks expenses from your SMS messages using **Google Gemini AI** and **Smart JSON Parsers**. Designed with a vibrant, modern aesthetic for the digital-native generation.

> **🔒 Privacy First:** Your data is processed securely. The app uses AI to interpret complex spending patterns while keeping your financial history organized and accessible.

---
## 🚀 Key Features

### 🧠 Gemini-Powered Intelligence
*   **🤖 AI SMS Auditor**: Uses **Gemini (gemma-3-27b-it)** to "read" and verify transaction intent. It automatically filters out junk, OTPs, and even tricky EMI reminders and due-alerts.
*   **💡 Spending Insights**: Get friendly, AI-generated tips and insights based on your recent spending habits directly on the home screen.
*   **📂 Auto-Categorization**: RAG (Retrieval-Augmented Generation) enhanced categorization that learns from your previous transaction history.

### 🎨 GenZ Aesthetic UI
*   **✨ Vibrant Theme**: A custom "GenZ" design system featuring deep purples, hot pink accents, and a sleek dark-mode-only interface.
*   **📊 Premium Visualization**: 
    *   **Heatmap Calendar**: Visualize your spending density using a custom purple-to-pink gradient.
    *   **Interactive Charts**: Real-time spending breakdowns by category with smooth animations.
    *   **Timeline View**: A chronological history of all your transactions with high-contrast Material Icons.

### ⚙️ Transparent Control
*   **Debug Console**: Transparently see how the app ✅ Approves or ❌ Rejects every incoming message in real-time.
*   **Optimized Performance**: ABI Splitting and resource shrinking ensure a tiny APK size (~3.1 MB) despite the heavy features.

---

## 📱 How It Works

1.  **Grant Permissions**: Simply grant SMS read access.
2.  **Auto-Scan**: The app instantly finds financial transactions using a high-speed regex pre-processor.
3.  **AI Verification**: Complex or ambiguous messages are verified by the **Gemini Cloud API** to ensure 100% accuracy.
4.  **No Manual Entry**: Just spend, and let the app handle the tracking.

---

## 🛠️ Tech Stack

*   **Language**: Kotlin 2.0
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM + Hilt (DI)
*   **Database**: Room Persistence Library
*   **Generative AI**: 
    *   **Google AI Client SDK** (Gemini)
    *   **Model**: gemma-3-27b-it
*   **Networking**: Retrofit / OkHttp (for Gemini API)

---

## ⚡ Setup & Installation

1.  **API Key**: Obtain a Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2.  **Configuration**: Add your key to `local.properties`:
    ```properties
    GEMINI_API_KEY=your_api_key_here
    ```
3.  **Clone & Build**:
    ```bash
    git clone https://github.com/your-username/ai-expense-tracker.git
    ./gradlew assembleRelease
    ```
4.  **APK**: ABI Splitting is enabled. Find the architecture-specific APK in `app/build/outputs/apk/release/` (e.g., `app-arm64-v8a-release.apk`).

---

## 📄 License

Developed as part of an MBA Final Year Project by **Namita Kadam**. Created for educational purposes to demonstrate modern Android development and AI integration.
