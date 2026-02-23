package games.BESTbattleship.metrics;

import core.AbstractGameState;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.BESTbattleship.BattleshipGameState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;

/**
 * A performance-focused listener that records computational efficiency metrics per player.
 * It tracks Forward Model (FM) calls, search effort, and execution time.
 */
public class BestPerformanceListener implements IGameListener {
    
    protected Game game;

    protected FileStatsLogger summaryLogger; // Logger for end-of-game summary metrics
    protected FileStatsLogger detailLogger; // Logger for per-turn detailed metrics (e.g., effort per turn during smart determinisation)

    private long[] lastEffort = new long[2]; // To track the cumulative effort at the last recorded point for each player, allowing us to calculate net effort per turn too

    public BestPerformanceListener() {}

    @Override
    public boolean setOutputDirectory(String... folders) {
        try {
            String path = String.join(File.separator, folders) + File.separator;
            new File(path).mkdirs();

            this.summaryLogger = new FileStatsLogger(path + "BEST_Battleship_SUMMARY.csv");
            this.detailLogger = new FileStatsLogger(path + "BEST_Battleship_DETAILS.csv");
            
            System.out.println(">>> Performance Loggers initialisés: " + path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ABOUT_TO_START) {
            BattleshipGameState.resetPerformanceMetrics();

            lastEffort[0] = 0;
            lastEffort[1] = 0;
        }

        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            AbstractGameState state = event.state;
            int playerID = state.getCurrentPlayer();
            
            long currentTotalEffort = BattleshipGameState.attemptsPerSolve[playerID].get();
            long netEffortThisTurn = currentTotalEffort - lastEffort[playerID];
            lastEffort[playerID] = currentTotalEffort;
            
            Map<String, Object> turnData = new LinkedHashMap<>();
            
            turnData.put("GameID", state.getGameID());
            turnData.put("Turn", state.getTurnCounter());
            turnData.put("Player", playerID);
            turnData.put("Effort_Net", netEffortThisTurn); // Net effort for this turn
            
            detailLogger.record(turnData);
        }

        if (event.type == Event.GameEvent.GAME_OVER) {
            AbstractGameState state = event.state;
            Map<String, Object> data = new LinkedHashMap<>();
            
            // Game and player info for context
            data.put("GameID", state.getGameID());
            data.put("P0_Agent", game.getPlayers().get(0).toString());
            data.put("P1_Agent", game.getPlayers().get(1).toString());

            // Metrics for each player (calls, time, effort, score)
            for (int i = 0; i < 2; i++) {
                long calls = BattleshipGameState.totalFMCALLS[i].get();
                long time = BattleshipGameState.totalTimeInCopy[i].get();
                long effort = BattleshipGameState.attemptsPerSolve[i].get();
                
                data.put("P" + i + "_TotalFMCalls", calls);
                data.put("P" + i + "_TotalTimeNS", time);
                data.put("P" + i + "_AvgTimeCopyNS", calls > 0 ? (double) time / calls : 0);

                // Total effort
                data.put("P" + i + "_TotalSearchEffort", effort);
                
                // Measures how many attempts on average are needed to satisfy the constraints (i.e., find a consistent grid layout)
                data.put("P" + i + "_AvgSearchEffort", calls > 0 ? (double) effort / calls : 0);
                
                // Final score for reference (not a performance metric, but useful for analysis)
                data.put("P" + i + "_Score", state.getGameScore(i));
            }

            summaryLogger.record(data);
        }
    }

    @Override
    public void report() { 
        summaryLogger.processDataAndFinish(); 
        detailLogger.processDataAndFinish(); 
    }

    @Override
    public void setGame(Game game) { this.game = game; }

    @Override
    public Game getGame() { return game; }
}