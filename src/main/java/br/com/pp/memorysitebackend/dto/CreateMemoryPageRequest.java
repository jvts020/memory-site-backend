package br.com.pp.memorysitebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateMemoryPageRequest {

    @NotBlank(message = "Texto dedicado não pode ser vazio.")
    private String dedicatedText;

    @Size(max = 7, message = "Máximo de 7 imagens permitido.")
    private List<String> imageUrls;

    @URL(message = "URL da música inválida.")
    private String musicUrl;

    @PastOrPresent(message = "Data alvo não pode ser no futuro.")
    private LocalDateTime targetDate;


    @Size(max = 50, message = "Slug sugerido muito longo.")

    private String suggestedSlug;
}