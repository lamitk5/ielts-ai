package com.ieltsaitutor.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestCatalogRepository {
    List<MockTestCatalogItem> findAll(boolean publishedOnly);
    Optional<MockTestCatalogItem> findBySlug(String slug);
    Optional<MockTestCatalogItem> findById(UUID id);
    MockTestCatalogItem save(MockTestCatalogItem item);
    void delete(UUID id);
}
