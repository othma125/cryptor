// Author: Othmane
package Tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.SecureRandom;

/**
 * Best-effort "deep delete": overwrites a file's bytes with random data and
 * forces them to disk before unlinking, so the plaintext can't be scraped back
 * with an undelete tool. One random pass is enough — the multi-pass rituals were
 * for 1990s magnetic drives.
 *
 * <p>
 * <b>Caveat.</b> On an SSD (or any journaling/copy-on-write filesystem) the
 * overwrite is not guaranteed to land on the original blocks: wear-levelling
 * writes the random bytes to fresh cells and leaves the old plaintext cells for
 * the controller's garbage collector, out of any program's reach. This defeats
 * casual recovery, not forensic recovery on flash. The only real guarantee is to
 * never write the plaintext in the clear in the first place.
 *
 * @author Othmane
 */
public final class SecureDelete {

    private SecureDelete() {
    }

    /**
     * Overwrites {@code file} with random bytes, flushes to disk, then deletes
     * it.
     *
     * @param file the file to wipe and remove
     * @throws IOException if the overwrite or the unlink fails
     */
    public static void wipe(File file) throws IOException {
        long len = file.length();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {   // rws = write-through, no OS write cache
            byte[] buf = new byte[8192];
            SecureRandom rnd = new SecureRandom();
            for (long written = 0; written < len; written += buf.length) {
                rnd.nextBytes(buf);
                raf.write(buf, 0, (int) Math.min(buf.length, len - written));
            }
            raf.getFD().sync();   // force the overwrite to the platter before we unlink
        }
        Files.delete(file.toPath());   // throws on failure, unlike File.delete()'s silent boolean
    }
}
