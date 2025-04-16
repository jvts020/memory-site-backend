package br.com.pp.memorysitebackend.controller; // Ajuste seu pacote

import br.com.pp.memorysitebackend.entity.MemoryPage;
// Remova a importação do Repository: import br.com.pp.memorysitebackend.repository.MemoryPageRepository;
import br.com.pp.memorysitebackend.service.MemoryPageService; // Importe o Serviço
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Remova a importação do ResponseStatusException se não for mais usada diretamente aqui
// import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
// Remova a importação do UUID se não for mais usada aqui
// import java.util.UUID;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryPageController {

    // Injeta o Serviço em vez do Repositório
    private final MemoryPageService memoryPageService;

    // --- Endpoint CREATE (POST /) ---
    @PostMapping
    public ResponseEntity<?> createMemoryPage(@RequestBody MemoryPage memoryPageRequest) {
        try {
            // Delega a criação (incluindo validação e slug) para o Serviço
            MemoryPage savedPage = memoryPageService.createMemoryPage(memoryPageRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPage);
        } catch (IllegalArgumentException e) {
            // Captura erros de validação do serviço
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Captura outros erros inesperados
            // Logar o erro 'e' aqui
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao criar memória.");
        }
    }

    // --- Endpoint READ by Slug (GET /{slug}) ---
    @GetMapping("/{slug}")
    public ResponseEntity<?> getMemoryPageBySlug(@PathVariable String slug) {
        Optional<MemoryPage> memoryPageOptional = memoryPageService.getMemoryPageBySlug(slug);

        // Retorna 200 OK com os dados se encontrado, ou 404 Not Found se vazio
        return memoryPageOptional
                .<ResponseEntity<?>>map(ResponseEntity::ok) // Se presente, cria ResponseEntity.ok(page)
                .orElseGet(() -> ResponseEntity.notFound().build()); // Se vazio, cria ResponseEntity.notFound()
    }

    // --- Endpoint READ All (GET /) --- NOVO!
    @GetMapping
    public ResponseEntity<List<MemoryPage>> getAllMemoryPages() {
        List<MemoryPage> pages = memoryPageService.getAllMemoryPages();
        return ResponseEntity.ok(pages); // Retorna 200 OK com a lista (pode ser vazia)
    }

    // --- Endpoint UPDATE (PUT /{slug}) --- NOVO!
    // Usamos PUT para substituição completa (ou parcial, como implementado no serviço)
    @PutMapping("/{slug}")
    public ResponseEntity<?> updateMemoryPage(@PathVariable String slug, @RequestBody MemoryPage updatedPageData) {
        try {
            Optional<MemoryPage> updatedPageOptional = memoryPageService.updateMemoryPage(slug, updatedPageData);
            return updatedPageOptional
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Logar erro 'e'
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao atualizar memória.");
        }
    }

    // --- Endpoint DELETE (DELETE /{slug}) --- NOVO!
    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteMemoryPage(@PathVariable String slug) {
        boolean deleted = memoryPageService.deleteMemoryPageBySlug(slug);
        if (deleted) {
            return ResponseEntity.noContent().build(); // Retorna 204 No Content (sucesso sem corpo)
        } else {
            return ResponseEntity.notFound().build(); // Retorna 404 Not Found
        }
    }
}