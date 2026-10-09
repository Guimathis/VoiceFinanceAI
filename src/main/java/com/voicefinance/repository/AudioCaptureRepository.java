package com.voicefinance.repository;

import com.voicefinance.domain.entity.AudioCapture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudioCaptureRepository extends JpaRepository<AudioCapture, UUID> {

    Optional<AudioCapture> findByIdAndUserId(UUID id, UUID userId);

    List<AudioCapture> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
