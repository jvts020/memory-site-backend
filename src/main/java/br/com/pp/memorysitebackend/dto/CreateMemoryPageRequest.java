package br.com.pp.memorysitebackend.dto;

import jakarta.validation.constraints.NotBlank; // Para validação
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent; // Exemplo para data
import lombok.Data;
import org.hibernate.validator.constraints.URL; // Validação de URL

import java.time.LocalDateTime;
import java.util.List;

@Data // Lombok
public class CreateMemoryPageRequest {

    @NotBlank(message = "Texto dedicado não pode ser vazio.")
    private String dedicatedText;

    // Valida que a lista não tenha mais que 7 itens
    @Size(max = 7, message = "Máximo de 7 imagens permitido.")
    private List<String> imageUrls; // URLs precisam ser válidas? Podemos adicionar @URL em cada uma se necessário

    @URL(message = "URL da música inválida.") // Valida formato da URL (opcional)
    private String musicUrl; // Pode ser nulo

    @PastOrPresent(message = "Data alvo não pode ser no futuro.") // Exemplo, ajuste conforme sua regra
    private LocalDateTime targetDate; // Pode ser nulo

    // Opcional: permitir que o usuário sugira um slug
    @Size(max = 50, message = "Slug sugerido muito longo.")
    // Adicionar validação de caracteres permitidos se desejar (ex: @Pattern)
    private String suggestedSlug;
}