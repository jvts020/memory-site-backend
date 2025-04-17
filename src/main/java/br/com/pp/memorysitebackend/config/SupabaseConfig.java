package br.com.pp.memorysitebackend.config; // Seu pacote

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
// Remova S3Configuration se não for usado
// import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class SupabaseConfig {

    // Mantenha esta - pode ser útil depois para montar URLs públicas completas
    @Value("${supabase.api.url}")
    private String supabaseUrl;

    // REMOVA a injeção da service_key (não usada para S3)
    // @Value("${supabase.service.key}")
    // private String supabaseServiceKey;

    // INJETA as NOVAS propriedades para as chaves S3 dedicadas
    @Value("${supabase.s3.access-key-id}")
    private String supabaseS3AccessKeyId;

    @Value("${supabase.s.secret-access-key}")
    private String supabaseS3SecretAccessKey;

    // Injeta a região
    @Value("${supabase.region}")
    private String supabaseRegion;

    // Logger para ajudar no debug
    private static final Logger log = LoggerFactory.getLogger(SupabaseConfig.class);

    @Bean
    public S3Client supabaseS3Client() {

        // Endpoint S3 CORRETO (da sua imagem das configurações de Storage)
        String supabaseS3Endpoint = "https://nhisovxouqpxhyakidug.supabase.co/storage/v1/s3";

        log.info("Configurando S3 Client. Endpoint: {}, Região: {}", supabaseS3Endpoint, supabaseRegion);
        // Log para verificar se as chaves estão sendo lidas (NÃO logue a secret key!)
        log.info("Usando S3 Access Key ID: {}", supabaseS3AccessKeyId != null && !supabaseS3AccessKeyId.isEmpty() ? "Presente (***)" : "AUSENTE!");

        // Configura credenciais: USANDO AS CHAVES S3 DEDICADAS
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                // Usa as propriedades injetadas com as chaves S3 corretas
                AwsBasicCredentials.create(supabaseS3AccessKeyId, supabaseS3SecretAccessKey)
        );

        // Cria e retorna o cliente S3 configurado
        return S3Client.builder()
                .region(Region.of(supabaseRegion))                 // Define a região
                .endpointOverride(URI.create(supabaseS3Endpoint)) // <<<--- USA O ENDPOINT CORRETO
                .credentialsProvider(credentialsProvider)         // <<<--- USA AS CREDENCIAIS S3 CORRETAS
                .forcePathStyle(true)                             // Manter para compatibilidade
                .build();
    }
}