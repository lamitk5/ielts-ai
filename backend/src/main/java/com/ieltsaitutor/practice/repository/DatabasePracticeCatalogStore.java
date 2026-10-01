package com.ieltsaitutor.practice.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.PracticeSet;

@Component
public class DatabasePracticeCatalogStore {

    private final Map<String, PracticeSet> store = new ConcurrentHashMap<>();

    public void save(PracticeSet set) {
        if (set != null && set.id() != null) {
            store.put(set.id(), set);
        }
    }

    public List<PracticeSet> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<PracticeSet> findBySkill(String skill) {
        if (skill == null) return List.of();
        String normalized = skill.trim().toLowerCase();
        return store.values().stream()
                .filter(s -> s.skill() != null && s.skill().equalsIgnoreCase(normalized))
                .toList();
    }

    public Optional<PracticeSet> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }
}
