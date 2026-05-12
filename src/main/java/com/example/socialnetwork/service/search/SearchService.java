package com.example.socialnetwork.service.search;

import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.domain.dto.search.SearchResultDto;

public interface SearchService {
    OffsetPageResponse<SearchResultDto> searchUsers(Long currentUserId,
                                                    String query,
                                                    Integer limit,
                                                    Integer offset,
                                                    Integer page,
                                                    String matchMode,
                                                    Integer ageFrom,
                                                    Integer ageTo,
                                                    String birthDate,
                                                    String city,
                                                    String gender);

    OffsetPageResponse<SearchResultDto> searchGroups(Long currentUserId,
                                                     String query,
                                                     Integer limit,
                                                     Integer offset,
                                                     Integer page,
                                                     String matchMode);
}
