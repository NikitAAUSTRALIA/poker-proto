package org.example.pokerproto.dto;

import java.time.Instant;

public record SeasonDto(
        Long id,
        String name,
        Instant startsAt,
        Instant endsAt,
        boolean active
) {}
