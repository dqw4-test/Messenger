package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.GroupJoinRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    Optional<GroupJoinRequest> findByGroupIdAndApplicantId(Long groupId, Long applicantId);

    List<GroupJoinRequest> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    void deleteByGroupIdAndApplicantId(Long groupId, Long applicantId);

    void deleteByGroupId(Long groupId);
}
