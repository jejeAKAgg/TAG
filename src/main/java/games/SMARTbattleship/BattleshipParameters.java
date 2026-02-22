package games.SMARTbattleship;

import core.AbstractParameters;
import java.util.Arrays;
import java.util.Objects;

public class BattleshipParameters extends AbstractParameters {

    // Fixed size for simplicity
    public int gridSize = 10;
    
    // Fixed ship sizes for simplicity
    public int[] shipSizes = new int[]{5, 4, 3, 3, 2};

    public BattleshipParameters() {
        super();
        setRandomSeed(System.currentTimeMillis());
    }

    public BattleshipParameters(long seed) {
        super();
        setRandomSeed(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        BattleshipParameters copy = new BattleshipParameters(getRandomSeed());
        copy.gridSize = this.gridSize;
        copy.shipSizes = this.shipSizes.clone(); 
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BattleshipParameters that = (BattleshipParameters) o;
        return gridSize == that.gridSize && 
               Arrays.equals(shipSizes, that.shipSizes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), gridSize);
        result = 31 * result + Arrays.hashCode(shipSizes);
        return result;
    }
}