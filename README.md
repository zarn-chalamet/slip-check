<div align="center">

# 🧾 SlipCheck

### Payment Slip Verification for Thai Online Sellers

Automated bank transfer slip verification via **Web Dashboard**, **Telegram Bot**, and **LINE Bot**,
with native multi-language support in **English**, **ไทย**, and **မြန်မာ**.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-Vite-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

[Overview](#-project-overview--problem-statement) •
[Architecture](#-system-architecture) •
[Providers](#-provider-integration-easyslip-v2--smart-mock-fallback) •
[ADRs](#-architecture-decision-records-adr-summary) •
[Quick Start](#-quick-start) •
[API](#-api--bot-command-reference) •
[Testing](#-testing)

</div>

---

## 📌 Project Overview & Problem Statement

Online sellers in Thailand process dozens to hundreds of bank transfer slips daily via **PromptPay** and Thai bank mobile apps (KPlus, SCB, BBL, etc.). Common pain points (**P1–P10**) include:

| Pain Point | Description |
|------------|-------------|
| 🎭 **Fake Slips** | Photographed or photoshopped slips that look authentic. |
| ♻️ **Reused Slips** | Customers using the same real slip to claim multiple orders across different channels. |
| 🏦 **Wrong Account Transfers** | Real slips paid to a different seller or personal account. |
| 🌏 **Multi-language Staff** | Burmese staff struggling to parse Thai bank app receipts or communicate status. |

**SlipCheck** addresses these by providing instant, channel-agnostic verification backed by real-time interbank verification, database-enforced duplicate protection, and strict PDPA privacy standards.

---

## 🏗️ System Architecture

```text
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│   Telegram Bot  │      │    LINE Bot     │      │   Web Dashboard │
└────────┬────────┘      └────────┬────────┘      └────────┬────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌───────────────────────────────────────────────────────────────────┐
│                         Spring Boot Core                          │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Channel Adapters (Telegram / LINE / Web REST Controllers)     │ │
│ ├───────────────────────────────────────────────────────────────┤ │
│ │ Core Verification Pipeline (Channel-Agnostic)                 │ │
│ │ 1. Image Check -> 2. Caption Parse -> 3. QR Decoder           │ │
│ │ 4. Provider Call -> 5. Receiver Match -> 6. Atomic DB Claim   │ │
│ ├───────────────────────────────────────────────────────────────┤ │
│ │ Security & Storage Layer                                      │ │
│ │ - AES-256-GCM Encrypted Accounts  - Atomic Unique Locks       │ │
│ │ - Zero Image Storage              - Append-Only Audit Log     │ │
│ └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────┬─────────────────────────────────┘
                                  │
                  ┌───────────────┼───────────────┐
                  ▼               ▼               ▼
           ┌────────────┐  ┌────────────┐  ┌────────────┐
           │ PostgreSQL │  │  EasySlip  │  │  Mock      │
           │  (Flyway)  │  │   v2 API   │  │  Fallback  │
           └────────────┘  └────────────┘  └────────────┘
```

---

## 🔌 Provider Integration: EasySlip v2 & Smart Mock Fallback

SlipCheck supports both a live production banking provider and a local developer mock.

### 1. Production Provider: EasySlip API v2

We integrated the EasySlip v2 API (`https://api.easyslip.com/v2/verify/bank`) as the primary real-world verification service because it provides:

- **18+ Thai Bank Support:** Verification across all major Thai banks (KBANK, SCB, BBL, KTB, BAY, TTB, GSB, etc.).
- **Standardized JSON Envelope:** Clean `success`, `data`, and `error` structures.
- **Account & Amount Matching:** Native capabilities to validate receiver account numbers and expected transfer amounts.
- **Multi-Branch Support:** Per-branch API key isolation and quota tracking.

### 2. Development Provider: Smart MockProvider

To ensure zero-cost testing and automated CI execution without consuming paid API credits, the system includes `MockProvider` (`provider.name=mock`):

- **Deterministic Magic Payloads:** Triggers specific responses via prefixes (e.g., `MOCK_OK_200_1234`, `MOCK_NOT_FOUND`, `MOCK_PENDING`, `MOCK_QUOTA`, `MOCK_DOWN`).
- **Free Local QR Scanner Fallback:** Automatically detects real Thai EMVCo QR payloads (starting with `000201`). It verifies the QR structure locally without internet or API keys, enabling end-to-end UI testing for free.

---

## 🏛️ Architecture Decision Records (ADR Summary)

| ADR | Decision | Summary |
|-----|----------|---------|
| **ADR-1** | Persistence-Backed Retries over In-Memory Circuit Breakers | Transient provider failures (`PENDING`, `UNAVAILABLE`) set `status=RETRY_SCHEDULED`. `RetryJob` re-executes retries on a configurable schedule (`[1, 3, 10]` minutes). This guarantees retries survive application restarts. |
| **ADR-2** | Zero QR Payload Persistence | To strictly enforce seller privacy and PDPA compliance, QR payloads are processed in-memory and never saved to the database or log files. Pending QR payloads live in-memory only (`PendingPayloadCache`, 30-min TTL). |
| **ADR-3** | Flyway Schema Ownership (`ddl-auto: none`) | PostgreSQL Flyway migrations (`V1__init.sql`) serve as the single source of truth for the database schema. Hibernate's automatic DDL generation is disabled. |
| **ADR-4** | Database-Level Duplicate Protection | Duplicate prevention is enforced via a SQL `UNIQUE` constraint on `claimed_transactions(seller_id, sending_bank, trans_ref)`. Slips are claimed directly in an atomic transaction, eliminating race conditions under high concurrency. |
| **ADR-5** | Encrypted Receiver Account Numbers | Registered bank account numbers are encrypted at rest using AES-256-GCM (`CryptoService`). The API layer only ever exposes the last 4 digits (`accountLast4`). |
| **ADR-6** | Channel Notification & Adapter Strategy | Web sync requests return within the HTTP lifecycle. Chat webhooks (Telegram/LINE) acknowledge receipt immediately and process asynchronously. Pushes use `NotificationRouter` based on active seller channel identities. |
| **ADR-7** | Native Multi-Language i18n | Every user-facing message is driven by `messages_{en,th,my}.properties`. Burmese and Thai translations are natively authored strings, preserving localized domain terms. |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.4.x, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 17, Flyway Migration |
| **Authentication** | JWT (HMAC-SHA256 via jjwt 0.12.x), Password Hashing (BCrypt) |
| **Encryption & Security** | AES-256-GCM (`javax.crypto`), Rate Limiting (Bucket4j) |
| **QR Processing** | ZXing 3.5.x (with multi-strategy fallback: rotation, grayscale, thresholding) |
| **Frontend** | React 19, Vite, TypeScript, Tailwind CSS, i18next |
| **Integrations** | EasySlip v2 API, Telegram Long Polling / Webhook, LINE Messaging API |
| **Containerization** | Docker, Multi-stage Dockerfile (Eclipse Temurin 21 JRE) |

---

## 🚀 Quick Start

### 1. Prerequisites

- Java 21+
- Node.js 22+
- PostgreSQL 17 (or Docker)

### 2. Environment Setup

Create a `.env` file in the root directory:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/slipcheck
DB_USER=postgres
DB_PASSWORD=postgres

# Security
JWT_SECRET=your-32-byte-minimum-random-jwt-secret-key
ENCRYPTION_KEY_BASE64=your-32-byte-base64-aes-key

# Provider Configuration (easyslip | mock)
PROVIDER_NAME=easyslip
PROVIDER_API_KEY=your-easyslip-api-key-here
PROVIDER_BASE_URL=https://api.easyslip.com/v2/verify/bank

# Bot Integrations (Optional)
TELEGRAM_BOT_TOKEN=
LINE_CHANNEL_SECRET=
LINE_CHANNEL_ACCESS_TOKEN=
```

### 3. Running with Docker Compose

```bash
# Build and run all services (DB, Backend, Frontend)
docker compose up --build -d
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Health Check | http://localhost:8080/actuator/health |

### 4. Local Development Execution

**Backend:**

```bash
cd backend
mvn spring-boot:run
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

---

## 📋 API & Bot Command Reference

### REST API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | Public | Register a seller account |
| `POST` | `/api/v1/auth/login` | Public | Login and obtain JWT token |
| `GET` | `/api/v1/seller/accounts` | JWT | List registered bank accounts (masked last 4) |
| `POST` | `/api/v1/seller/accounts` | JWT | Add a new receiving bank account |
| `POST` | `/api/v1/slips` | JWT | Upload a slip image for instant verification |
| `GET` | `/api/v1/slips` | JWT | Fetch submission history (paginated & filtered) |
| `GET` | `/api/v1/slips/review` | JWT | View review queue (`AMOUNT_MISMATCH`, etc.) |
| `POST` | `/api/v1/slips/{id}/review` | JWT | Approve or Reject flagged submissions |
| `GET` | `/api/v1/summary/daily` | JWT | Get seller's daily verification stats |

### Bot Commands (Telegram & LINE)

| Command | Description |
|---------|-------------|
| `/start` | Welcome message and channel link prompt |
| `/link <code>` | Link chat account using web-generated pairing code |
| `/lang` | Toggle preference between English, ไทย, and မြန်မာ |
| `/summary` | Request instant daily summary report |
| `/help` | View help guide and caption instructions |

---

## 🧪 Testing

```bash
cd backend

# Run all unit and integration tests
mvn test

# Run high-concurrency duplicate claim test (20 threads -> 1 VERIFIED + 19 DUPLICATE)
mvn test -Dtest="ConcurrencyTest"

# Verify complete i18n bundle key synchronization across EN, TH, MY
mvn test -Dtest="I18nCompletenessTest"
```

---

## 👥 Market Research & Seller Interviews

**Target Audience Validation** (Small Online Sellers in Thailand):

- **Interview Goal:** Understand manual check bottleneck and frequency of fraud.
- **Key Findings:**
  - Sellers manually match receiver names/suffixes across banking apps (takes 30–60 secs per slip).
  - Fake/edited slips and reused historical slips account for ~90% of fraud attempts.
  - Burmese staff management requires clear visual cues and native language status labels rather than complex banking terms.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
