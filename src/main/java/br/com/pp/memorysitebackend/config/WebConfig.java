package br.com.pp.memorysitebackend.config; // Seu pacote

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // URL do seu frontend em desenvolvimento
    // TODO: Adicionar a URL do frontend em produção aqui depois
    private static final String FRONTEND_DEV_URL = "https://memory-site-frontend.onrender.com";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Aplica esta configuração a todos os endpoints que começam com /api/
                .allowedOrigins(FRONTEND_DEV_URL) // Permite requisições SOMENTE desta origem
                // .allowedOrigins("*") // NUNCA use "*" em produção por segurança!
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos HTTP permitidos
                .allowedHeaders("*") // Permite todos os cabeçalhos na requisição (Content-Type, Authorization, etc)
                .allowCredentials(false); // Mantenha 'false' a menos que precise lidar com cookies/sessão cross-origin
        // .maxAge(3600); // Opcional: Tempo em segundos que o navegador pode cachear a resposta do OPTIONS
    }
}