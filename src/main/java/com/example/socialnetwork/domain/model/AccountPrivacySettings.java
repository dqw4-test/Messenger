package com.example.socialnetwork.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_privacy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountPrivacySettings {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_policy", nullable = false)
    private PrivacyPolicyMode messagePolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "info_policy", nullable = false)
    private PrivacyPolicyMode infoPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_policy", nullable = false)
    private PrivacyPolicyMode invitePolicy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (messagePolicy == null) {
            messagePolicy = PrivacyPolicyMode.ALL_EXCEPT_SELECTED;
        }
        if (infoPolicy == null) {
            infoPolicy = PrivacyPolicyMode.ALL_EXCEPT_SELECTED;
        }
        if (invitePolicy == null) {
            invitePolicy = PrivacyPolicyMode.ALL_EXCEPT_SELECTED;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
