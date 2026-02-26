package games.BESTbattleship;

import evaluation.RunGames;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import java.util.*;
import java.util.stream.Collectors;

public class RunBattleshipTournament {
    public static void main(String[] args) {
        
        // Sysinfo
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMxBean.getInputArguments();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        // Standard definition if no other args
        Map<String, String> config = new LinkedHashMap<>();
        config.put("game", "BESTBattleship");
        config.put("nPlayers", "2");
        config.put("matchups", "2500");
        config.put("mode", "exhaustive");
        config.put("seed", "42");
        config.put("listener", "games.BESTbattleship.metrics.BestPerformanceListener");
        config.put("playerDirectory", "src/main/java/games/BESTbattleship/agents");
        config.put("verbose", "false");

        // Overwrite with own args put in console
        for (String arg : args) {
            String[] split = arg.split("=");
            if (split.length == 2) {
                config.put(split[0], split[1]);
            }
        }

        String[] finalOptions = config.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);

        // ARGS LOGS
        System.out.println("\n" + "=".repeat(40));
        System.out.println("BATTLESHIP EXPERIMENT EXECUTION LOG");
        System.out.println("=".repeat(40));
        
        System.out.println("\n[SYSTEM & JVM CONFIGURATION]");
        System.out.println("  OS Name        : " + System.getProperty("os.name"));
        System.out.println("  Java Version   : " + System.getProperty("java.version"));
        System.out.println("  Max RAM Heap   : " + maxMemory + " MB");
        System.out.println("  JVM VM Args    : " + jvmArgs);
        
        System.out.println("\n[APPLICATION ARGUMENTS (RunGames)]");
        config.forEach((key, value) -> System.out.printf("  %-20s : %s%n", key, value));
        
        if (args.length > 0) {
            System.out.println("\n[RAW CLI OVERRIDES]");
            for (int i = 0; i < args.length; i++) {
                System.out.println("  " + i + ": " + args[i]);
            }
        }
        System.out.println("\n" + "=".repeat(70) + "\n");

        // Running the tournament
        RunGames.main(finalOptions);
    }
}