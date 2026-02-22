package games.BASICbattleship;

import games.GameType;
import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.CoreConstants;
import core.components.Component;
import core.components.GridBoard;
import core.components.BoardNode;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;

import java.util.ArrayList;
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
        return GameType.BASICBattleship;
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
            
            // Player view: Hidden information logic
            // Initialize empty grids for the opponent's side
            copy.player0ShipGrid = new GridBoard(player0ShipGrid.getWidth(), player0ShipGrid.getHeight(), new BoardNode(BattleshipConstants.WATER));
            copy.player1ShipGrid = new GridBoard(player1ShipGrid.getWidth(), player1ShipGrid.getHeight(), new BoardNode(BattleshipConstants.WATER));

            if (playerId == 0) {
                
                // Player 0 sees their own ships but must "guess" (randomize) Player 1's ships
                copy.player0ShipGrid = this.player0ShipGrid.copy(); 
                copy.randomizeGrid(copy.player1ShipGrid, copy.rnd); 
            } else {
                
                // Player 1 sees their own ships but must randomize Player 0's ships
                copy.player1ShipGrid = this.player1ShipGrid.copy();
                copy.randomizeGrid(copy.player0ShipGrid, copy.rnd); 
            }
        }

        // Update performance metrics
        totalFMCALLS.incrementAndGet();
        totalTimeInCopy.addAndGet(System.nanoTime() - startTime);

        return copy;
    }

    /**
     * Resets a grid to all WATER nodes.
     */

    private void resetGrid(GridBoard grid) {
        for(int x=0; x<grid.getWidth(); x++)
            for(int y=0; y<grid.getHeight(); y++)
                grid.setElement(x, y, new BoardNode(BattleshipConstants.WATER));
    }

    /**
     * Checks if a ship of a given size can be placed at specific coordinates.
     */

    private boolean canPlaceShip(GridBoard grid, int x, int y, int size, boolean horizontal) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        if (horizontal) {
            if (x < 0 || x + size > width || y < 0 || y >= height) return false;
            for (int i = 0; i < size; i++) {
                if (!((BoardNode)grid.getElement(x + i, y)).getComponentName().equals(BattleshipConstants.WATER)) return false;
            }
        } else {
            if (y < 0 || y + size > height || x < 0 || x >= width) return false;
            for (int i = 0; i < size; i++) {
                if (!((BoardNode)grid.getElement(x, y + i)).getComponentName().equals(BattleshipConstants.WATER)) return false;
            }
        }
        return true;
    }

    /**
     * Randomly places ships on the provided grid based on the game parameters.
     * Uses a retry loop to ensure all ships are successfully placed.
     */

    public void randomizeGrid(GridBoard grid, Random r) {
        BattleshipParameters params = (BattleshipParameters) getGameParameters();
        boolean generationComplete = false;
        
        while (!generationComplete) {
            resetGrid(grid);
            generationComplete = true; 

            for (int shipSize : params.shipSizes) {
                boolean placed = false;
                int attempts = 0; 
                
                while (!placed && attempts < 100) {
                    int x = r.nextInt(grid.getWidth());
                    int y = r.nextInt(grid.getHeight());
                    boolean horizontal = r.nextBoolean();
                    
                    if (canPlaceShip(grid, x, y, shipSize, horizontal)) {
                        placeShip(grid, x, y, shipSize, horizontal);
                        placed = true;
                    }
                    attempts++;
                }

                if (!placed) {
                    generationComplete = false; 
                    break; // Restart whole grid if placement fails
                }
            }
        }
    }

    /**
     * Writes the SHIP component name to the specified grid coordinates.
     */

    private void placeShip(GridBoard grid, int x, int y, int size, boolean horizontal) {
        if (horizontal) {
            for (int i = 0; i < size; i++) {
                ((BoardNode)grid.getElement(x + i, y)).setComponentName(BattleshipConstants.SHIP);
            }
        } else {
            for (int i = 0; i < size; i++) {
                ((BoardNode)grid.getElement(x, y + i)).setComponentName(BattleshipConstants.SHIP);
            }
        }
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

    /**
     * Counts the total number of HIT markers on the player's shot tracking grid.
     */

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
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
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
        System.out.println("Battleship Game State");
    }

    @Override
    public GridBoard getGridBoard() {
        return player0ShotGrid;
    }

    public static void resetPerformanceMetrics() {
        totalFMCALLS.set(0);
        totalTimeInCopy.set(0);
    }
}