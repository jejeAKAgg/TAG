package games.BESTbattleship;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.GridBoard;
import core.components.BoardNode;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import games.GameType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the state of a Battleship game.
 * Handles hidden information by randomizing opponent ship locations during state copying.
 */
public class BattleshipGameState extends AbstractGameState implements IPrintable, IGridGameState {

    // Performance metrics for research analysis
    public static AtomicLong totalFMCALLS = new AtomicLong(0);
    public static AtomicLong totalTimeInCopy = new AtomicLong(0);
    public static AtomicLong fallbackCount = new AtomicLong(0);

    // Current health points for both players (number of ship cells remaining)
    public int[] playerHP;
    
    // Grids for ship placement (hidden from opponent)
    public GridBoard player0ShipGrid;
    public GridBoard player1ShipGrid;
    
    // Grids for tracking shots fired (visible to the player)
    public GridBoard player0ShotGrid;
    public GridBoard player1ShotGrid;

    /**
     * @param gameParameters Game configuration (grid size, ship sizes, etc.)
     * @param nPlayers Number of players (typically 2)
     */
    public BattleshipGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        this.rnd = new Random(gameParameters.getRandomSeed());
        this.playerHP = new int[nPlayers];
    }

    @Override
    protected GameType _getGameType() {
        return GameType.BESTBattleship;
    }

    /**
     * Creates a copy of the current game state.
     * Crucial for AI search: if playerId is provided, opponent ships are randomized 
     * to simulate partial observability (Hidden Information).
     * * @param playerId The perspective from which the copy is made (-1 for full visibility).
     * @return A deep copy of the state.
     */
    @Override
    protected AbstractGameState _copy(int playerId) {
        long startTime = System.nanoTime();

        BattleshipGameState copy = new BattleshipGameState(gameParameters.copy(), getNPlayers());
        
        copy.playerHP = this.playerHP.clone();
        copy.player0ShotGrid = this.player0ShotGrid.copy();
        copy.player1ShotGrid = this.player1ShotGrid.copy();
        
        // Ensure the copy has a deterministic but unique random seed
        copy.rnd = new Random(this.rnd.nextLong()); 

        // System.out.println("Copy called with playerId = " + playerId); [DEBUG]

        if (playerId == -1) {
            
            // Observer view: Full visibility of both grids
            copy.player0ShipGrid = this.player0ShipGrid.copy();
            copy.player1ShipGrid = this.player1ShipGrid.copy();

        } else {

            // Player view: Only own ships are known, opponent's ships are randomized (smart determinisation)
            if (playerId == 0) {
                copy.player0ShipGrid = this.player0ShipGrid.copy();
                copy.player1ShipGrid = smartDeterminiseGrid(this.player0ShotGrid, this.player1ShipGrid.getWidth(), this.rnd);
            } else {
                copy.player1ShipGrid = this.player1ShipGrid.copy();
                copy.player0ShipGrid = smartDeterminiseGrid(this.player1ShotGrid, this.player0ShipGrid.getWidth(), this.rnd);
            }
        }

        totalFMCALLS.incrementAndGet();
        long duration = System.nanoTime() - startTime;
        totalTimeInCopy.addAndGet(duration);

        return copy;
    }

    /**
     * Performs "Smart Determinization" (Optimized High-Performance Version) to generate a plausible opponent ship layout.
     * This method creates a grid hypothesis that is logically consistent with all 
     * known information (HITs and MISSes) observed by the current player.
     * * The algorithm follows these steps:
     * 1. Scans known shots to identify all hit and miss coordinates.
     * 2. Attempts to place ships such that every known HIT is covered by a ship part.
     * 3. Ensures no ship parts are placed on known MISS coordinates.
     * 4. If a consistent layout cannot be found within the attempt limit, it falls back
     * to a naive randomization as a safety measure.
     *
     * @param knownShots The grid containing the current player's shot history (HIT/MISS).
     * @param size The dimensions of the grid.
     * @param r Random number generator for stochastic placement.
     * @return A logically consistent {@link GridBoard} representing a possible opponent state.
     */
    private GridBoard smartDeterminiseGrid(GridBoard knownShots, int size, Random r) {
        BattleshipParameters params = (BattleshipParameters) getGameParameters();

        GridBoard hypothesis = new GridBoard(size, size, new BoardNode(BattleshipConstants.WATER));

        // OPTIMISATION 1 : "Map of Misses"
        // Used to quickly check if a cell is a known MISS without iterating through a list of coordinates (O(1) vs O(N)).
        boolean[][] missMap = new boolean[size][size];
        
        // OPTIMISATION 2: "Pre-allocated Hit List" pour éviter les coûts de réallocation dynamique lors de l'ajout de hits.
        // Used to avoid the overhead of dynamic resizing that comes with ArrayList when adding elements, based on maximum number of items.
        List<int[]> hitCoords = new ArrayList<>(Arrays.stream(params.shipSizes).sum()); 

        // Extracting known HIT and MISS coordinates from the player's shot grid
        for(int x=0; x<size; x++) {
            for(int y=0; y<size; y++) {
                String status = ((BoardNode)knownShots.getElement(x, y)).getComponentName();
                if (status.equals(BattleshipConstants.HIT)) {
                    hitCoords.add(new int[]{x, y});
                } else if (status.equals(BattleshipConstants.MISS)) {
                    missMap[x][y] = true; // Quick access for MISS checks
                }
            }
        }
        
        // On prépare la liste des bateaux une fois
        List<Integer> shipsToPlace = new ArrayList<>(params.shipSizes.length);

        // BOUCLE DE RANDOM RESTART (Garde-fou à 50 tentatives)
        for (int attempts = 0; attempts < 50; attempts++) {
            
            // OPTIMISATION 3 : "Fast Reset (Zero Allocation)""
            // Just set the component name to WATER for all cells instead of creating new BoardNode instances.
            resetGrid(hypothesis);
            
            shipsToPlace.clear();
            for(int s : params.shipSizes) shipsToPlace.add(s);
            Collections.shuffle(shipsToPlace, r);

            List<int[]> uncoveredHits = new ArrayList<>(hitCoords);
            boolean success = true;

            for (int shipSize : shipsToPlace) {
                boolean placed = false;

                // CASE A: There are still orphan HITs -> We MUST place the ship on them (Constraint Satisfaction)
                if (!uncoveredHits.isEmpty()) {
                    for (int k=0; k<50; k++) {
                        int[] targetHit = uncoveredHits.get(r.nextInt(uncoveredHits.size()));
                        
                        boolean horizontal = r.nextBoolean();
                        int offset = r.nextInt(shipSize); // Because the hit could be wherever on a long lenght ship (front, middle, back)
                        
                        int startX = horizontal ? targetHit[0] - offset : targetHit[0];
                        int startY = horizontal ? targetHit[1] : targetHit[1] - offset;

                        // Fast check (O(1))
                        if (canPlaceShipFast(hypothesis, startX, startY, shipSize, horizontal, missMap)) {
                            placeShip(hypothesis, startX, startY, shipSize, horizontal);
                            placed = true;
                            
                            // OPTIMISATION 4: Manual Hit Removal
                            removeCoveredHits(uncoveredHits, startX, startY, shipSize, horizontal);
                            break;
                        }
                    }
                } 
                
                // CAS B: No more orphan HITs -> Free placement (but still respecting MISS constraints) 
                else {
                    for (int k=0; k<50; k++) {
                        int x = r.nextInt(size);
                        int y = r.nextInt(size);
                        boolean horizontal = r.nextBoolean();
                        
                        if (canPlaceShipFast(hypothesis, x, y, shipSize, horizontal, missMap)) {
                            placeShip(hypothesis, x, y, shipSize, horizontal);
                            placed = true;
                            break;
                        }
                    }
                }

                if (!placed) {
                    success = false;
                    break; // Restart
                }
            }

            // If all ships are placed AND all hits are covered -> SUCCESS
            if (success && uncoveredHits.isEmpty()) {
                return hypothesis;
            }
        }

        fallbackCount.incrementAndGet();

        // FALLBACK: If we fail to find a consistent layout after 50 attempts (very rare or indicates contradictory information), we return a random grid.
        resetGrid(hypothesis);
        logEvent(null, "Fallback to random grid after 50 failed attempts in smart determinisation.");
        randomizeGrid(hypothesis, r); 
        return hypothesis;
    }

    private boolean canPlaceShipFast(GridBoard grid, int x, int y, int size, boolean horizontal, boolean[][] missMap) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        if (horizontal) {
            if (x < 0 || x + size > width || y < 0 || y >= height) return false;
            for (int i = 0; i < size; i++) {
                if (missMap[x + i][y]) return false;
                if (!((BoardNode)grid.getElement(x + i, y)).getComponentName().equals(BattleshipConstants.WATER)) return false;
            }
        } else {
            if (y < 0 || y + size > height || x < 0 || x >= width) return false;
            for (int i = 0; i < size; i++) {
                if (missMap[x][y + i]) return false;
                if (!((BoardNode)grid.getElement(x, y + i)).getComponentName().equals(BattleshipConstants.WATER)) return false;
            }
        }
        return true;
    }


    private void removeCoveredHits(List<int[]> hits, int x, int y, int size, boolean horizontal) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            int[] h = hits.get(i);
            boolean covered;
            if (horizontal) {
                covered = (h[1] == y && h[0] >= x && h[0] < x + size);
            } else {
                covered = (h[0] == x && h[1] >= y && h[1] < y + size);
            }
            if (covered) {
                hits.remove(i);
            }
        }
    }

    private void resetGrid(GridBoard grid) {
        for(int x=0; x<grid.getWidth(); x++)
            for(int y=0; y<grid.getHeight(); y++)
                grid.setElement(x, y, new BoardNode(BattleshipConstants.WATER));
    }

    private boolean canPlaceShip(GridBoard grid, int x, int y, int size, boolean horizontal, List<int[]> missCoords) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        if (horizontal) {
            if (x < 0 || x + size > width || y < 0 || y >= height) return false;
            for (int i = 0; i < size; i++) {
                if (!((BoardNode)grid.getElement(x + i, y)).getComponentName().equals(BattleshipConstants.WATER)) return false;
                if (isMiss(x + i, y, missCoords)) return false;
            }
        } else {
            if (y < 0 || y + size > height || x < 0 || x >= width) return false;
            for (int i = 0; i < size; i++) {
                if (!((BoardNode)grid.getElement(x, y + i)).getComponentName().equals(BattleshipConstants.WATER)) return false;
                if (isMiss(x, y + i, missCoords)) return false;
            }
        }
        return true;
    }

    public void randomizeGrid(GridBoard grid, Random r) {
        BattleshipParameters params = (BattleshipParameters) getGameParameters();
        
        boolean generationComplete = false;
        
        // Infinite loop until success
        while (!generationComplete) {
            
            resetGrid(grid); 
            generationComplete = true; 

            for (int shipSize : params.shipSizes) {
                boolean placed = false;
                int attempts = 0; 
                
                // On essaie de placer ce bateau
                while (!placed && attempts < 100) {
                    int x = r.nextInt(grid.getWidth());
                    int y = r.nextInt(grid.getHeight());
                    boolean horizontal = r.nextBoolean();
                    
                    if (canPlaceShip(grid, x, y, shipSize, horizontal, new ArrayList<>())) {
                        placeShip(grid, x, y, shipSize, horizontal);
                        placed = true;
                    }
                    attempts++;
                }

                // If we fail to place a ship, we mark the entire generation as failed and break out of the loop to restart from scratch.
                if (!placed) {
                    generationComplete = false;
                    break; // Restart
                }
            }

            // Note: No increment of globalAttempts that stops everything. We keep going until it works.
        }
    }

    private boolean isMiss(int x, int y, List<int[]> missCoords) {
        for(int[] m : missCoords) {
            if (m[0] == x && m[1] == y) return true;
        }
        return false;
    }


    private void placeShip(GridBoard grid, int x, int y, int size, boolean horizontal) {
        String shipName = BattleshipConstants.SHIP; 
        if (horizontal) {
            for (int i = 0; i < size; i++) {
                ((BoardNode)grid.getElement(x + i, y)).setComponentName(shipName);
            }
        } else { // Vertical
            for (int i = 0; i < size; i++) {
                ((BoardNode)grid.getElement(x, y + i)).setComponentName(shipName);
            }
        }
    }

    @Override
    public GridBoard getGridBoard() {
        return (getCurrentPlayer() == 0) ? player0ShotGrid : player1ShotGrid;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        // 1. On garde ton score de base (Victoire/Défaite/Pourcentage de hits)
        double baseScore = getGameScore(playerId);
        
        // 2. On ajoute un petit bonus stratégique
        // Si le joueur a des hits groupés, c'est mieux que des hits éparpillés
        double strategicBonus = getClusteringBonus(playerId) * 0.1; // Poids faible (10%)

        return baseScore + strategicBonus;
    }

    private double getClusteringBonus(int playerId) {
        GridBoard myShots = (playerId == 0) ? player0ShotGrid : player1ShotGrid;
        int connectedHits = 0;
        int w = myShots.getWidth();
        int h = myShots.getHeight();

        // On parcourt la grille
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                // Si on a un HIT ici...
                if (((BoardNode)myShots.getElement(x, y)).getComponentName().equals(BattleshipConstants.HIT)) {
                    // ... on regarde à Droite et en Bas (pour éviter de compter deux fois)
                    if (x + 1 < w && ((BoardNode)myShots.getElement(x+1, y)).getComponentName().equals(BattleshipConstants.HIT)) {
                        connectedHits++;
                    }
                    if (y + 1 < h && ((BoardNode)myShots.getElement(x, y+1)).getComponentName().equals(BattleshipConstants.HIT)) {
                        connectedHits++;
                    }
                }
            }
        }
        // On normalise (on suppose qu'avoir 5 connexions est le max utile pour l'heuristique)
        return Math.min(connectedHits, 5) / 5.0;
    }

    @Override
    public double getGameScore(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.WIN_GAME) return 1.0;
        if (playerResults[playerId] == CoreConstants.GameResult.LOSE_GAME) return 0.0;

        BattleshipParameters params = (BattleshipParameters) getGameParameters();
        int totalHealth = 0;
        for (int size : params.shipSizes) totalHealth += size;
        
        return countHits(playerId) / totalHealth;
    }

    private double countHits(int playerId) {
        GridBoard myShots = (playerId == 0) ? player0ShotGrid : player1ShotGrid;
        double hits = 0;
        for (int x = 0; x < myShots.getWidth(); x++) {
            for (int y = 0; y < myShots.getHeight(); y++) {
                BoardNode cell = (BoardNode) myShots.getElement(x, y);
                if (cell.getComponentName().equals(BattleshipConstants.HIT)) {
                    hits++;
                }
            }
        }
        return hits;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(player0ShipGrid);
            add(player1ShipGrid);
            add(player0ShotGrid);
            add(player1ShotGrid);
        }};
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BattleshipGameState that = (BattleshipGameState) o;
        return Objects.equals(player0ShipGrid, that.player0ShipGrid) &&
               Objects.equals(player1ShipGrid, that.player1ShipGrid) &&
               Objects.equals(player0ShotGrid, that.player0ShotGrid) &&
               Objects.equals(player1ShotGrid, that.player1ShotGrid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), player0ShipGrid, player1ShipGrid, player0ShotGrid, player1ShotGrid);
    }

    @Override
    public void printToConsole() {
        System.out.println("========================================");
        System.out.println("BATTLESHIP STATE - PERSPECTIVE: " + (getCurrentPlayer() == 0 ? "P0" : "P1"));
        System.out.println("Phase: " + getGamePhase() + " | Round: " + getRoundCounter());
        System.out.println("HP: P0 = " + playerHP[0] + " | P1 = " + playerHP[1]);
        System.out.println("----------------------------------------");
        
        if (getCurrentPlayer() == 0) {
            System.out.println("P0 SHIP GRID (Ma flotte):");
            System.out.println(player0ShipGrid.toString());
            System.out.println("P0 SHOT GRID (Mes tirs):");
            System.out.println(player0ShotGrid.toString());
        } else {
            System.out.println("P1 SHIP GRID (Ma flotte):");
            System.out.println(player1ShipGrid.toString());
            System.out.println("P1 SHOT GRID (Mes tirs):");
            System.out.println(player1ShotGrid.toString());
        }
        System.out.println("========================================");
    }

    public static void resetPerformanceMetrics() {
        totalFMCALLS.set(0);
        totalTimeInCopy.set(0);
        fallbackCount.set(0);
    }
}