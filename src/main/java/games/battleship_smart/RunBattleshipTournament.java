package games.battleship_smart;

import evaluation.RunGames;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;


public class RunBattleshipTournament {

    public static void main(String[] args) {

        // System Informations
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMxBean.getInputArguments();
        long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        // Default config
        Map<String, String> config = new LinkedHashMap<>();

        config.put("game",            "SMARTBattleship");
        config.put("nPlayers",        "2");
        config.put("matchups",        "10000");          // total matchups (RunGames calculates gamesPerMatchup)
        config.put("mode",            "exhaustive");
        config.put("seed",            "2000");
        config.put("verbose",         "false");

        // Agents
        config.put("playerDirectory", "src/main/java/games/battleship_smart/agents");

        // Listeners :
        //   1. SmartPerformanceListener  → custom
        config.put("listener",
            "games.battleship_smart.metrics.SmartPerformanceListener");

        // Output path
        config.put("destDir", "results/battleship_smart/");

        // Overrides CLI
        for (String arg : args) {
            String[] split = arg.split("=", 2);
            if (split.length == 2) {
                config.put(split[0].trim(), split[1].trim());
            }
        }

        String[] finalOptions = config.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);

        // LOGS
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BATTLESHIP SMART — TOURNAMENT");
        System.out.println("=".repeat(60));

        System.out.println("\n[SYSTEM]");
        System.out.printf("  OS           : %s%n", System.getProperty("os.name"));
        System.out.printf("  Java         : %s%n", System.getProperty("java.version"));
        System.out.printf("  Max Heap     : %d MB%n", maxMemory);
        System.out.printf("  JVM Args     : %s%n", jvmArgs);

        System.out.println("\n[TOURNAMENT CONFIG]");
        config.forEach((k, v) -> System.out.printf("  %-20s : %s%n", k, v));

        if (args.length > 0) {
            System.out.println("\n[CLI OVERRIDES]");
            for (String arg : args) System.out.println("  " + arg);
        }

        System.out.println("\n" + "=".repeat(60) + "\n");

        // START
        RunGames.main(finalOptions);
    }
}