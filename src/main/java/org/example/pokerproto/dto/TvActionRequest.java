package org.example.pokerproto.dto;

import org.example.pokerproto.service.TvService;
import java.util.List;

public record TvActionRequest(
        String action,
        Long tournamentId,
        Integer deltaSeconds,
        Integer level,
        Integer secondsLeft,
        Boolean isBreak,
        Integer bigBlind,
        Integer smallBlind,
        Integer ante,
        List<TvService.BlindLevel> structure
) {}
