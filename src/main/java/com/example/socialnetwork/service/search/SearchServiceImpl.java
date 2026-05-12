package com.example.socialnetwork.service.search;

import com.example.socialnetwork.domain.dto.paging.OffsetPageQuery;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.domain.dto.search.SearchResultDto;
import com.example.socialnetwork.domain.model.UserGender;
import com.example.socialnetwork.repository.GroupChatRepository;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.service.paging.PageableQueryService;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final UserRepository userRepository;
    private final GroupChatRepository groupChatRepository;
    private final PageableQueryService pageableQueryService;

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<SearchResultDto> searchUsers(Long currentUserId,
                                                           String query,
                                                           Integer limit,
                                                           Integer offset,
                                                           Integer page,
                                                           String matchMode,
                                                           Integer ageFrom,
                                                           Integer ageTo,
                                                           String birthDate,
                                                           String city,
                                                           String gender) {
        String normalized = query == null ? "" : query.trim();
        String mode = normalizeMatchMode(matchMode);
        LocalDate exactBirthDate = parseBirthDate(birthDate);
        validateUserFilters(ageFrom, ageTo, birthDate, exactBirthDate);
        String cityNormalized = city == null ? null : city.trim().toLowerCase(Locale.ROOT);
        UserGender genderFilter = parseGender(gender);
        List<SearchResultDto> results = userRepository.searchByProfile(normalized).stream()
                .filter(user -> matches(mode, normalized, (user.getFirstName() + " " + user.getLastName()).trim(), String.valueOf(user.getId())))
                .filter(user -> matchesAge(user.getBirthDate(), ageFrom, ageTo))
                .filter(user -> exactBirthDate == null || exactBirthDate.equals(user.getBirthDate()))
                .filter(user -> cityNormalized == null || cityNormalized.isBlank()
                        || (user.getCity() != null && user.getCity().trim().toLowerCase(Locale.ROOT).contains(cityNormalized)))
                .filter(user -> genderFilter == null || genderFilter.equals(user.getGender()))
                .map(user -> new SearchResultDto(
                        user.getId(),
                        "user",
                        (user.getFirstName() + " " + user.getLastName()).trim(),
                        "User ID: " + user.getId()
                ))
                .toList();
        OffsetPageQuery pageQuery = pageableQueryService.resolve(limit, offset, page);
        return pageableQueryService.build(results, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<SearchResultDto> searchGroups(Long currentUserId,
                                                            String query,
                                                            Integer limit,
                                                            Integer offset,
                                                            Integer page,
                                                            String matchMode) {
        String normalized = query == null ? "" : query.trim();
        String mode = normalizeMatchMode(matchMode);
        List<SearchResultDto> results = groupChatRepository.searchByTitle(normalized).stream()
                .filter(group -> matches(mode, normalized, group.getTitle(), String.valueOf(group.getId())))
                .map(group -> new SearchResultDto(group.getId(), "group", group.getTitle(), "Group ID: " + group.getId()))
                .toList();
        OffsetPageQuery pageQuery = pageableQueryService.resolve(limit, offset, page);
        return pageableQueryService.build(results, pageQuery);
    }

    private String normalizeMatchMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "contains";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if ("contains".equals(normalized) || "prefix".equals(normalized) || "exact".equals(normalized)) {
            return normalized;
        }
        return "contains";
    }

    private boolean matches(String mode, String query, String... candidates) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        for (String candidateRaw : candidates) {
            String candidate = candidateRaw == null ? "" : candidateRaw.toLowerCase(Locale.ROOT);
            boolean ok = switch (mode) {
                case "exact" -> candidate.equals(needle);
                case "prefix" -> candidate.startsWith(needle);
                default -> candidate.contains(needle);
            };
            if (ok) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAge(LocalDate birthDate, Integer ageFrom, Integer ageTo) {
        if (ageFrom == null && ageTo == null) {
            return true;
        }
        if (birthDate == null) {
            return false;
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (ageFrom != null && age < ageFrom) {
            return false;
        }
        if (ageTo != null && age > ageTo) {
            return false;
        }
        return true;
    }

    private LocalDate parseBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthDate.trim());
        } catch (Exception ignore) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDate must be a valid ISO date (YYYY-MM-DD)");
        }
    }

    private UserGender parseGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        try {
            return UserGender.valueOf(gender.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignore) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender must be one of: MALE, FEMALE, OTHER");
        }
    }

    private void validateUserFilters(Integer ageFrom, Integer ageTo, String birthDateRaw, LocalDate exactBirthDate) {
        if (ageFrom != null && ageFrom < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ageFrom must be >= 0");
        }
        if (ageTo != null && ageTo < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ageTo must be >= 0");
        }
        if (ageFrom != null && ageTo != null && ageFrom > ageTo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ageFrom must be <= ageTo");
        }
        if (birthDateRaw != null && !birthDateRaw.isBlank() && exactBirthDate != null && (ageFrom != null || ageTo != null)) {
            int exactAge = Period.between(exactBirthDate, LocalDate.now()).getYears();
            if (ageFrom != null && exactAge < ageFrom) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDate conflicts with ageFrom");
            }
            if (ageTo != null && exactAge > ageTo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDate conflicts with ageTo");
            }
        }
    }
}
