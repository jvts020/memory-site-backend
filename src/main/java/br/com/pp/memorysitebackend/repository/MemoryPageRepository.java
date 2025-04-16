package br.com.pp.memorysitebackend.repository; // Ajuste seu pacote

import br.com.pp.memorysitebackend.entity.MemoryPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marca esta interface como um componente Spring (gerenciador de repositório)
public interface MemoryPageRepository extends JpaRepository<MemoryPage, Long> { // <Tipo da Entidade, Tipo do ID>

    // Spring Data JPA criará a implementação automaticamente!
    // Exemplo de método de busca customizado (Spring Data infere a query pelo nome)
    Optional<MemoryPage> findBySlug(String slug);

}