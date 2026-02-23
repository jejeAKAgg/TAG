package games.BESTbattleship;

import evaluation.RunGames;

import java.util.Arrays;

public class RunBattleshipTournament {
    public static void main(String[] args) {
        
        // Default options
        String[] defaultOptions = new String[]{
            "game=BESTBattleship",
            "nPlayers=2",
            "mode=exhaustive",
            "seed=42",
            "listener=games.BESTbattleship.metrics.BestPerformanceListener",
            "playerDirectory=src/main/java/games/BESTbattleship/agents",
            "verbose=false"
        };

        String[] finalOptions = new String[defaultOptions.length + args.length];
        System.arraycopy(defaultOptions, 0, finalOptions, 0, defaultOptions.length);
        System.arraycopy(args, 0, finalOptions, defaultOptions.length, args.length);
        
        // RUN
        System.out.println("Running BESTBattleship with config: " + Arrays.toString(finalOptions));
        RunGames.main(finalOptions);
    }
}