# LostLink — Campus Lost & Found Platform

LostLink is an enterprise-grade campus custody, lost property reporting, and physical restitution platform designed for university communities. It features end-to-end claim management, cryptographic 6-digit handover PIN verification, real-time live chat via WebSockets, smart item matching recommendations, role-based administration, and audit reporting.

- **Backend:** Java 17+ (Spring Boot 3.3.5), Spring Data JPA, Spring Security 6, JWT Authentication, WebSocket/STOMP (SockJS)
- **Database:** Embedded H2 Database (File-backed: `./data/lostfounddb`, zero-config persistent storage) with optional MySQL profile
- **Frontend:** React 19, Vite 8, Tailwind CSS (with Dark Mode), Lucide Icons, STOMP.js
- **Testing:** 28 Backend JUnit/Integration tests + 72 Node.js End-to-End automated test scenarios (100% passing)

---

## ⚡ Quick Start

Standardized launcher and teardown scripts are provided for both Windows and macOS/Linux.

### 🪟 Windows Users:
- **Start Services:** Double-click `run.bat` (or `start.bat`).
  - Automatically installs frontend dependencies on first run if missing.
  - Defaults to file-based persistent H2 storage (zero setup needed).
  - Launches Spring Boot (`http://localhost:8081`) and Vite React (`http://localhost:5173`) concurrently in a single window with synchronized browser startup.
  - *(Optional)* To run with MySQL instead: `run.bat mysql`.
- **Stop Services:** Double-click `stop.bat` to cleanly terminate processes on ports `8081` and `5173`.

### 🍎 / 🐧 macOS & Linux Users:
- **Start Services:** Run `./run.sh` (or `bash run.sh`).
  - Automatically ensures `mvnw` execution permissions.
  - Launches backend and frontend concurrently with trap signals for clean shutdown.
- **Stop Services:** Run `./stop.sh` (or `bash stop.sh`) to cleanly stop processes on ports `8081` and `5173`.

---

## 🌐 URLs & Service Ports

| Service | URL | Description |
| :--- | :--- | :--- |
| **Frontend Web App** | `http://localhost:5173` | LostLink interactive UI (Vite + React) |
| **Backend REST API** | `http://localhost:8081/api` | Spring Boot RESTful API endpoints |
| **H2 Web Console** | `http://localhost:8081/h2-console` | JDBC URL: `jdbc:h2:file:./data/lostfounddb` (User: `sa`, Password: *(empty)*) |
| **WebSocket STOMP** | `ws://localhost:8081/ws` | Real-time chat messaging and live status notifications |

---

## 🔐 Default Demo Login Credentials

The application comes pre-configured with campus roles and test accounts:

### Campus Administrator (`ROLE_ADMIN`):
- **Email:** `admin@lostfound.edu`
- **Password:** `Admin@123`
- *Access to Admin Dashboard, metrics, global item resolution, claim audit reports (CSV export), and user management.*

### Students / Item Reporters (`ROLE_USER`):
- **Rohan Sharma:** `rohan@student.edu` / `Password@123`
- **Abinav Sabu:** `abinav@lostfound.edu` / `Password@123`
- **David Miller:** `david@student.edu` / `Password@123`
- **Sneha Patel:** `sneha@student.edu` / `Password@123`

---

## 📦 Demo Item Catalog (12 Curated Items)

The repository includes a curated, realistic demonstration catalog of **12 distinct items** paired with **12 matching distinct photos** in `lostfound/uploads/`:

| ID | Item Name | Category | Status | Assigned Image |
| :---: | :--- | :--- | :---: | :--- |
| **1** | Apple iPhone 15 Pro (Natural Titanium) | `ELECTRONICS` | `LOST` | `d1b7a240-6f81-423c-91d1-61019a6d8001.jpg` |
| **2** | Apple MacBook Air M2 (Space Gray) | `ELECTRONICS` | `FOUND` | `d1b7a240-6f81-423c-91d1-61019a6d8002.jpg` |
| **3** | Navy Blue Leather Bi-Fold Wallet | `WALLETS_CARDS` | `CLAIMED` | `d1b7a240-6f81-423c-91d1-61019a6d8003.jpg` |
| **4** | The North Face Borealis Backpack (Black) | `CLOTHING` | `LOST` | `d1b7a240-6f81-423c-91d1-61019a6d8004.jpg` |
| **5** | Set of Dorm & Bike Keys on Black Carabiner | `KEYS` | `FOUND` | `d1b7a240-6f81-423c-91d1-61019a6d8005.jpg` |
| **6** | Apple Watch Series 9 (Midnight Aluminum 45mm) | `ELECTRONICS` | `REUNITED` | `d1b7a240-6f81-423c-91d1-61019a6d8006.jpg` |
| **7** | Sony WH-1000XM5 Wireless Headphones | `ELECTRONICS` | `FOUND` | `d1b7a240-6f81-423c-91d1-61019a6d8007.jpg` |
| **8** | Stainless Steel Hydro Flask (Olive Green 32oz) | `OTHER` | `LOST` | `d1b7a240-6f81-423c-91d1-61019a6d8008.jpg` |
| **9** | Texas Instruments TI-84 Plus CE Graphing Calculator | `ELECTRONICS` | `FOUND` | `d1b7a240-6f81-423c-91d1-61019a6d8009.jpg` |
| **10** | Campus Student ID & Access Smartcard | `WALLETS_CARDS` | `CLAIMED` | `d1b7a240-6f81-423c-91d1-61019a6d8010.jpg` |
| **11** | Compact Windproof Travel Umbrella (Navy Blue) | `OTHER` | `LOST` | `d1b7a240-6f81-423c-91d1-61019a6d8011.jpg` |
| **12** | Trek FX 2 Disc City Commuter Bicycle (Matte Black) | `OTHER` | `REUNITED` | `d1b7a240-6f81-423c-91d1-61019a6d8012.jpg` |

---

## 💻 Manual Setup & Commands

### 1. Prerequisites
- **Java JDK (17 or higher)**: `java -version`
- **Node.js (18+ or 20+) & npm**: `node -v`
- *(Maven is bundled via `./mvnw` / `mvnw.cmd`—no global Maven install required).*

### 2. Manual Execution (2 Terminals)
- **Terminal 1 (Backend):**
  ```bash
  cd lostfound
  ./mvnw spring-boot:run          # macOS / Linux
  .\mvnw.cmd spring-boot:run      # Windows
  ```
- **Terminal 2 (Frontend):**
  ```bash
  cd lostfound-frontend
  npm install                     # First run only
  npm run dev
  ```

---

## 🧪 Automated Testing & Verification

The project includes thorough unit, integration, security, and full-stack regression suites:

### 1. Backend Maven Tests (28 Tests)
Runs Spring Boot context smoke tests, JPA unit tests, and security regression checks:
```bash
cd lostfound
.\mvnw.cmd test                 # Windows
./mvnw test                     # macOS / Linux
```

### 2. Frontend Production Build
Validates Vite bundler, asset minification, and JSX/CSS syntax:
```bash
cd lostfound-frontend
npm run build
```

### 3. Full End-to-End Test Suite (72 Scenarios)
Verifies user registration, JWT auth, lost/found reporting, filtering & search, claim approvals, admin/student 6-digit handover PIN generation, physical handover verification, live messaging, IDOR protections, and static asset serving:
```bash
node test-full-suite.mjs
```

---

## 📁 Repository Structure

```
lostfound/
├── .gitignore
├── README.md                      # Project documentation and guide
├── run.bat / start.bat / stop.bat # Windows unified lifecycle scripts
├── run.sh / stop.sh               # macOS / Linux unified lifecycle scripts
├── test-full-suite.mjs            # Comprehensive 72-scenario automated E2E test suite
├── lostfound/                     # Spring Boot 3.3 Backend
│   ├── data/
│   │   ├── backup.sql             # SQL snapshot backup
│   │   └── lostfounddb.mv.db      # Persistent H2 file database
│   ├── uploads/                   # Stored item image uploads
│   ├── src/main/java/             # Controllers, Services, Security, Models, DTOs
│   ├── src/main/resources/        # application.properties and static configurations
│   └── pom.xml                    # Maven dependencies
└── lostfound-frontend/            # React 19 + Vite Frontend
    ├── src/
    │   ├── components/            # Reusable UI components & modals
    │   ├── context/               # AuthContext, NotificationContext
    │   ├── pages/                 # Home, ItemDetails, CreateItem, AdminDashboard, etc.
    │   └── services/              # Axios API service layer & WebSocket STOMP client
    └── package.json
```
