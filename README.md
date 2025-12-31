# 💰 AI-Powered Expense Tracker
# TxnSense 💰
### AI-Powered Expense Tracker (Android)

**TxnSense** is a modern, privacy-first Android application that automatically tracks your expenses by parsing SMS notifications on your device. Powered by Google's **Gemini Nano**, it intelligently categorizes transactions without sending your personal data to any external server. Designed with a vibrant, modern aesthetic for the digital-native generation.

> **🔒 Privacy First:** Your data is processed securely. The app uses AI to interpret complex spending patterns while keeping your financial history organized and accessible.

---
## 🚀 Key Features

### 🧠 Gemini-Powered Intelligence
*   **🤖 Smart SMS Auditor**: Uses **Gemini (gemma-3-27b-it)** to "read" verification intent. It now intelligently **ignores EMI reminders**, "Maintain Balance" alerts, and generic due-date notifications to prevent false expenses.
*   **📂 Auto-Categorization**: RAG (Retrieval-Augmented Generation) enhanced categorization that learns from your previous transaction history.

### 🎨 Aesthetic GenZ UI
*   **✨ Glassymorphism**: A stunning new "Glassy" Donut Chart for insights, featuring translucent layers and soft gradients.
*   **🟣 Payment Mode Focus**: Insights now strictly focus on **Payment Mode** breakdowns (UPI, Card, Cash) for clearer financial tracking.
*   **🔄 True Refresh**: The refresh button now performs a **Fresh Start**, clearing old data and reprocessing your inbox to ensure zero stale entries.
*   **📊 Vizualization**:
    *   **Heatmap Calendar**: Visualize your spending density using a custom purple-to-pink gradient.
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
