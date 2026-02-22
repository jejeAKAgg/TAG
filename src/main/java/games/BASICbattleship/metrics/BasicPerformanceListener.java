package games.BASICbattleship.metrics;

import core.AbstractGameState;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.BASICbattleship.BattleshipGameState;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A performance-focused listener that records computational efficiency metrics.
 * It tracks Forward Model (FM) calls and execution time to analyze AI performance.
 */

public class BasicPerformanceListener implements IGameListener {
    
    protected FileStatsLogger logger;
    protected Game game;

    /**
     * Initializes the listener and sets the output file path for performance data.
     */

    public BasicPerformanceListener() {
        
        // Output file configuration for performance tracking
        this.logger = new FileStatsLogger("metrics/out/BASIC_Battleship_PERFORMANCE.csv");
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
        }

        // Record performance data once the game concludes
        if (event.type == Event.GameEvent.GAME_OVER) {
            AbstractGameState state = event.state;
            Map<String, Object> data = new LinkedHashMap<>();
            
            // Basic game identification
            data.put("GameID", state.getGameID());
            data.put("Player0", game.getPlayers().get(0).toString());
            data.put("Player1", game.getPlayers().get(1).toString());
            
            // Final score to correlate computational effort with win efficacy
            data.put("P0_Score", state.getGameScore(0));
            
            // Performance metrics extracted from the global counters in GameState
            long calls = BattleshipGameState.totalFMCALLS.get();
            long time = BattleshipGameState.totalTimeInCopy.get();
            
            data.put("TotalFMCalls", calls);
            data.put("TotalTimeNS", time);
            
            // Calculate average time per state copy for efficiency analysis
            data.put("AvgTimePerCopyNS", calls > 0 ? (double)time / calls : 0);

            logger.record(data);
        }
    }

    /**
     * Processes remaining data and closes the logger.
     */

    @Override
    public void report() { 
        logger.processDataAndFinish(); 
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