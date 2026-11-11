package org.example.pokerproto.dto;

import java.time.Instant;

public record ParticipantDto(
        Long tgId,
        String username,
        String firstName,
        String lastName,
        String status,
        Instant registeredAt,
        String nickname
) {}
