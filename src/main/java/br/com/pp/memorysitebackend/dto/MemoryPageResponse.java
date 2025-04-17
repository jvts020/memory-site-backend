package br.com.pp.memorysitebackend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryPageResponse {

    private Long id;
    private String slug;
    private String dedicatedText;
    private List<String> imageUrls;
    private String musicUrl;
    private LocalDateTime targetDate;
    private LocalDateTime creationDate;
    private long viewCount;

}