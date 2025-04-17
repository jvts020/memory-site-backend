package br.com.pp.memorysitebackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;



import java.net.URI;

@Configuration
public class SupabaseConfig {

    @Value("${supabase.api.url}")
    private String supabaseUrl;



    @Value("${supabase.s3.access-key-id}")
    private String supabaseS3AccessKeyId;

    @Value("${supabase.s.secret-access-key}")
    private String supabaseS3SecretAccessKey;

    @Value("${supabase.region}")
    private String supabaseRegion;

    private static final Logger log = LoggerFactory.getLogger(SupabaseConfig.class);

    @Bean
    public S3Client supabaseS3Client() {

        String supabaseS3Endpoint = "https://nhisovxouqpxhyakidug.supabase.co/storage/v1/s3";

        log.info("Configurando S3 Client. Endpoint: {}, Região: {}", supabaseS3Endpoint, supabaseRegion);
        log.info("Usando S3 Access Key ID: {}", supabaseS3AccessKeyId != null && !supabaseS3AccessKeyId.isEmpty() ? "Presente (***)" : "AUSENTE!");

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(supabaseS3AccessKeyId, supabaseS3SecretAccessKey)
        );

        return S3Client.builder()
                .region(Region.of(supabaseRegion))
                .endpointOverride(URI.create(supabaseS3Endpoint))
                .credentialsProvider(credentialsProvider)
                .forcePathStyle(true)
                .build();
    }
}