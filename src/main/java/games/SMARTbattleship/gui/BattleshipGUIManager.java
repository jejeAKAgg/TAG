package games.SMARTbattleship.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.BASICbattleship.BattleshipGameState;
import games.BASICbattleship.BattleshipParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Manager for the Battleship Graphical User Interface.
 * Arranges the player's fleet (defense) and radar (attack) views.
 */
public class BattleshipGUIManager extends AbstractGUIManager {

    protected BattleshipBoardView playerFleetView;
    protected BattleshipBoardView playerRadarView;

    /**
     * Initializes the GUI manager and sets up the layout.
     * @param parent The main game panel.
     * @param game The game instance being played.
     * @param ac Controller for human player actions.
     * @param human Set of player IDs controlled by humans.
     */
    public BattleshipGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        BattleshipGameState gameState = (BattleshipGameState) game.getGameState();
        
        // Initialize views based on player 0's perspective
        playerFleetView = new BattleshipBoardView(gameState.player0ShipGrid);
        playerRadarView = new BattleshipBoardView(gameState.player0ShotGrid);

        // --- Fleet Panel Configuration (Left) ---
        JPanel fleetPanel = new JPanel();
        fleetPanel.setLayout(new BoxLayout(fleetPanel, BoxLayout.Y_AXIS));
        JLabel fleetTitle = new JLabel("My Fleet (Defense)");
        fleetTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        fleetPanel.add(fleetTitle);
        fleetPanel.add(playerFleetView);
        
        // --- Radar Panel Configuration (Right) ---
        JPanel radarPanel = new JPanel();
        radarPanel.setLayout(new BoxLayout(radarPanel, BoxLayout.Y_AXIS));
        JLabel radarTitle = new JLabel("My Radar (Attack)");
        radarTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        radarPanel.add(radarTitle);
        radarPanel.add(playerRadarView);

        // Calculate window dimensions based on grid sizes
        this.width = (playerFleetView.getPreferredSize().width * 2) + 60;
        this.height = playerFleetView.getPreferredSize().height + 200;

        // Assembly of the main view container
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        mainPanel.add(fleetPanel);
        mainPanel.add(radarPanel);

        // Top info panel (Turn count, player status, etc.)
        JPanel infoPanel = createGameStateInfoPanel("Battleship", gameState, width, defaultInfoPanelHeight);
        
        // Bottom action panel - Radar is highlighted as it's the interactive target for shots
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{playerRadarView},
                width, defaultActionPanelHeight, true);

        // Final parent container setup
        parent.setLayout(new BorderLayout());
        parent.add(mainPanel, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
    }

    /**
     * Defines the maximum number of actions that can be displayed at once.
     * For example, on a 10x10 grid, this is 100.
     */
    @Override
    public int getMaxActionSpace() {
        // We retrieve the grid size from the parameters to calculate total cells
        BattleshipParameters params = (BattleshipParameters) game.getGameState().getGameParameters();
        return params.gridSize * params.gridSize; 
    }

    /**
     * Updates the GUI components when the game state changes.
     * @param player The current player.
     * @param gameState The current state of the game.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            BattleshipGameState bgs = (BattleshipGameState) gameState;
            
            // Re-bind views to ensure the latest grid data is displayed
            playerFleetView.updateComponent(bgs.player0ShipGrid);
            playerRadarView.updateComponent(bgs.player0ShotGrid);
        }
        parent.repaint();
    }
}