package games.battleship_basic.gui;

import core.components.BoardNode;
import core.components.GridBoard;
import games.battleship_basic.BattleshipConstants;
import gui.views.ComponentView;
import gui.IScreenHighlight;

import java.awt.*;
import static gui.GUI.defaultItemSize;

/**
 * Visual representation of a Battleship grid board.
 * This class handles the rendering of cells including water, ships, hits, and misses.
 */
public class BattleshipBoardView extends ComponentView implements IScreenHighlight {

    private GridBoard gridBoard;

    /**
     * Creates a new view for the specified grid board.
     * @param grid The GridBoard component to visualize.
     */
    public BattleshipBoardView(GridBoard grid) {
        super(grid, grid.getWidth() * defaultItemSize, grid.getHeight() * defaultItemSize);
        this.gridBoard = grid;
    }

    /**
     * Updates the underlying data of the view.
     * @param grid The updated GridBoard state.
     */
    public void updateComponent(GridBoard grid) {
        this.gridBoard = grid;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, gridBoard, 0, 0);
    }

    /**
     * Iterates through the grid and renders each cell.
     * @param g The graphics context.
     * @param grid The grid to draw.
     * @param x Starting X coordinate.
     * @param y Starting Y coordinate.
     */
    public void drawGridBoard(Graphics2D g, GridBoard grid, int x, int y) {
        int width = grid.getWidth() * defaultItemSize;
        int height = grid.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width, height);

        for (int i = 0; i < grid.getHeight(); i++) {
            for (int j = 0; j < grid.getWidth(); j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, (BoardNode) grid.getElement(j, i), xC, yC);
            }
        }
    }

    /**
     * Renders an individual cell based on its state (SHIP, WATER, HIT, MISS).
     */
    private void drawCell(Graphics2D g, BoardNode element, int x, int y) {
        
        // Draw cell border
        g.setColor(Color.black);
        g.drawRect(x, y, defaultItemSize, defaultItemSize);

        if (element != null) {
            String name = element.getComponentName();
            
            // Render specific colors and shapes based on cell type
            if (name.equals(BattleshipConstants.WATER)) {
                g.setColor(Color.CYAN);
                g.fillRect(x + 1, y + 1, defaultItemSize - 2, defaultItemSize - 2);
            } else if (name.equals(BattleshipConstants.MISS)) {
                g.setColor(Color.BLUE);
                g.fillOval(x + 5, y + 5, defaultItemSize - 10, defaultItemSize - 10);
            } else if (name.equals(BattleshipConstants.HIT)) {
                g.setColor(Color.RED);
                g.fillOval(x + 5, y + 5, defaultItemSize - 10, defaultItemSize - 10);
            } else if (name.equals(BattleshipConstants.SHIP)) {
                g.setColor(Color.GRAY);
                g.fillRect(x + 5, y + 5, defaultItemSize - 10, defaultItemSize - 10);
            }
            
            // Optional: Draw text label for debug/clarity
            g.setColor(Color.BLACK);
            g.drawString(name, x + 10, y + 20);
        }
    }

    @Override
    public void clearHighlights() {
        repaint();
    }
}