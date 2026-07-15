package Tools;

/**
 * A single swap of two positions in a {@code short[]}, used to build
 * permutation orders from a password. Out-of-range or identical indices are
 * ignored.
 *
 * @author Othmane
 */
public class ExchangeMove {

    private final int index1;
    private final int index2;

    /**
     * @param a first index to swap
     * @param b second index to swap
     */
    public ExchangeMove(int a, int b) {
        index1 = a;
        index2 = b;
    }

    /**
     * Swaps {@code ord[index1]} and {@code ord[index2]} in place. Does nothing
     * if either index is out of bounds or the two indices are equal.
     *
     * @param ord array to mutate
     */
    public void perform(short[] ord) {
        if (index1 < ord.length && index2 < ord.length && index1 != index2) {
            short aux = ord[index1];
            ord[index1] = ord[index2];
            ord[index2] = aux;
        }
    }
}
