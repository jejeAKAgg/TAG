package games.BASICbattleship;

import evaluation.RunGames;

import java.util.Arrays;

public class RunBattleshipTournament {
    public static void main(String[] args) {
        
        // Default options
        String[] defaultOptions = new String[]{
            "game=BASICBattleship",
            "nPlayers=2",
            "mode=exhaustive",
            "seed=42",
            "listener=games.BASICbattleship.metrics.BasicPerformanceListener",
            "playerDirectory=src/main/java/games/BASICbattleship/agents",
            "verbose=false"
        };

        String[] finalOptions = new String[defaultOptions.length + args.length];
        System.arraycopy(defaultOptions, 0, finalOptions, 0, defaultOptions.length);
        System.arraycopy(args, 0, finalOptions, defaultOptions.length, args.length);
        
        // RUN
        System.out.println("Running BASICBattleship with config: " + Arrays.toString(finalOptions));
        RunGames.main(finalOptions);
    }
}