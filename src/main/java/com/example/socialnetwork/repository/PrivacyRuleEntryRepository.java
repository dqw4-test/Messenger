package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.PrivacyRuleEntry;
import com.example.socialnetwork.domain.model.PrivacyType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyRuleEntryRepository extends JpaRepository<PrivacyRuleEntry, Long> {
    List<PrivacyRuleEntry> findByUserIdAndPrivacyType(Long userId, PrivacyType privacyType);

    void deleteByUserIdAndPrivacyType(Long userId, PrivacyType privacyType);
}
