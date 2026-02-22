package games.BASICbattleship;

import evaluation.RunGames;

public class RunBattleshipTournament {
    public static void main(String[] args) {
        
        String[] options = new String[]{
            
            // --- CONFIG ---
            "game=BASICBattleship",
            "nPlayers=2",
            
            // --- CONFIG TOURNOI ---
            "mode=exhaustive", // Make each agent play against every other agent in a round-robin format
            "matchups=1000", // Total number of matches to be played between each pair of agents
            "seed=42",
            
            // --- OTHER METRICS ---
            "listener=games.BASICbattleship.metrics.BasicPerformanceListener",
            
            // --- AGENTS ---
            "playerDirectory=src/main/java/games/BASICbattleship/agents",
            
            // --- RESULTS ---
            "destDir=metrics/out/BASICbattleship_results",
            
            // --- LOGS ---
            "verbose=true"
        };
        
        // RUN
        System.out.println("Metrics: BASICBattleship");
        RunGames.main(options);
    }
}