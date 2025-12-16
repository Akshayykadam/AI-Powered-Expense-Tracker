# 💰 AI-Powered Expense Tracker

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device-success?style=for-the-badge&logo=google-safety&logoColor=white)
![AI](https://img.shields.io/badge/AI-Gemma%202B-FF6F00?style=for-the-badge&logo=google-gemini&logoColor=white)

A **intelligent, privacy-first** personal finance app for Android that automatically tracks expenses from your SMS messages using **Local AI (Google Gemma 2B)** and **Smart Regex Rules**. 

> **🔒 Zero Data Leakage:** All processing happens 100% offline on your device. Your financial data never leaves your phone.

---
## UI
<img src="https://github.com/user-attachments/assets/57408ef5-dc9d-4863-81a1-ab8348baa784" width="160" />
<img src="https://github.com/user-attachments/assets/5cbbe6d9-6b3e-4a82-8824-12b70696f011" width="160" />
<img src="https://github.com/user-attachments/assets/7239a998-32dd-4cd1-8b35-270b948895eb" width="160" />
<img src="https://github.com/user-attachments/assets/203bfdde-dcee-4d20-9155-c822b575769b" width="160" />
<img src="https://github.com/user-attachments/assets/c0a01eae-a8f9-4377-b189-811f970bfa21" width="160" />

## 🚀 Key Features

### 🧠 Hybrid Intelligence
*   **📏 Instant Rule-Based Mode**: Zero setup. Uses India-specific regex patterns to instantly track 99% of bank SMS with 100% accuracy.
*   **🤖 Local AI Mode (Gemma 2B)**: An advanced, on-device LLM that "reads" complex or unstructured messages to understand context. (Experimental)

### 📊 Premium Visualization
*   **Interactive Charts**: Beautiful bar charts with weekly, monthly, and yearly breakdowns.
*   **Trend Analysis**: Smooth cubic-bezier trend lines to visualize your spending trajectory over the year.
*   **Calendar View**: Heat-map style calendar to visualize daily spending density.

### ⚙️ Full Control
*   **AI Model Center**: Centralized hub in Settings to manage the 550MB AI model download.
*   **Dual Mode Toggle**: Switch between "Strict Rules" and "AI Hybrid" mode instantly.
*   **Debug Console**: Transparently see how the app decides to ✅ Approve or ❌ Reject every single message.

---

## 📱 How It Works

1.  **Auto-Scan**: On first launch, the app instantly scans your inbox using **Rule-Based Mode**.
2.  **No Internet Needed**: Your data appears immediately. No sign-up, no servers.
3.  **Optional AI Upgrade**: 
    *   Go to **Settings > AI Model Center**.
    *   Download the **Gemma 2B** model (~550MB) over Wi-Fi.
    *   Enable **Hybrid Mode** for enhanced categorization power.

---

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Architecture**: Clean Architecture + MVVM
*   **Dependency Injection**: Hilt
*   **Local Database**: Room (SQLite)
*   **On-Device AI**: 
    *   **Google AI Edge SDK** (MediaPipe GenAI)
    *   **Model**: Gemma 2B (INT4 Quantized)

---

## 📂 Project Structure

```
com.expense.tracker
├── data/              # Repository impl, Room DB, Local AI Service
│   ├── local/ai       # MediaPipe Integration & Model Management
│   └── local/sms      # SMS Reader & Regex Parsers
├── domain/            # Use Cases, Repository Interfaces, Models
├── ui/                # Jetpack Compose Screens & ViewModels
│   ├── components/    # Reusable UI (Charts, Cards, Dialogs)
│   ├── screens/       # Home, Calendar, Timeline, Settings
│   └── theme/         # Material 3 Theme & Typography
└── di/                # Hilt Modules
```

---

## ⚡ Setup & Installation

1.  **Prerequisites**: 
    *   Android Studio Ladybug (or newer)
    *   Android Device (Min SDK 26) with ~1GB free space for AI model.
2.  **Clone**: `git clone https://github.com/your-username/ai-expense-tracker.git`
3.  **Build**: Sync Gradle and run `assembleRelease`.
4.  **Run**: Deploy to device. Grant SMS permissions to start tracking.

---

## 📄 License

This project is for educational purposes, demonstrating the power of **On-Device Generative AI** in Android applications.
