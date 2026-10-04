# IELTS AI Tutor (LUMEN)

Nền tảng luyện thi toàn diện 4 kỹ năng IELTS (Listening, Reading, Writing, Speaking) tích hợp trợ lý Én, tra cứu bài tập qua RAG (pgvector), và chấm điểm/phân tích lỗi cá nhân hóa.

---

## 1. Giới thiệu Dự án (Project Overview)

**IELTS AI Tutor** được thiết kế nhằm mang lại trải nghiệm luyện thi cá nhân hóa cao cấp với phong cách thiết kế **Academic Luxury**:
- **Trợ lý Én**: Đồng hành xuyên suốt quá trình làm bài, hỗ trợ giải thích ngữ cảnh, tra cứu lời giải và phản hồi tương tác bằng tool calling có kiểm soát.
- **Luyện 4 kỹ năng độc lập**:
  - **Reading & Listening**: Không gian làm bài chia đôi (Split Learning Workspace), tự động chấm điểm theo answer key chuẩn.
  - **Writing**: Chấm bài tự động theo 4 tiêu chí IELTS (Task Response, Coherence, Lexical Resource, Grammatical Range) và cung cấp band score ước lượng.
  - **Speaking**: Giao diện phòng luyện với visualizer âm thanh, ghi âm cục bộ và ranh giới transcript/review trung thực.
- **RAG & Tìm kiếm Ngữ nghĩa**: Hệ thống nhúng tài liệu và bài tập vào vector database (PostgreSQL + pgvector), cho phép tìm kiếm và truy xuất thông tin chính xác.

---

## 2. Kiến trúc Hệ thống (Architecture)

Dự án tuân thủ mô hình kiến trúc **Modular Monolith** kết hợp **Domain-Driven Design (DDD)** và **Clean/Layered Architecture**:

- **Mô hình kiến trúc**: Modular Monolith tách biệt rõ ràng giữa các domain nghiệp vụ (`auth`, `practice`, `writing`, `speaking`, `tutor`, `rag`, `learning`).
- **Backend (Java Spring Boot 3.3+)**:
  - Xử lý nghiệp vụ tập trung, quản lý migration cơ sở dữ liệu với Flyway.
  - Điều phối đa nhà cung cấp AI độc lập với vendor (Groq, Cloudflare Workers AI, Google Gemini).
  - Tích hợp vector search thông qua PostgreSQL extension `pgvector` (`vector(768)`).
- **Frontend (React 18 + Vite)**:
  - Thiết kế theo chuẩn **Academic Luxury**: Font tiêu đề `Playfair Display`, font giao diện `Inter`.
  - Hiệu ứng mượt mà với Framer Motion và biểu đồ phân tích năng lực với Recharts.
  - Quản lý trạng thái làm bài độc lập, hỗ trợ phân tách màn hình và khả năng tương thích cao.

---

## 3. Cấu trúc Dự án (Project Structure)

```text
ielts-ai/
├── backend/                               # Spring Boot Backend
│   ├── src/main/java/com/ieltsaitutor/
│   │   ├── auth/                          # Xác thực người dùng, JWT & Security
│   │   ├── practice/                      # Nghiệp vụ luyện đề Reading & Listening
│   │   ├── writing/                       # Chấm bài Writing & đánh giá tiêu chí
│   │   ├── speaking/                      # Phòng luyện Speaking & giao thức STT
│   │   ├── tutor/                         # Trợ lý AI Tutor: orchestrator, memory, tools
│   │   ├── rag/                           # RAG engine, vector chunking & retrieval
│   │   ├── search/                        # Tìm kiếm bài tập ngữ nghĩa và keyword
│   │   ├── learning/                      # Tiến độ học, chuỗi ngày streak, bản nháp
│   │   └── admin/                         # Quản trị CMS, RAG index & generator
│   ├── src/main/resources/
│   │   ├── db/migration/                  # Flyway migrations (hiện đến V70)
│   │   └── application.properties        # Cấu hình backend
│   ├── docker-compose.yml                 # PostgreSQL 16 + pgvector container
│   └── pom.xml                            # Maven dependencies
├── frontend/                              # React + Vite Frontend
│   ├── src/
│   │   ├── components/                    # UI Components (layout, tutor, workspace,...)
│   │   ├── features/                      # Business logic modules (auth, tutor, practice,...)
│   │   ├── pages/                         # Các trang ứng dụng (Home, Practice, Writing,...)
│   │   ├── services/                      # API client giao tiếp với backend
│   │   ├── styles/                        # Tailwind CSS & global styles
│   │   └── main.jsx                       # Entry point React
│   ├── package.json                       # NPM dependencies
│   └── vite.config.js                     # Vite build configuration
├── docs/                                  # Tài liệu thiết kế, báo cáo & runbooks
├── scripts/                               # Scripts khởi động tự động và quản lý DB
│   ├── START_IELTS.bat                    # Script khởi chạy toàn bộ 1-click cho Windows
│   └── start-ielts.ps1                    # PowerShell startup script
├── architecture.yaml                      # Đặc tả ma trận phụ thuộc module
├── AGENTS.md                              # Quy chuẩn thiết kế và phát triển agent
└── README.md                              # Tài liệu tổng quan dự án
```

---

## 4. Công nghệ Sử dụng (Tech Stack)

| Thành phần | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3+, Spring Data JDBC, Flyway |
| **Frontend** | React 18, Vite, Tailwind CSS, Framer Motion, Recharts |
| **Database** | PostgreSQL 16, pgvector extension |
| **AI / LLM** | Google Gemini API, Groq, Cloudflare Workers AI |
| **DevOps & Tooling** | Docker, Maven, Node.js / NPM |

---

## 5. Hướng dẫn Khởi chạy (Local Setup)

### Bước 1: Khởi động Cơ sở dữ liệu
```powershell
docker compose -f backend/docker-compose.yml up -d
```

### Bước 2: Chạy Backend
```powershell
cd backend
./mvnw.cmd spring-boot:run
```

### Bước 3: Chạy Frontend
Mở terminal khác:
```powershell
cd frontend
npm install
npm run dev
```

Hoặc chạy nhanh toàn bộ bằng script:
```powershell
.\scripts\START_IELTS.bat
```

## 6. Tính năng hiện có

- Học viên: đăng ký/đăng nhập, onboarding, Practice Bank, Reading, Listening,
  Writing, Speaking, autosave, nộp bài, kết quả, lịch sử, tiến độ, kế hoạch
  hôm nay, bài đã lưu, diagnostic và mock test.
- Én: hội thoại được lưu, ngữ cảnh bài luyện, deterministic tools, RAG có
  citation/insufficient-evidence state và attachment pipeline cho các định
  dạng được cấu hình.
- Quản trị: quản lý học viên, kho bài, RAG, AI Practice Generator, validation,
  review và phê duyệt trước khi nội dung xuất hiện trong Practice Bank.
- Bảo mật: kiểm tra role ở backend, ownership isolation cho tài nguyên learner,
  upload validation và provider-neutral frontend contract.

## 7. Biến môi trường

Sao chép `backend/.env.example` thành cấu hình local của bạn. Các provider AI
đều là tùy chọn khi chạy automated tests; Groq được chọn mặc định làm provider
chat chính, sau đó Cloudflare và Gemini làm fallback. Không đưa API key vào
frontend hoặc commit vào Git.

Frontend hỗ trợ `VITE_API_PROXY_TARGET`; khi không đặt biến này, môi trường
local mặc định proxy `/api` tới `http://127.0.0.1:8081`.

## 8. Kiểm thử

```powershell
cd frontend
npm test -- --run
npm run lint
npm run build
cd ..\backend
.\mvnw.cmd test
.\mvnw.cmd package
```

## 9. Giới hạn cần biết

- PostgreSQL/pgvector phải chạy trước khi backend khởi động đầy đủ và Flyway
  được kiểm tra.
- Speaking chỉ phân tích pronunciation khi provider audio/transcript tương ứng
  thực sự được cấu hình; giao diện không tự bịa điểm.
- Real-provider smoke test cần credential, quota và môi trường mạng hợp lệ.
