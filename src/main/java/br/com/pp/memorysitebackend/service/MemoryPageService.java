package br.com.pp.memorysitebackend.service;

import br.com.pp.memorysitebackend.entity.MemoryPage;
import java.util.List;
import java.util.Optional;

public interface MemoryPageService {
    MemoryPage createMemoryPage(MemoryPage memoryPage);
    Optional<MemoryPage> getMemoryPageBySlug(String slug);
    List<MemoryPage> getAllMemoryPages();
    Optional<MemoryPage> updateMemoryPage(String slug, MemoryPage updatedPageData);
    boolean deleteMemoryPageBySlug(String slug);
}