package com.example.socialnetwork.domain.dto.paging;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OffsetPageResponse<T> {
    private List<T> items;
    private int limit;
    private int offset;
    private int page;
    private long total;
    private boolean hasNext;
    private boolean hasPrevious;
}
