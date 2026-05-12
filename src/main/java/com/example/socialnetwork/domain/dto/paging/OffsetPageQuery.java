package com.example.socialnetwork.domain.dto.paging;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OffsetPageQuery {
    private final int limit;
    private final int offset;
    private final int page;
}
