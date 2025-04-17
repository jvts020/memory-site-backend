package br.com.pp.memorysitebackend.repository;

import br.com.pp.memorysitebackend.entity.MemoryPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemoryPageRepository extends JpaRepository<MemoryPage, Long> {

    Optional<MemoryPage> findBySlug(String slug);

}