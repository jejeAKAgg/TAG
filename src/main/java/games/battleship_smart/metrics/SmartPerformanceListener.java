package games.battleship_smart.metrics;

import core.AbstractGameState;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.battleship_smart.BattleshipGameState;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;


public class SmartPerformanceListener implements IGameListener {

    protected Game game;

    /** ENDGAME logger → SUMMARY.csv */
    protected FileStatsLogger summaryLogger;

    /** PER TURN logger → DETAILS.csv */
    protected FileStatsLogger detailLogger;

    /** Cumulated effort */
    private long[] lastEffort = new long[2];

    public SmartPerformanceListener() {}

    @Override
    public boolean setOutputDirectory(String... folders) {
        try {
            String path = String.join(File.separator, folders) + File.separator;
            new File(path).mkdirs();

            this.summaryLogger = new FileStatsLogger(path + "SMART_Battleship_SUMMARY.csv");
            this.detailLogger  = new FileStatsLogger(path + "SMART_Battleship_DETAILS.csv");

            System.out.println(">>> SmartPerformanceListener → " + path);
            return true;
        } catch (Exception e) {
            System.err.println("SmartPerformanceListener — error setOutputDirectory : " + e.getMessage());
            return false;
        }
    }

    // Event function(s)
    @Override
    public void onEvent(Event event) {

        // ABOUT_TO_START
        if (event.type == Event.GameEvent.ABOUT_TO_START) {
            BattleshipGameState.resetPerformanceMetrics();
            lastEffort[0] = 0;
            lastEffort[1] = 0;
        }

        // ACTION_CHOSEN
        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            AbstractGameState state = event.state;
            int playerID = state.getCurrentPlayer();

            long currentEffort  = BattleshipGameState.attemptsPerSolve[playerID].get();
            long netEffort      = currentEffort - lastEffort[playerID];
            lastEffort[playerID] = currentEffort;

            long fallbacks = BattleshipGameState.fallbackCount[playerID].get();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("GameID",          state.getGameID());
            row.put("Turn",            state.getTurnCounter());
            row.put("Player",          playerID);
            row.put("Effort_Net",      netEffort);
            row.put("Fallbacks_Total", fallbacks);

            if (detailLogger != null) detailLogger.record(row);
        }

        // GAME OVER
        if (event.type == Event.GameEvent.GAME_OVER) {
            BattleshipGameState state = (BattleshipGameState) event.state;

            Map<String, Object> row = new LinkedHashMap<>();

            // Context
            row.put("GameID",            state.getGameID());
            row.put("P0_Agent",          game.getPlayers().get(0).toString());
            row.put("P1_Agent",          game.getPlayers().get(1).toString());

            // HP (for drama metrics)
            row.put("P0_HP_Remaining",   state.playerHP[0]);
            row.put("P1_HP_Remaining",   state.playerHP[1]);
            row.put("Drama_HPDifference", Math.abs(state.playerHP[0] - state.playerHP[1]));

            // Detailed metrics per player
            for (int i = 0; i < 2; i++) {
                long calls     = BattleshipGameState.totalFMCALLS[i].get();
                long timeNS    = BattleshipGameState.totalTimeInCopy[i].get();
                long effort    = BattleshipGameState.attemptsPerSolve[i].get();
                long fallbacks = BattleshipGameState.fallbackCount[i].get();

                double avgTimeNS   = calls > 0 ? (double) timeNS  / calls : 0.0;
                double avgEffort   = calls > 0 ? (double) effort   / calls : 0.0;
                double successRate = calls > 0 ? (double)(calls - fallbacks) / calls * 100.0 : 0.0;

                String p = "P" + i + "_";
                row.put(p + "TotalFMCalls",    calls);
                row.put(p + "TotalTimeNS",     timeNS);
                row.put(p + "AvgTimeCopyNS",   avgTimeNS);
                row.put(p + "TotalEffort",     effort);
                row.put(p + "AvgEffort",       avgEffort);
                row.put(p + "TotalFallbacks",  fallbacks);
                row.put(p + "CSP_SuccessRate", successRate);
                row.put(p + "Score",           state.getGameScore(i));
            }

            if (summaryLogger != null) summaryLogger.record(row);
        }
    }

    // Overview
    @Override
    public void report() {
        if (summaryLogger != null) summaryLogger.processDataAndFinish();
        if (detailLogger  != null) detailLogger.processDataAndFinish();
    }

    // Other(s)
    @Override
    public void setGame(Game game) { this.game = game; }

    @Override
    public Game getGame() { return game; }
}