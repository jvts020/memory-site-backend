package br.com.pp.memorysitebackend.service; // Seu pacote
import java.io.IOException;
// Importe os DTOs
import br.com.pp.memorysitebackend.dto.CreateMemoryPageRequest;
import br.com.pp.memorysitebackend.dto.MemoryPageResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
// Remova a importação da entidade se não for mais usada diretamente na assinatura
// import br.com.pp.memorysitebackend.entity.MemoryPage;

import java.util.List;
import java.util.Optional;

public interface MemoryPageService {

    // Recebe DTO de Request, Retorna DTO de Response
    MemoryPageResponse createMemoryPage(CreateMemoryPageRequest requestDto);

    // Retorna Optional de DTO de Response
    Optional<MemoryPageResponse> getMemoryPageBySlug(String slug);

    // Retorna Lista de DTOs de Response
    List<MemoryPageResponse> getAllMemoryPages();

    // Recebe DTO de Request, Retorna Optional de DTO de Response
    Optional<MemoryPageResponse> updateMemoryPage(String slug, CreateMemoryPageRequest updatedPageData); // Reutilizando DTO por enquanto

    // Mantém retorno boolean ou pode retornar void se preferir lançar exceção caso não encontre
    boolean deleteMemoryPageBySlug(String slug);

    List<String> uploadAndAssociateImages(String slug, List<MultipartFile> files) throws IOException, IllegalArgumentException;

    byte[] generateQrCodeForSlug(String slug) throws IOException, IllegalArgumentException;
}