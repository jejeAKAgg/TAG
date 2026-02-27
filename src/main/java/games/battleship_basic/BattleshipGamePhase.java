package games.battleship_basic;

import core.interfaces.IGamePhase;

public enum BattleshipGamePhase implements IGamePhase {
    SETUP, // Unused (ships are placed automatically in a random manner)
    PLAY
}