package com.example.socialnetwork.domain.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResultDto {
    private Long id;
    private String type;
    private String title;
    private String subtitle;
}
