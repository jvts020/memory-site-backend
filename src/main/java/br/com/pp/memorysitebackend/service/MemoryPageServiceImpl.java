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
// Garanta que está implementando a interface ATUALIZADA
public class MemoryPageServiceImpl implements MemoryPageService {

    private final MemoryPageRepository memoryPageRepository;
    private static final Logger log = LoggerFactory.getLogger(MemoryPageServiceImpl.class);

    private static final Pattern NON_ALPHANUMERIC_OR_HYPHEN = Pattern.compile("[^a-z0-9-]");
    private static final Pattern HYPHEN_DUPLICATES = Pattern.compile("-{2,}");

    // Injeta Cliente S3 configurado no SupabaseConfig
    private final S3Client s3Client;

    // Injeta novas propriedades do Supabase
    @Value("${supabase.api.url}")
    private String supabaseApiUrl; // Usado para construir a URL pública

    @Value("${supabase.bucket.name}")
    private String supabaseBucketName;

    @Value("${app.base-url}") // Diz ao Spring para buscar o valor de 'app.base-url' no application.properties
    private String appBaseUrl;
    @Override
    @Transactional
    // Assinatura atualizada para receber DTO de Request
    public MemoryPageResponse createMemoryPage(CreateMemoryPageRequest requestDto) {
        // Validação de regras de negócio (exemplo)
        if (requestDto.getImageUrls() != null && requestDto.getImageUrls().size() > 7) {
            throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
        }
        // Nota: Validação de formato (@NotBlank etc) ocorre antes, no Controller, devido ao @Valid

        MemoryPage memoryPage = mapToEntity(requestDto); // Mapeia DTO para Entidade

        // Lógica de Slug Melhorada
        String finalSlug;
        if (requestDto.getSuggestedSlug() != null && !requestDto.getSuggestedSlug().isBlank()) {
            finalSlug = generateUniqueSlug(requestDto.getSuggestedSlug());
        } else {
            String baseSlug = sanitizeSlug(requestDto.getDedicatedText().substring(0, Math.min(requestDto.getDedicatedText().length(), 30)));
            if (baseSlug.isEmpty()) { baseSlug = "memoria"; }
            finalSlug = generateUniqueSlug(baseSlug);
        }
        memoryPage.setSlug(finalSlug);

        // Estado inicial
        memoryPage.setId(null);
        memoryPage.setViewCount(0);
        memoryPage.setSynced(false); // Definindo o campo corrigido

        MemoryPage savedPage = memoryPageRepository.save(memoryPage);
        log.info("MemoryPage criada com slug: {}", savedPage.getSlug());
        return mapToDto(savedPage); // Retorna DTO de Resposta
    }

    @Override
    @Transactional // Assinatura atualizada para retornar Optional<DTO>
    public Optional<MemoryPageResponse> getMemoryPageBySlug(String slug) {
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            MemoryPage page = pageOptional.get();
            // Incremento de view (exemplo - pode ser otimizado)
            page.setViewCount(page.getViewCount() + 1);
            memoryPageRepository.save(page); // Salva contador - CUIDADO: remove o readOnly=true se fizer isso
            return Optional.of(mapToDto(page)); // Mapeia para DTO
        }
        return Optional.empty();
    }

    // ATENÇÃO: Se você incrementa o contador no GET, remova o (readOnly = true) acima
    // e adicione @Transactional sem readOnly. Fica assim:
    /*
    @Override
    @Transactional // Não é mais readOnly porque salvamos o contador
    public Optional<MemoryPageResponse> getMemoryPageBySlug(String slug) {
        Optional<MemoryPage> pageOptional = memoryPageRepository.findBySlug(slug);
        if (pageOptional.isPresent()) {
            MemoryPage page = pageOptional.get();
            page.setViewCount(page.getViewCount() + 1);
            MemoryPage updatedPage = memoryPageRepository.save(page); // Salva e pega a referência atualizada
            return Optional.of(mapToDto(updatedPage));
        }
        return Optional.empty();
    }
    */


    @Override
    @Transactional(readOnly = true) // Assinatura atualizada para retornar List<DTO>
    public List<MemoryPageResponse> getAllMemoryPages() {
        return memoryPageRepository.findAll()
                .stream()
                .map(this::mapToDto) // Usa o método de mapeamento
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    // Assinatura atualizada para receber DTO de Request e retornar Optional<DTO>
    public Optional<MemoryPageResponse> updateMemoryPage(String slug, CreateMemoryPageRequest updatedPageData) {
        Optional<MemoryPage> existingPageOptional = memoryPageRepository.findBySlug(slug);

        if (existingPageOptional.isPresent()) {
            MemoryPage existingPage = existingPageOptional.get();

            // Validação de regras de negócio (exemplo)
            if (updatedPageData.getImageUrls() != null && updatedPageData.getImageUrls().size() > 7) {
                throw new IllegalArgumentException("Máximo de 7 imagens permitido.");
            }

            // Atualiza campos com base no DTO
            // Nota: A validação de formato (@NotBlank etc) ocorreu no Controller com @Valid
            if (updatedPageData.getDedicatedText() != null) { // Verifica se veio no DTO
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
            existingPage.setSynced(false); // Marcar como não sincronizado

            MemoryPage savedPage = memoryPageRepository.save(existingPage);
            return Optional.of(mapToDto(savedPage)); // Retorna DTO atualizado
        } else {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    // Assinatura não muda, mas a lógica interna sim (se necessário)
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

    // --- Métodos auxiliares de Mapeamento e Slug ---

    // Mapeia DTO de criação para Entidade
    private MemoryPage mapToEntity(CreateMemoryPageRequest dto) {
        MemoryPage entity = new MemoryPage();
        // Mapeia campos do DTO para a entidade
        entity.setDedicatedText(dto.getDedicatedText());
        entity.setImageUrls(dto.getImageUrls() != null ? new ArrayList<>(dto.getImageUrls()) : new ArrayList<>());
        entity.setMusicUrl(dto.getMusicUrl());
        entity.setTargetDate(dto.getTargetDate());
        // id, slug, creationDate, viewCount, isSynced são gerados/definidos pelo serviço/JPA
        return entity;
    }

    // Mapeia Entidade para DTO de resposta
    private MemoryPageResponse mapToDto(MemoryPage entity) {
        // Cria um DTO com os dados da entidade
        return new MemoryPageResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getDedicatedText(),
                // Garante que a lista não seja nula no DTO, mesmo que seja no banco (improvável com ElementCollection)
                entity.getImageUrls() != null ? new ArrayList<>(entity.getImageUrls()) : new ArrayList<>(),
                entity.getMusicUrl(),
                entity.getTargetDate(),
                entity.getCreationDate(),
                entity.getViewCount()
        );
    }

    // Gera um slug único baseado em uma sugestão/base
    private String generateUniqueSlug(String baseSuggestion) {
        String currentSlug = sanitizeSlug(baseSuggestion);
        if (currentSlug.isEmpty()) { // Garante que não seja vazio após sanitizar
            currentSlug = UUID.randomUUID().toString().substring(0, 8); // Fallback se sanitização resultar em vazio
        }
        int attempt = 0;
        String originalSlug = currentSlug; // Guarda o slug base sanitizado

        // Loop para garantir unicidade
        while (memoryPageRepository.findBySlug(currentSlug).isPresent()) {
            attempt++;
            String suffix = "-" + attempt;
            // Garante que não exceda o tamanho máximo da coluna
            int maxBaseLength = 50 - suffix.length();
            // Recalcula a base a partir do original sanitizado para evitar encurtamento progressivo
            String base = originalSlug.substring(0, Math.min(originalSlug.length(), maxBaseLength));
            currentSlug = base + suffix;
            log.warn("Colisão de slug detectada para '{}'. Tentando '{}'", baseSuggestion, currentSlug);
            if (attempt > 10) {
                log.error("Muitas tentativas de gerar slug único para base: {}. Usando UUID como fallback.", baseSuggestion);
                currentSlug = UUID.randomUUID().toString().substring(0, 12);
                // Verifica uma última vez o fallback (extremamente improvável colidir)
                if (memoryPageRepository.findBySlug(currentSlug).isPresent()) {
                    throw new IllegalStateException("Não foi possível gerar slug único após múltiplas tentativas e fallback.");
                }
                break; // Sai do loop com o fallback
            }
        }
        return currentSlug;
    }

    // Limpa e formata uma string para ser usada como slug
    private String sanitizeSlug(String input) {
        if (input == null || input.isBlank()) { return ""; }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lowerCase = withoutAccents.toLowerCase();
        String replaced = NON_ALPHANUMERIC_OR_HYPHEN.matcher(lowerCase).replaceAll("-");
        String collapsedHyphens = HYPHEN_DUPLICATES.matcher(replaced).replaceAll("-");
        return collapsedHyphens.replaceAll("^-|-$", ""); // Remove hífens no início/fim
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
            // Gera chave única para o objeto no S3 (pode incluir path se quiser organizar)
            String objectKey = "images/" + slug + "_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0,6) + extension;

            log.info("Fazendo upload para Supabase Storage. Bucket: '{}', Key: '{}'", supabaseBucketName, objectKey);

            try {
                // Prepara a requisição de upload para o S3/Supabase
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(supabaseBucketName) // Nome do bucket
                        .key(objectKey)            // Chave (caminho/nome) do objeto no bucket
                        .contentType(file.getContentType()) // Tipo de conteúdo (ex: image/jpeg)
                        // .acl(ObjectCannedACL.PUBLIC_READ) // Se o bucket NÃO for público, pode precisar disso
                        .build();

                // Envia o arquivo
                PutObjectResponse response = s3Client.putObject(putObjectRequest,
                        RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                // Verifica se o upload foi bem-sucedido (opcional, mas bom)
                if (response != null && response.sdkHttpResponse().isSuccessful()) {
                    // Constrói a URL pública do objeto no Supabase Storage
                    // Formato: <Supabase_URL>/storage/v1/object/public/<Bucket_Name>/<Object_Key>
                    // Precisamos codificar o objectKey caso tenha caracteres especiais (embora nosso padrão evite)
                    String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8).replace("+", "%20");
                    String publicUrl = supabaseApiUrl + "/storage/v1/object/public/" + supabaseBucketName + "/" + encodedKey;
                    savedPublicUrls.add(publicUrl); // GUARDA A URL PÚBLICA
                    log.info("Upload com sucesso para Supabase. URL pública: {}", publicUrl);
                } else {
                    log.error("Falha no upload para Supabase S3 para o arquivo {}. Resposta: {}", objectKey, response);
                    throw new IOException("Falha no upload para Supabase S3 para o arquivo " + originalFilename);
                }

            } catch (Exception e) { // Captura exceções do SDK S3 também
                log.error("Erro durante upload para Supabase S3 do arquivo '{}' para slug {}", objectKey, slug, e);
                throw new IOException("Falha ao fazer upload do arquivo: " + originalFilename, e);
            }
        }

        // Atualiza a entidade com as URLs PÚBLICAS
        existingImageUrls.addAll(savedPublicUrls);
        memoryPage.setImageUrls(existingImageUrls);
        memoryPage.setSynced(false);
        memoryPageRepository.save(memoryPage);
        log.info("URLs de imagem (Supabase) atualizadas para slug {}: {}", slug, savedPublicUrls);

        return savedPublicUrls; // Retorna as URLs públicas
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCodeForSlug(String slug) throws IOException, IllegalArgumentException {
        if (!memoryPageRepository.findBySlug(slug).isPresent()) {
            log.warn("Tentativa de gerar QR Code para slug não existente: {}", slug);
            throw new IllegalArgumentException("Memory page not found with slug: " + slug);
        }


        String pageUrl = appBaseUrl + "/m/" + slug; // Use o campo injetado
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