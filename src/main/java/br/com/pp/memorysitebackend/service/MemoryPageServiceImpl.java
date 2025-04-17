package br.com.pp.memorysitebackend.service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import net.glxn.qrgen.javase.QRCode;
import net.glxn.qrgen.core.image.ImageType;
import br.com.pp.memorysitebackend.dto.CreateMemoryPageRequest;
import br.com.pp.memorysitebackend.dto.MemoryPageResponse;
import br.com.pp.memorysitebackend.entity.MemoryPage;
import br.com.pp.memorysitebackend.repository.MemoryPageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MemoryPageServiceImpl implements MemoryPageService {

    private final MemoryPageRepository memoryPageRepository;
    private static final Logger log = LoggerFactory.getLogger(MemoryPageServiceImpl.class);

    private static final Pattern NON_ALPHANUMERIC_OR_HYPHEN = Pattern.compile("[^a-z0-9-]");
    private static final Pattern HYPHEN_DUPLICATES = Pattern.compile("-{2,}");

    private final S3Client s3Client;


    @Value("${supabase.api.url}")
    private String supabaseApiUrl;

    @Value("${supabase.bucket.name}")
    private String supabaseBucketName;

    @Value("${app.base-url}")
    private String appBaseUrl;
    @Override
    @Transactional

    public MemoryPageResponse createMemoryPage(CreateMemoryPageRequest requestDto) {
        if (requestDto.getImageUrls() != null && requestDto.getImageUrls().size() > 7) {
            throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
        }

        MemoryPage memoryPage = mapToEntity(requestDto);

        String finalSlug;
        if (requestDto.getSuggestedSlug() != null && !requestDto.getSuggestedSlug().isBlank()) {
            finalSlug = generateUniqueSlug(requestDto.getSuggestedSlug());
        } else {
            String baseSlug = sanitizeSlug(requestDto.getDedicatedText().substring(0, Math.min(requestDto.getDedicatedText().length(), 30)));
            if (baseSlug.isEmpty()) { baseSlug = "memoria"; }
            finalSlug = generateUniqueSlug(baseSlug);
        }
        memoryPage.setSlug(finalSlug);

        memoryPage.setId(null);
        memoryPage.setViewCount(0);
        memoryPage.setSynced(false);

        MemoryPage savedPage = memoryPageRepository.save(memoryPage);
        log.info("MemoryPage criada com slug: {}", savedPage.getSlug());
        return mapToDto(savedPage);
    }

    @Override
    @Transactional
    public Optional<MemoryPageResponse> getMemoryPageBySlug(String slug) {
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            MemoryPage page = pageOptional.get();
            page.setViewCount(page.getViewCount() + 1);
            memoryPageRepository.save(page);
            return Optional.of(mapToDto(page));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryPageResponse> getAllMemoryPages() {
        return memoryPageRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<MemoryPageResponse> updateMemoryPage(String slug, CreateMemoryPageRequest updatedPageData) {
        Optional<MemoryPage> existingPageOptional = memoryPageRepository.findBySlug(slug);

        if (existingPageOptional.isPresent()) {
            MemoryPage existingPage = existingPageOptional.get();

            if (updatedPageData.getImageUrls() != null && updatedPageData.getImageUrls().size() > 7) {
                throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
            }

            if (updatedPageData.getDedicatedText() != null) {
                existingPage.setDedicatedText(updatedPageData.getDedicatedText());
            }
            if (updatedPageData.getImageUrls() != null) {
                existingPage.getImageUrls().clear();
                existingPage.getImageUrls().addAll(updatedPageData.getImageUrls());
            }
            if (updatedPageData.getMusicUrl() != null) {
                existingPage.setMusicUrl(updatedPageData.getMusicUrl());
            }
            if (updatedPageData.getTargetDate() != null) {
                existingPage.setTargetDate(updatedPageData.getTargetDate());
            }
            existingPage.setSynced(false);

            MemoryPage savedPage = memoryPageRepository.save(existingPage);
            return Optional.of(mapToDto(savedPage));
        } else {
            return Optional.empty();
        }
    }

    @Override
    @Transactional

    public boolean deleteMemoryPageBySlug(String slug) {
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            memoryPageRepository.delete(pageOptional.get());
            log.info("MemoryPage deletada com slug: {}", slug);
            return true;
        } else {
            log.warn("Tentativa de deletar MemoryPage com slug não encontrado: {}", slug);
            return false;
        }
    }

    private MemoryPage mapToEntity(CreateMemoryPageRequest dto) {
        MemoryPage entity = new MemoryPage();
        entity.setDedicatedText(dto.getDedicatedText());
        entity.setImageUrls(dto.getImageUrls() != null ? new ArrayList<>(dto.getImageUrls()) : new ArrayList<>());
        entity.setMusicUrl(dto.getMusicUrl());
        entity.setTargetDate(dto.getTargetDate());
        return entity;
    }

    private MemoryPageResponse mapToDto(MemoryPage entity) {
        return new MemoryPageResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getDedicatedText(),
                entity.getImageUrls() != null ? new ArrayList<>(entity.getImageUrls()) : new ArrayList<>(),
                entity.getMusicUrl(),
                entity.getTargetDate(),
                entity.getCreationDate(),
                entity.getViewCount()
        );
    }

    private String generateUniqueSlug(String baseSuggestion) {
        String currentSlug = sanitizeSlug(baseSuggestion);
        if (currentSlug.isEmpty()) {
            currentSlug = UUID.randomUUID().toString().substring(0, 8);
        }
        int attempt = 0;
        String originalSlug = currentSlug;

        // Loop para garantir unicidade
        while (memoryPageRepository.findBySlug(currentSlug).isPresent()) {
            attempt++;
            String suffix = "-" + attempt;
            int maxBaseLength = 50 - suffix.length();
            String base = originalSlug.substring(0, Math.min(originalSlug.length(), maxBaseLength));
            currentSlug = base + suffix;
            log.warn("Colisão de slug detectada para '{}'. Tentando '{}'", baseSuggestion, currentSlug);
            if (attempt > 10) {
                log.error("Muitas tentativas de gerar slug único para base: {}. Usando UUID como fallback.", baseSuggestion);
                currentSlug = UUID.randomUUID().toString().substring(0, 12);
                if (memoryPageRepository.findBySlug(currentSlug).isPresent()) {
                    throw new IllegalStateException("Não foi possível gerar slug único após múltiplas tentativas e fallback.");
                }
                break;
            }
        }
        return currentSlug;
    }

    private String sanitizeSlug(String input) {
        if (input == null || input.isBlank()) { return ""; }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lowerCase = withoutAccents.toLowerCase();
        String replaced = NON_ALPHANUMERIC_OR_HYPHEN.matcher(lowerCase).replaceAll("-");
        String collapsedHyphens = HYPHEN_DUPLICATES.matcher(replaced).replaceAll("-");
        return collapsedHyphens.replaceAll("^-|-$", "");
    }
    @Transactional
    @Override
    public List<String> uploadAndAssociateImages(String slug, List<MultipartFile> files) throws IOException, IllegalArgumentException {
        MemoryPage memoryPage = memoryPageRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Memory page not found with slug: " + slug));

        List<String> existingImageUrls = memoryPage.getImageUrls();
        if (existingImageUrls == null) { existingImageUrls = new ArrayList<>(); }
        if (files == null || files.isEmpty()) { throw new IllegalArgumentException("Nenhum arquivo enviado."); }
        if (existingImageUrls.size() + files.size() > 7) { throw new IllegalArgumentException("Limite de 7 imagens excedido."); }

        List<String> savedPublicUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) { continue; }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectKey = "images/" + slug + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0,6) + extension;

            log.info("Fazendo upload para Supabase Storage. Bucket: '{}', Key: '{}'", supabaseBucketName, objectKey);

            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(supabaseBucketName)
                        .key(objectKey)
                        .contentType(file.getContentType())
                        .build();


                PutObjectResponse response = s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                if (response != null && response.sdkHttpResponse().isSuccessful()) {
                    String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8).replace("+", "%20");
                    String publicUrl = supabaseApiUrl + "/storage/v1/object/public/" + supabaseBucketName + "/" + encodedKey;
                    savedPublicUrls.add(publicUrl);
                    log.info("Upload com sucesso para Supabase. URL pública: {}", publicUrl);
                } else {
                    log.error("Falha no upload para Supabase S3 para o arquivo {}. Resposta: {}", objectKey, response);
                    throw new IOException("Falha no upload para Supabase S3 para o arquivo " + originalFilename);
                }

            } catch (Exception e) {
                log.error("Erro durante upload para Supabase S3 do arquivo '{}' para slug {}", objectKey, slug, e);
                throw new IOException("Falha ao fazer upload do arquivo: " + originalFilename, e);
            }
        }


        existingImageUrls.addAll(savedPublicUrls);
        memoryPage.setImageUrls(existingImageUrls);
        memoryPage.setSynced(false);
        memoryPageRepository.save(memoryPage);
        log.info("URLs de imagem (Supabase) atualizadas para slug {}: {}", slug, savedPublicUrls);

        return savedPublicUrls;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCodeForSlug(String slug) throws IOException, IllegalArgumentException {
        if (!memoryPageRepository.findBySlug(slug).isPresent()) {
            log.warn("Tentativa de gerar QR Code para slug não existente: {}", slug);
            throw new IllegalArgumentException("Memory page not found with slug: " + slug);
        }


        String pageUrl = appBaseUrl + "/m/" + slug;
        log.info("Gerando QR Code para a URL: {}", pageUrl);

        try (ByteArrayOutputStream stream = QRCode.from(pageUrl)
                .to(ImageType.PNG)
                .withSize(250, 250)
                .stream()) {
            return stream.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar stream de bytes do QR Code para URL: {}", pageUrl, e);
            throw new IOException("Erro ao gerar imagem QR Code", e);
        }
    }

}