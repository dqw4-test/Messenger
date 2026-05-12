package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.domain.dto.search.SearchResultDto;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.search.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Global search")
public class SearchController {
    private final SearchService searchService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/users")
    public OffsetPageResponse<SearchResultDto> users(@RequestParam String query,
                                                     @RequestParam(required = false) Integer limit,
                                                     @RequestParam(required = false) Integer offset,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) String matchMode,
                                                     @RequestParam(required = false) Integer ageFrom,
                                                     @RequestParam(required = false) Integer ageTo,
                                                     @RequestParam(required = false) String birthDate,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(required = false) String gender) {
        return searchService.searchUsers(currentUserProvider.getCurrentUserId(), query, limit, offset, page, matchMode, ageFrom, ageTo, birthDate, city, gender);
    }

    @GetMapping("/groups")
    public OffsetPageResponse<SearchResultDto> groups(@RequestParam String query,
                                                      @RequestParam(required = false) Integer limit,
                                                      @RequestParam(required = false) Integer offset,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) String matchMode) {
        return searchService.searchGroups(currentUserProvider.getCurrentUserId(), query, limit, offset, page, matchMode);
    }
}
