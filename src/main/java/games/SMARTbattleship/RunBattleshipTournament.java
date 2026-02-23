package games.SMARTbattleship;

import evaluation.RunGames;

import java.util.Arrays;

public class RunBattleshipTournament {
    public static void main(String[] args) {
        
        // Default options
        String[] defaultOptions = new String[]{
            "game=SMARTBattleship",
            "nPlayers=2",
            "mode=exhaustive",
            "seed=42",
            "listener=games.SMARTbattleship.metrics.SmartPerformanceListener",
            "playerDirectory=src/main/java/games/SMARTbattleship/agents",
            "verbose=false"
        };

        String[] finalOptions = new String[defaultOptions.length + args.length];
        System.arraycopy(defaultOptions, 0, finalOptions, 0, defaultOptions.length);
        System.arraycopy(args, 0, finalOptions, defaultOptions.length, args.length);
        
        // RUN
        System.out.println("Running SMARTBattleship with config: " + Arrays.toString(finalOptions));
        RunGames.main(finalOptions);
    }
}