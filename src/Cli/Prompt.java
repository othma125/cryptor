// Author: Othmane
package Cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * One interactive question, holding the text it asks. Reads from the console
 * when there is one and from stdin when the run is piped.
 *
 * <p>
 * The password is never taken as an argument: a command-line password leaks
 * into shell history and the process list.
 *
 * @author Othmane
 */
final class Prompt {

    private final String text;

    Prompt(String text) {
        this.text = text;
    }

    /**
     * Reads a password without echo.
     *
     * @return the password, or {@code null} if the user cancelled at the prompt
     * (Ctrl+C or EOF) — nothing has been written yet, so that is a clean abort
     * rather than an error
     */
    char[] readPassword() {
        try {
            if (System.console() != null)
                return System.console().readPassword(this.text);
            System.out.print(this.text);
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            return line == null ? null : line.toCharArray();
        } catch (IOException cancelledAtPrompt) {
            return null;
        }
    }

    /**
     * Reads a yes/no answer for the destructive {@code -d} question. Only an
     * explicit {@code y}/{@code yes} (any case) is a yes; EOF or anything else
     * is a no, so a piped run never deletes by accident.
     */
    boolean confirm() {
        try {
            String line;
            if (System.console() != null)
                line = System.console().readLine(this.text);
            else {
                System.err.print(this.text);
                line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            }
            if (line == null)
                return false;
            line = line.trim().toLowerCase();
            return line.equals("y") || line.equals("yes");
        } catch (IOException e) {
            return false;
        }
    }
}
