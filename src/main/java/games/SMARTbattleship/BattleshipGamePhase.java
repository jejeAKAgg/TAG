package games.SMARTbattleship;

import core.interfaces.IGamePhase;

public enum BattleshipGamePhase implements IGamePhase {
    SETUP, // Unused (ships are placed automatically in a random manner)
    PLAY
}