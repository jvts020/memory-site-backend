package br.com.pp.memorysitebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error; // Ex: "Bad Request", "Not Found"
    private String message; // Mensagem geral do erro
    private String path;    // URL que causou o erro

    // Opcional: para erros de validação
    private Map<String, String> validationErrors;

    // Construtor simplificado para erros comuns
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}