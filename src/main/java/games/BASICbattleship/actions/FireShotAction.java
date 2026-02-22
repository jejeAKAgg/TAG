package games.BASICbattleship.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import games.BASICbattleship.BattleshipConstants;
import games.BASICbattleship.BattleshipGameState;

import java.util.Objects;

/**
 * Represents the action of firing a shot at specific coordinates on the opponent's grid.
 * This action updates both the current player's shot tracking grid and the opponent's ship grid.
 */

public class FireShotAction extends AbstractAction {
    
    public final int x;
    public final int y;

    /**
     * Constructor for the fire action.
     * @param x Horizontal coordinate (0 to width-1)
     * @param y Vertical coordinate (0 to height-1)
     */
    
    public FireShotAction(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Executes the shot logic: checks for a hit/miss, updates grids, and modifies game state.
     * @param gs The current game state (must be an instance of {@link BattleshipGameState})
     * @return true if the action was successfully executed.
     */
    
    @Override
    public boolean execute(AbstractGameState gs) {
        if (!(gs instanceof BattleshipGameState)) return false;
        
        BattleshipGameState bgs = (BattleshipGameState) gs;
        int playerID = bgs.getCurrentPlayer();
        int opponentID = 1 - playerID;

        // Determine which grids to update based on the active player
        GridBoard myShots = (playerID == 0) ? bgs.player0ShotGrid : bgs.player1ShotGrid;
        GridBoard opponentShips = (opponentID == 0) ? bgs.player0ShipGrid : bgs.player1ShipGrid;

        BoardNode targetCell = (BoardNode) opponentShips.getElement(x, y);
        String targetName = targetCell.getComponentName();

        if (targetName.equals(BattleshipConstants.SHIP)) { 
            // Handle HIT: Update both the player's tracking grid and opponent's physical grid
            myShots.setElement(x, y, new BoardNode(BattleshipConstants.HIT));
            opponentShips.setElement(x, y, new BoardNode(BattleshipConstants.HIT));

            //System.out.println("Player " + playerID + " hit at " + x + "," + y); [DEBUG]
        } else { 
            // Handle MISS: Mark the tracking grid
            myShots.setElement(x, y, new BoardNode(BattleshipConstants.MISS));

            // Only update the opponent's grid if it was previously water (prevents overwriting existing hits)
            if (targetName.equals(BattleshipConstants.WATER)) {
                 opponentShips.setElement(x, y, new BoardNode(BattleshipConstants.MISS));
            }
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new FireShotAction(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireShotAction that = (FireShotAction) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Fire at (%d, %d)", x, y);
    }
}