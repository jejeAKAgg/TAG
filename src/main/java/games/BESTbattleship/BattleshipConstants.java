package games.BESTbattleship;

import java.util.ArrayList;
import core.components.BoardNode;

public class BattleshipConstants {
    public static final String WATER = ".";
    public static final String SHIP = "S";
    public static final String MISS = "M";
    public static final String HIT = "H";
    
    public static final ArrayList<BoardNode> terrain = new ArrayList<BoardNode>() {{
        add(new BoardNode(WATER));
        add(new BoardNode(SHIP));
        add(new BoardNode(MISS));
        add(new BoardNode(HIT));
    }};
}