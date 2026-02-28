package games.battleship_best_old.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import games.battleship_best_old.BattleshipConstants;
import games.battleship_best_old.BattleshipGameState;

import java.util.Objects;

/**
 * Represents the action of firing a shot at specific coordinates.
 * This action updates the grids and decrements the opponent's health points (HP) upon a hit.
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
     * Executes the shot logic. 
     * If a ship is hit, the opponent's HP in {@link BattleshipGameState#playerHP} is decremented.
     * @param gs The current game state.
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
            // Handle HIT: Update grids and decrement opponent health
            myShots.setElement(x, y, new BoardNode(BattleshipConstants.HIT));
            opponentShips.setElement(x, y, new BoardNode(BattleshipConstants.HIT));

            // Efficiency improvement: decrement HP counter to avoid O(N^2) grid scans in ForwardModel
            bgs.playerHP[opponentID]--;

        } else { 
            // Handle MISS: Mark the tracking grid
            myShots.setElement(x, y, new BoardNode(BattleshipConstants.MISS));

            // Only update the opponent's grid if it was water to avoid overwriting state
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