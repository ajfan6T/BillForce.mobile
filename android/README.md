# Billforce Owner — Android App

A native Android companion app for **Billforce** (Billing & Business Management for Indian Shops).  
Lets the **shop owner** connect to the local Billforce database and view a live dashboard — no internet required.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🔌 **Direct DB Connect** | Reads the Billforce SQLite `.db` file directly from phone storage |
| 🔐 **4-digit PIN lock** | Secure the dashboard with a PIN (set on first connect) |
| 📊 **Today's Sales** | Revenue, invoice count, cash-in for today |
| 📅 **Month Overview** | Monthly sales, expenses, and profit |
| 💰 **Accounts** | Total receivable (customers owe you) & payable (you owe suppliers) |
| 📦 **Stock Alerts** | Low stock / out-of-stock items highlighted |
| 📈 **7-Day Sales Chart** | Bar chart of the last 7 days' revenue |
| 🧾 **Recent Invoices** | Last 10 invoices with status (Paid / Due / Partial) |
| 🔄 **Pull to Refresh** | Reload data anytime with pull-to-refresh |

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── local/         BillforceDbReader — reads SQLite directly
│   ├── model/         Data classes (DashboardSummary, InvoiceSummary, etc.)
│   └── repository/    DashboardRepository + PrefsRepository (DataStore)
├── viewmodel/         AppViewModel — state machine (Setup → PIN → Dashboard)
└── ui/
    ├── screens/       SetupScreen, PinScreen, DashboardScreen, UtilScreens
    ├── components/    SalesBarChart
    ├── navigation/    BillforceNavGraph
    └── theme/         Color, Type, Theme (teal brand colors)
```

**Tech stack:** Kotlin · Jetpack Compose · Material 3 · DataStore · Coroutines

---

## 🚀 Getting Started

### 1. Open in Android Studio

1. Open Android Studio (Hedgehog or newer)
2. **File → Open** → select `c:\Users\Lenovo\Desktop\billforce\android`
3. Wait for Gradle sync to complete
4. Run on an emulator or Android device (API 26+)

### 2. Connect the Billforce Database

The app reads your Billforce desktop app's `.db` file directly.

**Copy the database file to your Android phone:**

```
PC location (typical):
  C:\Users\<You>\AppData\Roaming\Billforce\data.db
  — or —
  C:\billforce\data.db

Steps:
1. Connect phone via USB
2. Copy data.db to phone → Downloads/ or Documents/
3. Open the Billforce Owner app
4. Tap "Browse & Select .db File"
5. Navigate to the copied file
6. Set a 4-digit PIN and tap "Connect & Open"
```

> **Tip:** You can also use a shared network folder or cloud storage (Google Drive) to sync the `.db` file automatically.

### 3. Everyday Use

- Open app → enter PIN → view dashboard
- Pull down anywhere to refresh data
- Tap **⋮ → Refresh** to manually reload
- Tap **⋮ → Disconnect** to change the database file

---

## 🗄️ Database Schema Compatibility

The `BillforceDbReader` auto-detects common column names across different Billforce versions:

| Data | Tables tried | Columns tried |
|---|---|---|
| Sales/Invoices | `invoices`, `sales`, `bills`, `vouchers` | `total`, `grand_total`, `amount` |
| Dates | — | `date`, `invoice_date`, `created_at` |
| Customers | `customers`, `ledger` | `customer_name`, `party_name` |
| Stock | `items`, `stock`, `products`, `inventory` | `quantity`, `stock`, `qty` |
| Expenses | `expenses`, `expenditure` | `amount`, `expense_amount` |
| Settings | `settings` | key = `shop_name`, `owner_name` |

---

## 📱 Screenshots Flow

```
[Splash] → [Setup: Pick DB File] → [Setup: Set PIN] → [PIN Entry] → [Dashboard]
```

---

## 🔒 Privacy & Security

- All data stays **on your device** — no cloud, no internet
- The `.db` file is copied to app cache as **read-only** (never modifies your Billforce data)
- PIN is stored locally in **DataStore** (not readable by other apps)
- No analytics, no tracking

---

## 🛠️ Build

```powershell
cd android
./gradlew assembleDebug       # Debug APK
./gradlew assembleRelease     # Release APK (needs signing)
```

APK output: `app/build/outputs/apk/`
