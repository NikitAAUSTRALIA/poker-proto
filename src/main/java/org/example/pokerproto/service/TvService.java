package org.example.pokerproto.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Tournament;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TvService {

    private final TournamentService tournamentService;

    public record BlindLevel(int level, int sb, int bb, int ante, int minutes, boolean isBreak) {}

    public static final List<BlindLevel> DEFAULT_STRUCTURE = List.of(
            new BlindLevel(1, 100, 200, 200, 20, false),
            new BlindLevel(2, 200, 400, 400, 20, false),
            new BlindLevel(3, 300, 600, 600, 20, false),
            new BlindLevel(4, 400, 800, 800, 20, false),
            new BlindLevel(5, 500, 1000, 1000, 20, false),
            new BlindLevel(6, 0, 0, 0, 15, true),
            new BlindLevel(7, 600, 1200, 1200, 20, false),
            new BlindLevel(8, 800, 1600, 1600, 20, false),
            new BlindLevel(9, 1000, 2000, 2000, 20, false),
            new BlindLevel(10, 1200, 2400, 2400, 20, false),
            new BlindLevel(11, 1500, 3000, 3000, 20, false),
            new BlindLevel(12, 0, 0, 0, 15, true),
            new BlindLevel(13, 2000, 4000, 4000, 20, false),
            new BlindLevel(14, 2500, 5000, 5000, 20, false),
            new BlindLevel(15, 3000, 6000, 6000, 20, false),
            new BlindLevel(16, 4000, 8000, 8000, 20, false),
            new BlindLevel(17, 5000, 10000, 10000, 20, false),
            new BlindLevel(18, 0, 0, 0, 15, true),
            new BlindLevel(19, 6000, 12000, 12000, 20, false),
            new BlindLevel(20, 8000, 16000, 16000, 20, false),
            new BlindLevel(21, 10000, 20000, 20000, 20, false),
            new BlindLevel(22, 15000, 30000, 30000, 20, false),
            new BlindLevel(23, 20000, 40000, 40000, 20, false),
            new BlindLevel(24, 25000, 50000, 50000, 20, false),
            new BlindLevel(25, 30000, 60000, 60000, 20, false),
            new BlindLevel(26, 40000, 80000, 80000, 20, false),
            new BlindLevel(27, 50000, 100000, 100000, 20, false)
    );

    @Data
    public static class TvState {
        private Long tournamentId;
        private String tournamentTitle = "SOVA POKER TOURNAMENT";
        private String excelUrl = "";
        private boolean isRunning = false;
        private int currentLevel = 1;
        private int secondsLeft = 20 * 60;
        private int totalSeconds = 20 * 60;
        private boolean isBreak = false;
        private long lastSyncTimestamp = System.currentTimeMillis();
        private Integer smallBlind = 100;
        private Integer bigBlind = 200;
        private Integer ante = 200;
    }

    private final TvState currentState = new TvState();

    private void applyLevel(int levelNum) {
        int idx = Math.max(0, Math.min(levelNum - 1, DEFAULT_STRUCTURE.size() - 1));
        BlindLevel bl = DEFAULT_STRUCTURE.get(idx);
        currentState.setCurrentLevel(levelNum);
        currentState.setBreak(bl.isBreak());
        if (!bl.isBreak()) {
            currentState.setSmallBlind(bl.sb());
            currentState.setBigBlind(bl.bb());
            currentState.setAnte(bl.ante());
        }
        int secs = bl.minutes() * 60;
        currentState.setSecondsLeft(secs);
        currentState.setTotalSeconds(secs);
    }

    public synchronized TvState getState() {
        if (currentState.isRunning()) {
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - currentState.getLastSyncTimestamp()) / 1000;
            if (elapsedSeconds > 0) {
                int newSeconds = (int) Math.max(0, currentState.getSecondsLeft() - elapsedSeconds);
                currentState.setSecondsLeft(newSeconds);
                currentState.setLastSyncTimestamp(now);
                if (newSeconds == 0) {
                    if (currentState.getCurrentLevel() < DEFAULT_STRUCTURE.size()) {
                        nextLevel(0);
                    } else {
                        currentState.setRunning(false);
                    }
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
        if (currentState.getCurrentLevel() < DEFAULT_STRUCTURE.size()) {
            applyLevel(currentState.getCurrentLevel() + 1);
            if (seconds > 0) {
                currentState.setSecondsLeft(seconds);
                currentState.setTotalSeconds(seconds);
            }
        }
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
    }

    public synchronized TvState prevLevel(int seconds) {
        if (currentState.getCurrentLevel() > 1) {
            applyLevel(currentState.getCurrentLevel() - 1);
            if (seconds > 0) {
                currentState.setSecondsLeft(seconds);
                currentState.setTotalSeconds(seconds);
            }
        }
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
        } else {
            int lvlSecs = seconds > 0 ? seconds : 20 * 60;
            currentState.setSecondsLeft(lvlSecs);
            currentState.setTotalSeconds(lvlSecs);
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
        if (level != null && level > 0) {
            applyLevel(level);
        }
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
