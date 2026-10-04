package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class MockTestCatalogService {
    private final MockTestCatalogRepository repository;
    public MockTestCatalogService(MockTestCatalogRepository repository) { this.repository = repository; }
    public List<MockTestCatalogItem> published() { return repository.findAll(true); }
    public List<MockTestCatalogItem> all() { return repository.findAll(false); }
    public MockTestCatalogItem save(Command command) {
        if (command == null || command.slug() == null || command.slug().isBlank() || command.title() == null || command.title().isBlank() || command.sections() == null || command.sections().isEmpty()) throw new IllegalArgumentException("Mock Test cần tiêu đề và ít nhất một phần thi.");
        Instant now = Instant.now();
        MockTestCatalogItem old = repository.findBySlug(command.slug()).orElse(null);
        return repository.save(new MockTestCatalogItem(old == null ? UUID.randomUUID() : old.id(), command.slug().trim(), command.title().trim(), command.version() == null ? "v1" : command.version(), command.sections().stream().mapToInt(MockTestCatalogItem.Section::timeLimitSeconds).sum(), command.published(), old == null ? now : old.createdAt(), now, List.copyOf(command.sections())));
    }
    public MockTestCatalogItem update(UUID id, Command command) {
        if (repository.findById(id).isEmpty()) throw new java.util.NoSuchElementException("Không tìm thấy bài thi thử.");
        if (command == null || command.slug() == null || command.slug().isBlank() || command.title() == null || command.title().isBlank() || command.sections() == null || command.sections().isEmpty()) throw new IllegalArgumentException("Mock Test cần tiêu đề và ít nhất một phần thi.");
        Instant now = Instant.now();
        MockTestCatalogItem old = repository.findById(id).orElseThrow();
        return repository.save(new MockTestCatalogItem(id, command.slug().trim(), command.title().trim(), command.version() == null ? "v1" : command.version(), command.sections().stream().mapToInt(MockTestCatalogItem.Section::timeLimitSeconds).sum(), command.published(), old.createdAt(), now, List.copyOf(command.sections())));
    }
    public MockTestCatalogItem publish(UUID id, boolean published) { MockTestCatalogItem old = repository.findById(id).orElseThrow(); return repository.save(new MockTestCatalogItem(old.id(), old.slug(), old.title(), old.version(), old.totalTimeLimitSeconds(), published, old.createdAt(), Instant.now(), old.sections())); }
    public void delete(UUID id) { repository.delete(id); }
    public record Command(String slug, String title, String version, boolean published, List<MockTestCatalogItem.Section> sections) {}
}
