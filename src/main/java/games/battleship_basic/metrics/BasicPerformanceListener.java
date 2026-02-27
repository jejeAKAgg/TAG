package games.battleship_basic.metrics;

import core.AbstractGameState;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.battleship_basic.BattleshipGameState;

import java.util.LinkedHashMap;
import java.util.Map;

import java.io.File;

/**
 * A performance-focused listener that records computational efficiency metrics.
 * It tracks Forward Model (FM) calls and execution time to analyze AI performance.
 */
public class BasicPerformanceListener implements IGameListener {
    
    protected Game game;

    protected FileStatsLogger summaryLogger; // Logger for end-of-game summary metrics
    protected FileStatsLogger detailLogger; // Logger for per-turn detailed metrics (e.g., effort per turn during smart determinisation)

    private long[] lastEffort = new long[2]; // To track the cumulative effort at the last recorded point for each player, allowing us to calculate net effort per turn too

    /**
     * Initializes the listener and sets the output file path for performance data.
     */
    public BasicPerformanceListener() {}

    @Override
    public boolean setOutputDirectory(String... folders) {
        try {
            String path = String.join(File.separator, folders) + File.separator;
            new File(path).mkdirs();

            this.summaryLogger = new FileStatsLogger(path + "BASIC_Battleship_SUMMARY.csv");
            this.detailLogger = new FileStatsLogger(path + "BASIC_Battleship_DETAILS.csv");
            
            System.out.println(">>> Performance Loggers initialisés: " + path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Responds to game events to manage metric lifecycle and recording.
     * @param event The event occurring in the game engine.
     */
    @Override
    public void onEvent(Event event) {
        
        // Reset metrics before each match to ensure isolated data points
        if (event.type == Event.GameEvent.ABOUT_TO_START) {
            BattleshipGameState.resetPerformanceMetrics();

            lastEffort[0] = 0;
            lastEffort[1] = 0;
        }

        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            AbstractGameState state = event.state;
            int playerID = state.getCurrentPlayer();
            
            long currentTotalEffort = 0;
            long netEffortThisTurn = 0;
            long currentFallbacks = 0;
            
            Map<String, Object> turnData = new LinkedHashMap<>();
            
            turnData.put("GameID", state.getGameID());
            turnData.put("Turn", state.getTurnCounter());
            turnData.put("Player", playerID);
            turnData.put("Effort_Net", netEffortThisTurn); // Net effort for this turn
            turnData.put("Fallbacks_Total", currentFallbacks);
            
            detailLogger.record(turnData);
        }

        // Record performance data once the game concludes
        if (event.type == Event.GameEvent.GAME_OVER) {
            BattleshipGameState state = (BattleshipGameState) event.state;
            Map<String, Object> data = new LinkedHashMap<>();
            
            // Game and player info for context
            data.put("GameID", state.getGameID());
            data.put("P0_Agent", game.getPlayers().get(0).toString());
            data.put("P1_Agent", game.getPlayers().get(1).toString());

            data.put("P0_AgentHP", state.playerHP[0]);
            data.put("P1_AgentHP", state.playerHP[1]);

            int hpDifference = Math.abs(state.playerHP[0] - state.playerHP[1]);
            data.put("Drama_HPDifference", hpDifference);

            // Metrics for each player (calls, time, effort, score)
            for (int i = 0; i < 2; i++) {
                long calls = BattleshipGameState.totalFMCALLS[i].get();
                long time = BattleshipGameState.totalTimeInCopy[i].get();
                
                data.put("P" + i + "_TotalFMCalls", calls);
                data.put("P" + i + "_TotalTimeNS", time);
                data.put("P" + i + "_AvgTimeCopyNS", calls > 0 ? (double) time / calls : 0);
                
                // Final score for reference (not a performance metric, but useful for analysis)
                data.put("P" + i + "_Score", state.getGameScore(i));
            }

            summaryLogger.record(data);
        }
    }

    /**
     * Processes remaining data and closes the logger.
     */
    @Override
    public void report() { 
        summaryLogger.processDataAndFinish(); 
        detailLogger.processDataAndFinish(); 
    }

    @Override
    public void setGame(Game game) { 
        this.game = game; 
    }

    @Override
    public Game getGame() { 
        return game; 
    }
}