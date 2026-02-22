package games.BASICbattleship;

import core.StandardForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import games.BASICbattleship.actions.FireShotAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Forward Model handles the game logic, transition rules, and action generation for Battleship.
 * This implementation uses an optimized HP-tracking system for fast win-condition checking.
 */
public class BattleshipForwardModel extends StandardForwardModel {

    /**
     * Initializes the game state. Sets up grids, calculates total HP based on ship sizes,
     * and performs random ship placement.
     * @param firstState The initial state to be configured.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        BattleshipGameState bgs = (BattleshipGameState) firstState;
        BattleshipParameters params = (BattleshipParameters) firstState.getGameParameters();
        
        // Calculate total HP (sum of all ship lengths) to initialize the HP counters
        int totalHP = 0;
        for (int size : params.shipSizes) totalHP += size;
        bgs.playerHP = new int[]{totalHP, totalHP};

        int gridSize = params.gridSize; 

        // Initialize Boards for both players with WATER nodes
        BoardNode waterNode = new BoardNode(BattleshipConstants.WATER);
        bgs.player0ShipGrid = new GridBoard(gridSize, gridSize, waterNode);
        bgs.player1ShipGrid = new GridBoard(gridSize, gridSize, waterNode);
        bgs.player0ShotGrid = new GridBoard(gridSize, gridSize, waterNode);
        bgs.player1ShotGrid = new GridBoard(gridSize, gridSize, waterNode);

        Random rand = bgs.getRnd();
        
        // Place ships randomly on the grids
        bgs.randomizeGrid(bgs.player0ShipGrid, rand);
        bgs.randomizeGrid(bgs.player1ShipGrid, rand);

        // Transition to the playing phase
        bgs.setGamePhase(BattleshipGamePhase.PLAY);
    }

    /**
     * Generates all valid FireShotActions for the current player.
     * Actions are only available for coordinates that have not been targeted yet.
     * @param gameState The current state of the game.
     * @return A list of available actions.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BattleshipGameState bgs = (BattleshipGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (bgs.getGamePhase() == BattleshipGamePhase.PLAY) {
            int player = bgs.getCurrentPlayer();
            GridBoard myShots = (player == 0) ? bgs.player0ShotGrid : bgs.player1ShotGrid;

            for (int x = 0; x < myShots.getWidth(); x++) {
                for (int y = 0; y < myShots.getHeight(); y++) {
                    BoardNode cell = (BoardNode) myShots.getElement(x, y);
                    
                    // Only target cells containing WATER (not MISS or HIT)
                    if (cell.getComponentName().equals(BattleshipConstants.WATER)) {
                        actions.add(new FireShotAction(x, y));
                    }
                }
            }
        }
        return actions;
    }

    /**
     * Handles game logic after an action is applied, such as checking for a winner
     * and rotating turns.
     * @param currentState The state after the action execution.
     * @param action The action that was executed.
     */
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        BattleshipGameState bgs = (BattleshipGameState) currentState;

        if (checkWin(bgs)) {
            int winner = bgs.getCurrentPlayer();
            int loser = 1 - winner;
            
            bgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, winner);
            bgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, loser);
            
            endGame(bgs);
        } else {
            endPlayerTurn(bgs);
        }
    }

    /**
     * Determines if the current player has won by checking if the opponent's HP has reached zero.
     * @param bgs The current game state.
     * @return true if the opponent has no remaining ship segments.
     */
    private boolean checkWin(BattleshipGameState bgs) {
        int opponentID = 1 - bgs.getCurrentPlayer();
        
        // Optimized O(1) check using the HP counter
        return bgs.playerHP[opponentID] == 0;
    }
}