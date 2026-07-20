// Author: Othmane
package Cli;

/**
 * A bad command line. Thrown instead of calling {@code System.exit} from deep
 * inside parsing, so {@link Main} owns every exit code in one place and the
 * parser stays a pure function that tests can call.
 *
 * @author Othmane
 */
public class UsageException extends Exception {

    public UsageException(String message) {
        super(message);
    }
}
