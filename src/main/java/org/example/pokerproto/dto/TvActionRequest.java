package org.example.pokerproto.dto;

public record TvActionRequest(
        String action,
        Long tournamentId,
        Integer deltaSeconds,
        Integer level,
        Integer secondsLeft,
        Boolean isBreak,
        Integer bigBlind,
        Integer smallBlind,
        Integer ante
) {}
