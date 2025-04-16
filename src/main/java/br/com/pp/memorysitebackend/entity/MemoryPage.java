package br.com.pp.memorysitebackend.entity; // Ajuste seu pacote

import jakarta.persistence.*; // Pacote para anotações JPA (pode ser javax.persistence em versões mais antigas do Spring Boot)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp; // Para data de criação automática

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // Marca esta classe como uma entidade JPA (mapeada para uma tabela)
@Table(name = "memory_pages") // Define o nome da tabela no banco (opcional, mas boa prática)
@Data // Lombok: Gera getters, setters, toString, equals, hashCode automaticamente
@NoArgsConstructor // Lombok: Gera construtor sem argumentos
@AllArgsConstructor // Lombok: Gera construtor com todos os argumentos
public class MemoryPage {

    @Id // Marca este campo como a chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Define que o ID será gerado automaticamente pelo banco (autoincremento)
    private Long id;

    @Column(nullable = false, unique = true, length = 50) // Coluna não nula, única e com tamanho máximo
    private String slug; // Identificador único para a URL personalizada (ex: "aniversario-ana-2025")

    @Lob // Indica que o campo pode armazenar textos longos
    @Column(columnDefinition = "TEXT") // Define o tipo da coluna no banco como TEXT (varia entre bancos)
    private String dedicatedText; // O texto dedicado

    // Para armazenar a lista de URLs das imagens (máximo 7)
    @ElementCollection(fetch = FetchType.EAGER) // Mapeia uma coleção de elementos básicos (Strings)
    @CollectionTable(name = "memory_page_image_urls", joinColumns = @JoinColumn(name = "memory_page_id")) // Tabela separada para as URLs
    @OrderColumn // Mantém a ordem das imagens na lista
    @Column(name = "image_url", length = 512) // Nome da coluna na tabela de URLs e tamanho
    private List<String> imageUrls = new ArrayList<>(); // Inicializa a lista

    @Column(length = 512) // Tamanho máximo para URL da música
    private String musicUrl; // URL ou caminho para a música

    private LocalDateTime targetDate; // Data do evento ou alvo do contador

    @CreationTimestamp // Define que o Hibernate preencherá com a data/hora atual na criação
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate; // Data de criação do registro
    private boolean isSynced = false;
    // Campos para o contador (exemplo simples: contador de visualizações)
    private long viewCount = 0;

    // Adicionaremos validação para o limite de 7 imagens na camada de serviço depois.
}