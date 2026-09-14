# IELTS Learning Platform - Modular Monolith

Dự án backend nền tảng học và luyện thi IELTS được thiết kế theo kiến trúc **Modular Monolith** sử dụng **Spring Boot 3.3+**, **Java 23**, và **Spring Modulith**.

---

## 1. Công nghệ & Thư viện

- **Java**: 23
- **Spring Boot**: 3.3.4
- **Spring Modulith**: 1.2.4 (`spring-modulith-starter-core`, `spring-modulith-starter-test`)
- **Maven**: 3.9+

---

## 2. Cấu trúc Module & Package

Hệ thống được tổ chức thành các module độc lập với ranh giới rõ ràng thông qua `package-info.java` và `@ApplicationModule`:

| Module | Package | Loại | Mô tả | Allowed Dependencies |
|---|---|---|---|---|
| **presentation** | `com.ielts.presentation` | Delivery | REST API, Controller, Web endpoints | `shared`, `application` |
| **application** | `com.ielts.application` | Orchestration | Điều phối nghiệp vụ liên module | `shared`, `infrastructure`, domain submodules, ai submodules |
| **domain.user** | `com.ielts.domain.user` | Domain | Thực thể User, quản lý người học & giảng viên | `shared` |
| **domain.course** | `com.ielts.domain.course` | Domain | Thực thể Course, chương trình học | `shared`, `user` |
| **domain.lesson** | `com.ielts.domain.lesson` | Domain | Thực thể Lesson, bài học trong khóa | `shared`, `course` |
| **domain.skill** | `com.ielts.domain.skill` | Domain | Thực thể Skill, 4 kỹ năng (L/R/W/S) | `shared`, `lesson` |
| **domain.progress** | `com.ielts.domain.progress` | Domain | Tiến độ học, điểm số, lịch sử luyện tập | `shared`, `user`, `course`, `lesson`, `skill` |
| **ai.asr** | `com.ielts.ai.asr` | AI Submodule | Nhận dạng giọng nói (Automatic Speech Recognition) | `shared` |
| **ai.nlp.writing** | `com.ielts.ai.nlp.writing` | AI Submodule | Chấm & phân tích bài viết IELTS Writing | `shared` |
| **ai.nlp.speaking** | `com.ielts.ai.nlp.speaking` | AI Submodule | Đánh giá phát âm, ngữ pháp Speaking | `shared`, `asr` |
| **ai.tts** | `com.ielts.ai.tts` | AI Submodule | Tổng hợp giọng đọc (Text to Speech) | `shared` |
| **ai.recommendation** | `com.ielts.ai.recommendation` | AI Submodule | Gợi ý bài học & lộ trình cá nhân hóa | `shared`, `progress`, `course` |
| **ai.chatbot** | `com.ielts.ai.chatbot` | AI Submodule | Trợ lý ảo hội thoại luyện nói/hỏi đáp | `shared` |
| **infrastructure** | `com.ielts.infrastructure` | Technical | Cấu hình kỹ thuật, adapter bên thứ ba | `shared` |
| **shared** | `com.ielts.shared` | Cross-Cutting | Tiện ích chung, base types, exceptions | Không phụ thuộc module nào |

---

## 3. Quy ước Stub & Ranh giới

1. **Package Info**: Mỗi package module chứa file `package-info.java` với annotation:
   ```java
   @ApplicationModule(
       displayName = "...",
       allowedDependencies = { ... }
   )
   package com.ielts.<module>;
   ```
2. **Spring Modulith Detection Strategy**: Cấu hình trong `application.properties`:
   ```properties
   spring.modulith.detection-strategy=explicitly-annotated
   ```
3. **Stubs**:
   - Mỗi module có 1 `@Service` hoặc `@Component` rỗng.
   - Mỗi sub-module domain có 1 Entity stub (`User`, `Course`, `Lesson`, `Skill`, `Progress`).
   - Mỗi module AI có 1 interface `Evaluator` và 1 implementation rỗng (`Default*Evaluator`).

---

## 4. Kiểm tra Kiến trúc (Modularity Verification)

Kiểm tra ranh giới phụ thuộc tự động thông qua JUnit test:
```java
@Test
void verifyModularArchitecture() {
    ApplicationModules.of(IeltsApplication.class).verify();
}
```
Nếu có module nào vi phạm danh sách `allowedDependencies`, test sẽ báo lỗi ngay lập tức khi build.

---

## 5. Phân tích Phổ (Spectral Analysis)

File `architecture.yaml` tại thư mục gốc định nghĩa toàn bộ:
- Danh sách các node (15 modules).
- Các cạnh phụ thuộc có hướng (`source` $\to$ `target`).
- Trọng số và kiểu quan hệ (`orchestration`, `domain_collaboration`, `ai_pipeline`, `shared_kernel`).

Dùng trực tiếp cho các thuật toán phân tích phổ đồ thị:
- **Ma trận kề (Adjacency Matrix)** $A$
- **Ma trận bậc (Degree Matrix)** $D$
- **Ma trận Laplacian** $L = D - A$
- **Phân cụm phổ (Spectral Clustering)** và đánh giá hệ số mô đun (Modularity Q).
