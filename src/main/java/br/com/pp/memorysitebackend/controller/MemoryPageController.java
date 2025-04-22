package br.com.pp.memorysitebackend.controller;

import br.com.pp.memorysitebackend.dto.CreateMemoryPageRequest;
import br.com.pp.memorysitebackend.dto.MemoryPageResponse;
import br.com.pp.memorysitebackend.service.MemoryPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryPageController {

    private final MemoryPageService memoryPageService;
    private static final Logger log = LoggerFactory.getLogger(MemoryPageController.class);


    @PostMapping
    public ResponseEntity<MemoryPageResponse> createMemoryPage(@Valid @RequestBody CreateMemoryPageRequest requestDto) {
        log.info("Recebida requisição para criar MemoryPage: {}", requestDto);
        MemoryPageResponse savedPageDto = memoryPageService.createMemoryPage(requestDto);
        log.info("MemoryPage criada com sucesso: {}", savedPageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPageDto);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<MemoryPageResponse> getMemoryPageBySlug(@PathVariable String slug) {
        log.info("Recebida requisição para buscar MemoryPage com slug: {}", slug);
        Optional<MemoryPageResponse> responseDtoOptional = memoryPageService.getMemoryPageBySlug(slug);
        return responseDtoOptional
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<MemoryPageResponse>> getAllMemoryPages() {
        log.info("Recebida requisição para listar todas MemoryPages");
        List<MemoryPageResponse> pageDtos = memoryPageService.getAllMemoryPages();
        log.info("Retornando {} MemoryPages", pageDtos.size());
        return ResponseEntity.ok(pageDtos);
    }

    @PutMapping("/{slug}")

    public ResponseEntity<MemoryPageResponse> updateMemoryPage(@PathVariable String slug, @Valid @RequestBody CreateMemoryPageRequest requestDto) {
        log.info("Recebida requisição para atualizar MemoryPage com slug {}: {}", slug, requestDto);
        Optional<MemoryPageResponse> updatedPageDtoOptional = memoryPageService.updateMemoryPage(slug, requestDto);
        if(updatedPageDtoOptional.isPresent()) {
            log.info("MemoryPage atualizada com sucesso para slug {}", slug);
        } else {
            log.info("Nenhuma MemoryPage encontrada para atualizar com slug {}", slug);
        }
        return updatedPageDtoOptional
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<Void> deleteMemoryPage(@PathVariable String slug) {
        log.info("Recebida requisição para deletar MemoryPage com slug: {}", slug);
        boolean deleted = memoryPageService.deleteMemoryPageBySlug(slug);
        if (deleted) {
            log.info("MemoryPage deletada com sucesso para slug {}", slug);
            return ResponseEntity.noContent().build();
        } else {
            log.info("Nenhuma MemoryPage encontrada para deletar com slug {}", slug);
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{slug}/qrcode")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String slug) throws IOException {
        log.info("Recebida requisição para gerar QR Code para slug: {}", slug);
        byte[] qrCodeBytes = memoryPageService.generateQrCodeForSlug(slug);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrCodeBytes.length);
        log.info("Retornando imagem QR Code para slug: {}", slug);
        return new ResponseEntity<>(qrCodeBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{slug}/images")
    public ResponseEntity<List<String>> uploadImages(
                                                      @PathVariable String slug,
                                                      @RequestParam("files") List<MultipartFile> files) throws IOException {
        log.info("Recebida requisição para upload de {} arquivos para slug: {}", files.size(), slug);
        List<String> savedFileNames = memoryPageService.uploadAndAssociateImages(slug, files);
        log.info("Arquivos salvos com sucesso para slug {}: {}", slug, savedFileNames);
        return ResponseEntity.ok(savedFileNames);
    }

    @PostMapping("/{slug}/music")

    public ResponseEntity<String> uploadMusic(
                                               @PathVariable String slug,
                                               @RequestParam("musicFile") MultipartFile musicFile) throws IOException {
        log.info("Recebida requisição para upload de música para slug: {}", slug);
        String savedPublicUrl = memoryPageService.uploadAndAssociateMusic(slug, musicFile);
        log.info("Música salva com sucesso para slug {}: {}", slug, savedPublicUrl);
        return ResponseEntity.ok(savedPublicUrl);
    }
}