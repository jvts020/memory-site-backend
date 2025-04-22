package br.com.pp.memorysitebackend.service;

import java.io.IOException;
import br.com.pp.memorysitebackend.dto.CreateMemoryPageRequest;
import br.com.pp.memorysitebackend.dto.MemoryPageResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface MemoryPageService {

    MemoryPageResponse createMemoryPage(CreateMemoryPageRequest requestDto);

    Optional<MemoryPageResponse> getMemoryPageBySlug(String slug);

    List<MemoryPageResponse> getAllMemoryPages();

    Optional<MemoryPageResponse> updateMemoryPage(String slug, CreateMemoryPageRequest updatedPageData);

    boolean deleteMemoryPageBySlug(String slug);

    List<String> uploadAndAssociateImages(String slug, List<MultipartFile> files) throws IOException, IllegalArgumentException;

    byte[] generateQrCodeForSlug(String slug) throws IOException, IllegalArgumentException;

    String uploadAndAssociateMusic(String slug, MultipartFile musicFile) throws IOException, IllegalArgumentException;

}