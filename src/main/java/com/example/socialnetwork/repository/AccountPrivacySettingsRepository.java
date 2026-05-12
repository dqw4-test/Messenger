package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.AccountPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPrivacySettingsRepository extends JpaRepository<AccountPrivacySettings, Long> {
}
