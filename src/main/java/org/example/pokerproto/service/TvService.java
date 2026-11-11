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
            new BlindLevel(1, 100, 100, 100, 15, false),
            new BlindLevel(2, 100, 200, 200, 15, false),
            new BlindLevel(3, 150, 300, 300, 15, false),
            new BlindLevel(4, 200, 400, 400, 15, false),
            new BlindLevel(5, 0, 0, 0, 5, true),
            new BlindLevel(6, 300, 600, 600, 15, false),
            new BlindLevel(7, 400, 800, 800, 15, false),
            new BlindLevel(8, 500, 1000, 1000, 15, false),
            new BlindLevel(9, 600, 1200, 1200, 15, false),
            new BlindLevel(10, 0, 0, 0, 5, true),
            new BlindLevel(11, 800, 1600, 1600, 15, false),
            new BlindLevel(12, 1000, 2000, 2000, 15, false),
            new BlindLevel(13, 1200, 2400, 2400, 15, false),
            new BlindLevel(14, 1500, 3000, 3000, 15, false),
            new BlindLevel(15, 0, 0, 0, 15, true),
            new BlindLevel(16, 2000, 4000, 4000, 15, false),
            new BlindLevel(17, 3000, 6000, 6000, 15, false),
            new BlindLevel(18, 4000, 8000, 8000, 15, false),
            new BlindLevel(19, 5000, 10000, 10000, 15, false),
            new BlindLevel(20, 0, 0, 0, 5, true),
            new BlindLevel(21, 6000, 12000, 12000, 15, false),
            new BlindLevel(22, 8000, 16000, 16000, 15, false),
            new BlindLevel(23, 10000, 20000, 20000, 15, false),
            new BlindLevel(24, 15000, 30000, 30000, 15, false),
            new BlindLevel(25, 0, 0, 0, 5, true),
            new BlindLevel(26, 20000, 40000, 40000, 15, false),
            new BlindLevel(27, 30000, 60000, 60000, 15, false),
            new BlindLevel(28, 40000, 80000, 80000, 15, false),
            new BlindLevel(29, 50000, 100000, 100000, 15, false)
    );

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
        private Integer bigBlind = 100;
        private Integer ante = 100;
        private List<BlindLevel> customStructure;
    }

    private final TvState currentState = new TvState();

    private List<BlindLevel> activeStructure() {
        return (currentState.getCustomStructure() != null && !currentState.getCustomStructure().isEmpty())
                ? currentState.getCustomStructure()
                : DEFAULT_STRUCTURE;
    }

    private void applyLevel(int levelNum) {
        List<BlindLevel> structure = activeStructure();
        int idx = Math.max(0, Math.min(levelNum - 1, structure.size() - 1));
        BlindLevel bl = structure.get(idx);
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

    public synchronized TvState setCustomStructure(List<BlindLevel> structure) {
        if (structure != null && !structure.isEmpty()) {
            currentState.setCustomStructure(new java.util.ArrayList<>(structure));
            applyLevel(currentState.getCurrentLevel());
            currentState.setLastSyncTimestamp(System.currentTimeMillis());
        }
        return copyState();
    }

    public synchronized TvState resetCustomStructure() {
        currentState.setCustomStructure(null);
        applyLevel(currentState.getCurrentLevel());
        currentState.setLastSyncTimestamp(System.currentTimeMillis());
        return copyState();
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
                    if (currentState.getCurrentLevel() < activeStructure().size()) {
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
        if (currentState.getCurrentLevel() < activeStructure().size()) {
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
        if (seconds > 0) {
            currentState.setSecondsLeft(seconds);
            currentState.setTotalSeconds(seconds);
        } else {
            List<BlindLevel> structure = activeStructure();
            int idx = Math.max(0, Math.min(currentState.getCurrentLevel() - 1, structure.size() - 1));
            BlindLevel bl = structure.get(idx);
            int breakSecs = (bl.isBreak() && bl.minutes() > 0) ? bl.minutes() * 60 : 5 * 60;
            int lvlSecs = (!bl.isBreak() && bl.minutes() > 0) ? bl.minutes() * 60 : 15 * 60;
            int targetSecs = nextBreak ? breakSecs : lvlSecs;
            currentState.setSecondsLeft(targetSecs);
            currentState.setTotalSeconds(targetSecs);
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
        copy.setCustomStructure(currentState.getCustomStructure());
        return copy;
    }
}
