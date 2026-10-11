package org.example.pokerproto.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Tournament;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TvService {

    private final TournamentService tournamentService;

    @Data
    public static class TvState {
        private Long tournamentId;
        private String tournamentTitle = "SOVA POKER TOURNAMENT";
        private String excelUrl = "";
        private boolean isRunning = false;
        private int currentLevel = 1;
        private int secondsLeft = 15 * 60;
        private int totalSeconds = 15 * 60;
        private boolean isBreak = false;
        private long lastSyncTimestamp = System.currentTimeMillis();
        private Integer smallBlind = 100;
        private Integer bigBlind = 200;
        private Integer ante = 200;
    }

    private final TvState currentState = new TvState();

    public synchronized TvState getState() {
        if (currentState.isRunning()) {
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - currentState.getLastSyncTimestamp()) / 1000;
            if (elapsedSeconds > 0) {
                int newSeconds = (int) Math.max(0, currentState.getSecondsLeft() - elapsedSeconds);
                currentState.setSecondsLeft(newSeconds);
                currentState.setLastSyncTimestamp(now);
                if (newSeconds == 0) {
                    currentState.setRunning(false);
                }
            }
        }
        return copyState();
    }

    public synchronized TvState setTournament(Long tournamentId) {
        Optional<Tournament> opt = tournamentService.findById(tournamentId);
        if (opt.isPresent()) {
            Tournament t = opt.get();
            currentState.setTournamentId(t.getId());
            currentState.setTournamentTitle(t.getTitle());
            currentState.setExcelUrl(t.getExcelUrl() != null ? t.getExcelUrl() : "");
        }
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState startTimer() {
        if (!currentState.isRunning() && currentState.getSecondsLeft() > 0) {
            currentState.setRunning(true);
            currentState.setLastSyncTimestamp(System.currentTimeMillis());
        }
        return copyState();
    }

    public synchronized TvState pauseTimer() {
        if (currentState.isRunning()) {
            getState();
            currentState.setRunning(false);
            currentState.setLastSyncTimestamp(System.currentTimeMillis());
        }
        return copyState();
    }

    public synchronized TvState nextLevel(int seconds) {
        currentState.setCurrentLevel(currentState.getCurrentLevel() + 1);
        currentState.setSecondsLeft(seconds > 0 ? seconds : 15 * 60);
        currentState.setTotalSeconds(seconds > 0 ? seconds : 15 * 60);
        currentState.setBreak(false);
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState prevLevel(int seconds) {
        if (currentState.getCurrentLevel() > 1) {
            currentState.setCurrentLevel(currentState.getCurrentLevel() - 1);
        }
        currentState.setSecondsLeft(seconds > 0 ? seconds : 15 * 60);
        currentState.setTotalSeconds(seconds > 0 ? seconds : 15 * 60);
        currentState.setBreak(false);
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState toggleBreak(int seconds) {
        boolean nextBreak = !currentState.isBreak();
        currentState.setBreak(nextBreak);
        if (nextBreak) {
            int breakSecs = seconds > 0 ? seconds : 15 * 60;
            currentState.setSecondsLeft(breakSecs);
            currentState.setTotalSeconds(breakSecs);
        }
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState adjustTime(int deltaSeconds) {
        getState();
        currentState.setSecondsLeft(Math.max(0, currentState.getSecondsLeft() + deltaSeconds));
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState syncCustom(Integer level, Integer secondsLeft, Boolean isBreak, Boolean isRunning) {
        if (level != null) currentState.setCurrentLevel(level);
        if (secondsLeft != null) {
            currentState.setSecondsLeft(Math.max(0, secondsLeft));
            currentState.setTotalSeconds(Math.max(currentState.getTotalSeconds(), secondsLeft));
        }
        if (isBreak != null) currentState.setBreak(isBreak);
        if (isRunning != null) currentState.setRunning(isRunning);
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState setBlinds(Integer bigBlind, Integer smallBlind, Integer ante) {
        if (bigBlind != null && bigBlind > 0) {
            currentState.setBigBlind(bigBlind);
            currentState.setSmallBlind(smallBlind != null && smallBlind > 0 ? smallBlind : Math.max(1, bigBlind / 2));
            currentState.setAnte(ante != null && ante >= 0 ? ante : bigBlind);
            currentState.setLastSyncTimestamp(System.currentTimeMillis());
        }
        return copyState();
    }

    private TvState copyState() {
        TvState copy = new TvState();
        copy.setTournamentId(currentState.getTournamentId());
        copy.setTournamentTitle(currentState.getTournamentTitle());
        copy.setExcelUrl(currentState.getExcelUrl());
        copy.setRunning(currentState.isRunning());
        copy.setCurrentLevel(currentState.getCurrentLevel());
        copy.setSecondsLeft(currentState.getSecondsLeft());
        copy.setTotalSeconds(currentState.getTotalSeconds());
        copy.setBreak(currentState.isBreak());
        copy.setLastSyncTimestamp(currentState.getLastSyncTimestamp());
        copy.setSmallBlind(currentState.getSmallBlind());
        copy.setBigBlind(currentState.getBigBlind());
        copy.setAnte(currentState.getAnte());
        return copy;
    }
}
