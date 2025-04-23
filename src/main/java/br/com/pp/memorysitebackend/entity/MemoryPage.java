package br.com.pp.memorysitebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "memory_pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String dedicatedText;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "memory_page_image_urls", joinColumns = @JoinColumn(name = "memory_page_id"))
    @OrderColumn
    @Column(name = "image_url", length = 512)
    private List<String> imageUrls = new ArrayList<>();

    @Column(length = 512)
    private String musicUrl;

    private LocalDateTime targetDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;
    private boolean isSynced = false;
    private long viewCount = 0;

}