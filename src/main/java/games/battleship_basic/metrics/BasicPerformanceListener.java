package games.battleship_basic.metrics;

import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.battleship_basic.BattleshipGameState;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;


public class BasicPerformanceListener implements IGameListener {

    protected Game game;

    /** ENDGAME logger → SUMMARY.csv */
    protected FileStatsLogger summaryLogger;

    public BasicPerformanceListener() {}

    @Override
    public boolean setOutputDirectory(String... folders) {
        try {
            String path = String.join(File.separator, folders) + File.separator;
            new File(path).mkdirs();

            this.summaryLogger = new FileStatsLogger(path + "BASIC_Battleship_SUMMARY.csv");

            System.out.println(">>> BasicPerformanceListener → " + path);
            return true;
        } catch (Exception e) {
            System.err.println("BasicPerformanceListener — error setOutputDirectory : " + e.getMessage());
            return false;
        }
    }

    // Event function(s)
    @Override
    public void onEvent(Event event) {

        // ABOUT_TO_START
        if (event.type == Event.GameEvent.ABOUT_TO_START) {
            BattleshipGameState.resetPerformanceMetrics();
        }

        // GAME OVER
        if (event.type == Event.GameEvent.GAME_OVER) {
            BattleshipGameState state = (BattleshipGameState) event.state;

            Map<String, Object> row = new LinkedHashMap<>();

            // Context
            row.put("GameID",             state.getGameID());
            row.put("P0_Agent",           game.getPlayers().get(0).toString());
            row.put("P1_Agent",           game.getPlayers().get(1).toString());

            // HP (for drama metrics)
            row.put("P0_HP_Remaining",    state.playerHP[0]);
            row.put("P1_HP_Remaining",    state.playerHP[1]);
            row.put("Drama_HPDifference", Math.abs(state.playerHP[0] - state.playerHP[1]));

            // Detailed metrics per player
            for (int i = 0; i < 2; i++) {
                long calls  = BattleshipGameState.totalFMCALLS[i].get();
                long timeNS = BattleshipGameState.totalTimeInCopy[i].get();

                String p = "P" + i + "_";
                row.put(p + "TotalFMCalls",  calls);
                row.put(p + "TotalTimeNS",   timeNS);
                row.put(p + "AvgTimeCopyNS", calls > 0 ? (double) timeNS / calls : 0.0);
                row.put(p + "Score",         state.getGameScore(i));
            }

            if (summaryLogger != null) summaryLogger.record(row);
        }
    }

    // Overview
    @Override
    public void report() {
        if (summaryLogger != null) summaryLogger.processDataAndFinish();
    }

    // Other(s)
    @Override
    public void setGame(Game game) { this.game = game; }

    @Override
    public Game getGame() { return game; }
}