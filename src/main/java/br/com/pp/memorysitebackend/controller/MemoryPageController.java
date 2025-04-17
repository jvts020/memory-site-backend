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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryPageController {

    private final MemoryPageService memoryPageService;
    private static final Logger log = LoggerFactory.getLogger(MemoryPageController.class);

    @PostMapping
    public ResponseEntity<?> createMemoryPage(@Valid @RequestBody CreateMemoryPageRequest requestDto) {
        log.info("Recebida requisição para criar MemoryPage: {}", requestDto);
        try {
            MemoryPageResponse savedPageDto = memoryPageService.createMemoryPage(requestDto);
            log.info("MemoryPage criada com sucesso: {}", savedPageDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPageDto);
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao criar MemoryPage: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao criar MemoryPage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao criar memória.");
        }
    }

    @GetMapping("/{slug}")
    public ResponseEntity<MemoryPageResponse> getMemoryPageBySlug(@PathVariable String slug) {
        log.info("Recebida requisição para buscar MemoryPage com slug: {}", slug);
        Optional<MemoryPageResponse> responseDtoOptional = memoryPageService.getMemoryPageBySlug(slug);
        if(responseDtoOptional.isPresent()) {
            log.info("MemoryPage encontrada para slug {}", slug);
        } else {
            log.info("Nenhuma MemoryPage encontrada para slug {}", slug);
        }
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
    public ResponseEntity<?> updateMemoryPage(@PathVariable String slug, @Valid @RequestBody CreateMemoryPageRequest requestDto) {
        log.info("Recebida requisição para atualizar MemoryPage com slug {}: {}", slug, requestDto);
        try {
            Optional<MemoryPageResponse> updatedPageDtoOptional = memoryPageService.updateMemoryPage(slug, requestDto);
            if(updatedPageDtoOptional.isPresent()) {
                log.info("MemoryPage atualizada com sucesso para slug {}", slug);
            } else {
                log.info("Nenhuma MemoryPage encontrada para atualizar com slug {}", slug);
            }
            return updatedPageDtoOptional
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao atualizar MemoryPage com slug {}: {}", slug, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar MemoryPage com slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno ao atualizar memória.");
        }
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
    public ResponseEntity<?> getQrCode(@PathVariable String slug) {
        log.info("Recebida requisição para gerar QR Code para slug: {}", slug);
        try {
            byte[] qrCodeBytes = memoryPageService.generateQrCodeForSlug(slug);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeBytes.length);

            log.info("Retornando imagem QR Code para slug: {}", slug);
            return new ResponseEntity<>(qrCodeBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.warn("Erro ao gerar QR Code para slug {}: {}", slug, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Erro de IO ao gerar QR Code para slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao gerar imagem QR Code.");
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar QR Code para slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro inesperado.");
        }
    }


    @PostMapping("/{slug}/images")
    public ResponseEntity<?> uploadImages(
            @PathVariable String slug,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Recebida requisição para upload de {} arquivos para slug: {}", files.size(), slug);
        try {
            List<String> savedFileNames = memoryPageService.uploadAndAssociateImages(slug, files);
            log.info("Arquivos salvos com sucesso para slug {}: {}", slug, savedFileNames);
            return ResponseEntity.ok(savedFileNames);

        } catch (IllegalArgumentException e) {
            log.warn("Erro de argumento ao fazer upload para slug {}: {}", slug, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("Erro de IO ao fazer upload para slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar um ou mais arquivos.");
        } catch (Exception e) {
            log.error("Erro inesperado ao fazer upload para slug {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro inesperado no servidor.");
        }
    }
}