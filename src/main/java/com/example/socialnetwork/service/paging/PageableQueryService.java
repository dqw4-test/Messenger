package com.example.socialnetwork.service.paging;

import com.example.socialnetwork.domain.dto.paging.OffsetPageQuery;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PageableQueryService {
    public OffsetPageQuery resolve(Integer limit, Integer offset, Integer page) {
        int resolvedLimit = limit == null ? 20 : limit;
        if (resolvedLimit <= 0 || resolvedLimit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }

        int resolvedOffset = offset == null ? 0 : offset;
        int resolvedPage = page == null ? 1 : page;
        if (resolvedOffset < 0 || resolvedPage <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offset must be non-negative and page must be positive");
        }

        if (offset == null && page != null) {
            resolvedOffset = (resolvedPage - 1) * resolvedLimit;
        }

        if (page == null && offset != null) {
            resolvedPage = (resolvedOffset / resolvedLimit) + 1;
        }

        return new OffsetPageQuery(resolvedLimit, resolvedOffset, resolvedPage);
    }

    public <T> OffsetPageResponse<T> build(List<T> source, OffsetPageQuery query) {
        int from = Math.min(query.getOffset(), source.size());
        int to = Math.min(from + query.getLimit(), source.size());
        List<T> items = from >= to ? Collections.emptyList() : source.subList(from, to);
        return new OffsetPageResponse<>(
                items,
                query.getLimit(),
                query.getOffset(),
                query.getPage(),
                source.size(),
                to < source.size(),
                from > 0
        );
    }
}
