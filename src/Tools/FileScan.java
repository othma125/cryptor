// Author: Othmane
package Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Turns whatever the user pointed at — files, directories, or a mix — into the
 * flat list of files to work on. Shared by the CLI's path arguments and the
 * GUI's drag-and-drop, so a dropped folder and {@code -r} on the command line
 * pick the same files rather than drifting apart.
 *
 * @author Othmane
 */
public final class FileScan {

    private FileScan() {
    }

    /**
     * Expands {@code roots} into the files to work on. A directory yields the
     * files inside it — top-level only, or every subfolder when
     * {@code recursive} — keeping only those {@link #matchesMode} accepts. A
     * root that is already a file is taken as given, whatever its name; callers
     * that want those filtered too should apply {@link #matchesMode} to the
     * result.
     *
     * @param roots the files and directories the user pointed at
     * @param recursive walk directories into their subfolders
     * @param encrypting {@code true} when encrypting, which decides whether
     * {@code .cr} files are the ones wanted or the ones skipped
     * @return the expanded list, in the order the roots were given
     * @throws IOException if a directory cannot be walked
     */
    public static List<File> expand(Iterable<File> roots, boolean recursive, boolean encrypting) throws IOException {
        List<File> found = new ArrayList<>();
        for (File root : roots) {
            if (!root.isDirectory()) {
                found.add(root);
                continue;
            }
            if (recursive) {
                try (Stream<Path> walk = Files.walk(root.toPath())) {
                    walk.filter(Files::isRegularFile)
                            .map(Path::toFile)
                            .filter(f -> matchesMode(f, encrypting))
                            .forEach(found::add);
                }
            }
            else {
                File[] inside = root.listFiles(f -> f.isFile() && matchesMode(f, encrypting));
                if (inside != null)
                    Collections.addAll(found, inside);
            }
        }
        return found;
    }

    /**
     * Whether a file's name suits the direction: encryption wants everything
     * but the {@code .cr} files (so a second pass over a folder re-encrypts the
     * plaintext instead of stacking {@code .cr.cr}), decryption wants only them.
     */
    public static boolean matchesMode(File file, boolean encrypting) {
        return file.getName().endsWith(".cr") != encrypting;
    }
}
