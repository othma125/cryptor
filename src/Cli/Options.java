// Author: Othmane
package Cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * One parsed command line: the mode, the flags, and the paths. Immutable once
 * built, so everything downstream reads settings instead of re-deriving them
 * from {@code args}.
 *
 * <p>
 * Parsing never exits or prints; a bad line raises {@link UsageException} and
 * {@link Main} decides what that costs.
 *
 * @author Othmane
 */
public final class Options {

    public static final String USAGE =
            "usage: java -cp out Cli.Main encrypt|decrypt [-r] [-d [-y]] <file|dir> [<file|dir> ...]";

    /** Which direction the batch runs in. */
    public enum Mode { ENCRYPT, DECRYPT }

    public final Mode mode;
    /** Walk directories into their subfolders ({@code -r}). */
    public final boolean recursive;
    /** Wipe each original once it is encrypted ({@code -d}, encrypt only). */
    public final boolean deleteOriginal;
    /** Skip the {@code -d} warning prompt ({@code -y}). */
    public final boolean assumeYes;
    private final List<String> paths;

    private Options(Mode mode, boolean recursive, boolean deleteOriginal, boolean assumeYes, List<String> paths) {
        this.mode = mode;
        this.recursive = recursive;
        this.deleteOriginal = deleteOriginal;
        this.assumeYes = assumeYes;
        this.paths = paths;
    }

    public boolean encrypting() {
        return this.mode == Mode.ENCRYPT;
    }

    /** The verb as typed, for messages like "no files to encrypt".
     * @return  */
    public String verb() {
        return this.encrypting() ? "encrypt" : "decrypt";
    }

    /**
     * Parses {@code encrypt|decrypt [flags] <path>...}. Leading flags come in any
     * order; short ones bundle the usual way, so {@code -rdy} is {@code -r -d -y}.
     * The first argument that does not start with {@code -} begins the paths.
     *
     * @param args
     * @return 
     * @throws UsageException if the mode, a flag, or the path list is wrong
     */
    public static Options parse(String[] args) throws UsageException {
        if (args.length < 2)
            throw new UsageException("");
        Mode mode;
        switch (args[0]) {
            case "encrypt": mode = Mode.ENCRYPT; break;
            case "decrypt": mode = Mode.DECRYPT; break;
            default: throw new UsageException("");
        }
        boolean recursive = false, deleteOriginal = false, assumeYes = false;
        int i = 1;
        for (; i < args.length && args[i].startsWith("-"); i++) {
            String opt = args[i];
            switch (opt) {
                case "--recursive": recursive = true; break;
                case "--delete": deleteOriginal = true; break;
                case "--yes": assumeYes = true; break;
                default:
                    if (opt.startsWith("--") || opt.length() < 2)
                        throw new UsageException("unknown option: " + opt);
                    for (char c : opt.substring(1).toCharArray())
                        switch (c) {
                            case 'r': recursive = true; break;
                            case 'd': deleteOriginal = true; break;
                            case 'y': assumeYes = true; break;
                            default: throw new UsageException("unknown option: -" + c);
                        }
            }
        }
        if (deleteOriginal && mode == Mode.DECRYPT)
            throw new UsageException("-d only applies to encrypt (decrypt never touches the .cr's original).");
        if (i >= args.length)
            throw new UsageException("");
        return new Options(mode, recursive, deleteOriginal, assumeYes,
                new ArrayList<>(java.util.Arrays.asList(args).subList(i, args.length)));
    }

    /**
     * Expands the path arguments into the files to work on. A directory yields
     * the files inside it (top-level only, or every subfolder with {@code -r});
     * encrypt takes every file but the {@code .cr} ones, so a second run over a
     * directory re-encrypts the plaintext rather than stacking {@code .cr.cr},
     * and decrypt takes only {@code .cr} files. An explicit file argument is
     * always taken as given, whatever its name.
     * @return 
     * @throws java.io.IOException
     */
    public File[] resolveFiles() throws IOException {
        List<File> found = new ArrayList<>();
        for (String path : this.paths) {
            File f = new File(path).getAbsoluteFile();   // absolute so getParent() is never null on a bare name
            if (!f.isDirectory()) {
                found.add(f);
                continue;
            }
            if (this.recursive) {
                try (Stream<Path> walk = Files.walk(f.toPath())) {
                    walk.filter(Files::isRegularFile)
                            .map(Path::toFile)
                            .filter(this::wanted)
                            .forEach(found::add);
                }
            }
            else {
                File[] inside = f.listFiles(c -> c.isFile() && this.wanted(c));
                if (inside != null)
                    Collections.addAll(found, inside);
            }
        }
        return found.toArray(new File[0]);
    }

    /** A file picked up from a directory is wanted if its name suits the mode. */
    private boolean wanted(File f) {
        return f.getName().endsWith(".cr") != this.encrypting();
    }
}
