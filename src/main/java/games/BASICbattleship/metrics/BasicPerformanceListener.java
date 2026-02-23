package games.BASICbattleship.metrics;

import core.AbstractGameState;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import games.BASICbattleship.BattleshipGameState;
import java.util.LinkedHashMap;
import java.util.Map;

import java.io.File;
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
        // Laisser vide, le logger sera créé dans setOutputDirectory
    }

    @Override
    public boolean setOutputDirectory(String... folders) {
        if (folders == null || folders.length == 0) return false;

        try {
            String folderPath = String.join(File.separator, folders);
            String fileName = "BASIC_Battleship_PERFORMANCE.csv";
            String fullPath = folderPath + File.separator + fileName;

            File file = new File(fullPath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            this.logger = new FileStatsLogger(fullPath);
            System.out.println(">>> Performance Logger initialisé : " + fullPath);
            return true; // On retourne un boolean comme demandé
        } catch (Exception e) {
            System.err.println("Erreur Logger : " + e.getMessage());
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