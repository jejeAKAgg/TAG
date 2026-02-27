package games.battleship_smart;

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

    // Performance metrics for research analysis, indexed by PlayerID [0, 1]
    public static AtomicLong[] totalFMCALLS = {new AtomicLong(0), new AtomicLong(0)};
    public static AtomicLong[] totalTimeInCopy = {new AtomicLong(0), new AtomicLong(0)};
    public static AtomicLong[] attemptsPerSolve = {new AtomicLong(0), new AtomicLong(0)};
    public static AtomicLong[] fallbackCount = {new AtomicLong(0), new AtomicLong(0)};

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
        return GameType.SMARTBattleship;
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
                copy.player1ShipGrid = smartDeterminiseGrid(this.player0ShotGrid, this.player1ShipGrid.getWidth(), copy.rnd, 0);
            } else {
                copy.player1ShipGrid = this.player1ShipGrid.copy();
                copy.player0ShipGrid = smartDeterminiseGrid(this.player1ShotGrid, this.player0ShipGrid.getWidth(), copy.rnd, 1);
            }
        }

        // Metrics attribution to the requesting player
        if (playerId >= 0 && playerId < 2) {
            totalFMCALLS[playerId].incrementAndGet();
            long duration = System.nanoTime() - startTime;
            totalTimeInCopy[playerId].addAndGet(duration);
        }
        
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
     * @param playerID The ID of the player for whom we are generating the hypothesis (used for logging/metrics).
     * @return A logically consistent {@link GridBoard} representing a possible opponent state.
     */
    private GridBoard smartDeterminiseGrid(GridBoard knownShots, int size, Random r, int playerID) {

        BattleshipParameters params = (BattleshipParameters) getGameParameters();

        GridBoard hypothesis = new GridBoard(size, size);

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                hypothesis.setElement(x, y, new BoardNode(BattleshipConstants.WATER));
            }
        }

        if (isGridEmpty(knownShots)) {
            randomizeGrid(hypothesis, r);
            return hypothesis;
        }

        // OPTIMISATION 1 : "Map of Misses"
        // Used to quickly check if a cell is a known MISS without iterating through a list of coordinates (O(1) vs O(N))
        boolean[][] missMap = new boolean[size][size];
        
        // OPTIMISATION 2: "Pre-allocated Hit List"
        // Used to avoid the overhead of dynamic resizing that comes with ArrayList when adding elements, based on maximum number of items
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
        
        // We prepare the list of ships once, and shuffle it for each attempt to add variability in placement
        List<Integer> shipsToPlace = new ArrayList<>(params.shipSizes.length);

        // Max attempts is a safeguard against infinite loops in cases of contradictory information (e.g., too many hits without enough space to place ships)
        for (int attempts = 0; attempts < 10000; attempts++) {
            
            // OPTIMISATION 3 : "Fast Reset (Zero Allocation)""
            // Just set the component name to WATER for all cells instead of creating new BoardNode instances
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
                        if (canPlaceShip(hypothesis, startX, startY, shipSize, horizontal, missMap)) {
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
                        
                        if (canPlaceShip(hypothesis, x, y, shipSize, horizontal, missMap)) {
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
                attemptsPerSolve[playerID].addAndGet(attempts+1);
                return hypothesis;
            }
        }

        // No fallbacks this time, we throw an exception to signal that the constraints are likely contradictory (e.g., too many hits without enough space to place ships)
        // helps identify potential bugs in the game logic or in the AI's understanding of the state
        //throw new IllegalStateException("CSP solver failed to find a valid state for player " + playerID + " after 10000 attempts.");
        
        // CSP failed case
        fallbackCount[playerID].incrementAndGet();
        
        resetGrid(hypothesis);
        randomizeGrid(hypothesis, r);
        
        return hypothesis;

    }

    private boolean canPlaceShip(GridBoard grid, int x, int y, int size, boolean horizontal, boolean[][] missMap) {
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

    private void placeShip(GridBoard grid, int x, int y, int size, boolean horizontal) {
        for (int i = 0; i < size; i++) {
            if (horizontal) ((BoardNode)grid.getElement(x + i, y)).setComponentName(BattleshipConstants.SHIP);
            else ((BoardNode)grid.getElement(x, y + i)).setComponentName(BattleshipConstants.SHIP);
        }
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

    /**
     * Sets up the own player's ships.
     * @param grid The player's grid.
     * @param r Random number generator.
     */
    public void randomizeGrid(GridBoard grid, Random r) {
        BattleshipParameters params = (BattleshipParameters) getGameParameters();
        
        boolean generationComplete = false;
        
        // Infinite loop until success
        while (!generationComplete) {
            
            resetGrid(grid); 
            generationComplete = true; 

            // Setting up an empty MISS map
            boolean[][] emptyMissMap = new boolean[grid.getWidth()][grid.getHeight()];

            for (int shipSize : params.shipSizes) {
                boolean placed = false;
                int attempts = 0; 
                
                while (!placed && attempts < 10000) {
                    int x = r.nextInt(grid.getWidth());
                    int y = r.nextInt(grid.getHeight());
                    boolean horizontal = r.nextBoolean();
                    
                    if (canPlaceShip(grid, x, y, shipSize, horizontal, emptyMissMap)) {
                        placeShip(grid, x, y, shipSize, horizontal);
                        placed = true;
                    }
                    attempts++;
                }

                // If we fail to place a ship, we mark the entire generation as failed and break out of the loop to restart from scratch
                if (!placed) {
                    generationComplete = false;
                    break; // Restart
                }
            }

            // Note: No increment of globalAttempts that stops everything. We keep going until it works
        }
    }

    private void resetGrid(GridBoard grid) {
        for(int x=0; x<grid.getWidth(); x++)
            for(int y=0; y<grid.getHeight(); y++)
                ((BoardNode)grid.getElement(x, y)).setComponentName(BattleshipConstants.WATER);
    }

    private boolean isGridEmpty(GridBoard board) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                String name = ((BoardNode)board.getElement(x, y)).getComponentName();

                if (!name.equals(BattleshipConstants.WATER)) return false;
            }
        }
        return true;
    }



    @Override
    public GridBoard getGridBoard() {
        return (getCurrentPlayer() == 0) ? player0ShotGrid : player1ShotGrid;
    }

    @Override
    public double getGameScore(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.WIN_GAME) return 1.0;
        if (playerResults[playerId] == CoreConstants.GameResult.LOSE_GAME) return 0.0;
        
        int totalHealth = Arrays.stream(((BattleshipParameters)getGameParameters()).shipSizes).sum();
        
        return countHits(playerId) / (double)totalHealth;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        BattleshipParameters params = (BattleshipParameters) getGameParameters();
        return getGameScore(playerId) + (getClusteringBonus(playerId) * params.heuristicWeight * 0.5);
    }

    private double getClusteringBonus(int playerId) {
        GridBoard myShots = (playerId == 0) ? player0ShotGrid : player1ShotGrid;
        
        int connectedHits = 0;
        int w = myShots.getWidth();
        int h = myShots.getHeight();

        // Visiting each cell to count adjacent HITs, which can indicate potential ship locations and thus provide a bonus to the heuristic score
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (((BoardNode)myShots.getElement(x, y)).getComponentName().equals(BattleshipConstants.HIT)) {
                    if (x + 1 < w && ((BoardNode)myShots.getElement(x+1, y)).getComponentName().equals(BattleshipConstants.HIT)) {
                        connectedHits++;
                    }
                    if (y + 1 < h && ((BoardNode)myShots.getElement(x, y+1)).getComponentName().equals(BattleshipConstants.HIT)) {
                        connectedHits++;
                    }
                }
            }
        }
        return Math.min(connectedHits, 5) / 5.0;
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
        System.out.println("========================================");
    }

    public static void resetPerformanceMetrics() {
        for (int i = 0; i < 2; i++) {
            totalFMCALLS[i].set(0);
            totalTimeInCopy[i].set(0);
            attemptsPerSolve[i].set(0);
            fallbackCount[i].set(0);
        }
    }
}