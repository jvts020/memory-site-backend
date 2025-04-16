package br.com.pp.memorysitebackend.service;

import br.com.pp.memorysitebackend.entity.MemoryPage;
import br.com.pp.memorysitebackend.repository.MemoryPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service; // Marca como um componente de Serviço do Spring
import org.springframework.transaction.annotation.Transactional; // Para operações de escrita

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service // Indica ao Spring que esta classe contém lógica de negócio
@RequiredArgsConstructor // Lombok para injeção de dependência via construtor
public class MemoryPageServiceImpl implements MemoryPageService {

    private final MemoryPageRepository memoryPageRepository;

    @Override
    @Transactional // Garante que a operação seja atômica (ou salva tudo ou nada)
    public MemoryPage createMemoryPage(MemoryPage memoryPage) {
        // --- Lógica movida do Controller ---
        // Validação básica (pode ser melhorada com DTOs e Bean Validation depois)
        if (memoryPage.getDedicatedText() == null || memoryPage.getDedicatedText().isBlank()) {
            throw new IllegalArgumentException("Texto dedicado não pode ser vazio.");
        }
        if (memoryPage.getImageUrls() != null && memoryPage.getImageUrls().size() > 7) {
            throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
        }

        // Geração do Slug (pode ser melhorado depois)
        String slug = UUID.randomUUID().toString().substring(0, 8);
        memoryPage.setSlug(slug);

        // Garantir estado inicial correto
        memoryPage.setId(null); // Garante criação
        memoryPage.setViewCount(0);
        memoryPage.setSynced(false); // Assume que não está sincronizado ao criar/modificar

        // Salva no banco através do repositório
        return memoryPageRepository.save(memoryPage);
    }

    @Override
    @Transactional(readOnly = true) // Opcional: Indica que é uma transação apenas de leitura (otimização)
    public Optional<MemoryPage> getMemoryPageBySlug(String slug) {
        // Lógica de incremento de view (exemplo simples)
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            MemoryPage page = pageOptional.get();
            page.setViewCount(page.getViewCount() + 1);
            memoryPageRepository.save(page); // Salva a view incrementada
            return Optional.of(page); // Retorna a página atualizada
        }
        return Optional.empty(); // Ou retorna o Optional vazio diretamente
        // return memoryPageRepository.findBySlug(slug); // Busca simples sem contador
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryPage> getAllMemoryPages() {
        return memoryPageRepository.findAll(); // Busca todos
    }

    @Override
    @Transactional
    public Optional<MemoryPage> updateMemoryPage(String slug, MemoryPage updatedPageData) {
        // 1. Busca a página existente pelo slug
        Optional<MemoryPage> existingPageOptional = memoryPageRepository.findBySlug(slug);

        if (existingPageOptional.isPresent()) {
            MemoryPage existingPage = existingPageOptional.get();

            // 2. Atualiza os campos desejados (CUIDADO: não sobrescreva ID, slug, creationDate)
            // Validações podem ser adicionadas aqui também
            if (updatedPageData.getDedicatedText() != null && !updatedPageData.getDedicatedText().isBlank()) {
                existingPage.setDedicatedText(updatedPageData.getDedicatedText());
            }
            if (updatedPageData.getImageUrls() != null) {
                if(updatedPageData.getImageUrls().size() > 7) {
                    throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
                }
                // Substitui a lista inteira (pode ser otimizado se necessário)
                existingPage.getImageUrls().clear();
                existingPage.getImageUrls().addAll(updatedPageData.getImageUrls());
            }
            if (updatedPageData.getMusicUrl() != null) {
                existingPage.setMusicUrl(updatedPageData.getMusicUrl());
            }
            if (updatedPageData.getTargetDate() != null) {
                existingPage.setTargetDate(updatedPageData.getTargetDate());
            }
            existingPage.setSynced(false); // Marcar como não sincronizado ao atualizar

            // 3. Salva a entidade atualizada
            return Optional.of(memoryPageRepository.save(existingPage));
        } else {
            return Optional.empty(); // Retorna vazio se não encontrou a página para atualizar
        }
    }

    @Override
    @Transactional
    public boolean deleteMemoryPageBySlug(String slug) {
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            memoryPageRepository.delete(pageOptional.get()); // Deleta a entidade encontrada
            // Alternativa: memoryPageRepository.deleteById(pageOptional.get().getId());
            return true; // Indica que deletou com sucesso
        } else {
            return false; // Indica que não encontrou para deletar
        }
    }
}