// Author: Othmane
package Cli;

import Encryption.DecryptingSenario;
import Encryption.EncryptingSenario;
import Encryption.Senario;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * One file's share of the batch: runs the same {@link EncryptingSenario}/
 * {@link DecryptingSenario} worker the GUI uses, feeds its progress into the
 * shared bar, and keeps its own outcome.
 *
 * <p>
 * The outcome lives on the task rather than in an array the caller passes in,
 * so nothing has to agree on an index to read a result back.
 *
 * @author Othmane
 */
final class FileTask implements Callable<Void> {

    private final File file;
    private final Options options;
    private final char[] password;
    private final Set<Senario> active;
    private final ProgressBar bar;
    private final int slot;

    private String result;
    private boolean succeeded;

    FileTask(File file, Options options, char[] password, Set<Senario> active, ProgressBar bar, int slot) {
        this.file = file;
        this.options = options;
        this.password = password;
        this.active = active;
        this.bar = bar;
        this.slot = slot;
    }

    /** The one-line report for this file, valid once {@link #call()} has returned. */
    String result() {
        return this.result;
    }

    boolean succeeded() {
        return this.succeeded;
    }

    @Override
    public Void call() throws Exception {
        this.succeeded = this.process();
        return null;
    }

    private boolean process() throws Exception {
        if (!this.file.isFile()) {
            this.result = "no such file: " + this.file;
            this.bar.set(this.slot, 100);   // count this slot as finished so the mean can still reach 100
            return false;
        }
        Senario sen = this.options.encrypting()
                ? new EncryptingSenario(this.file, this.password)
                : new DecryptingSenario(this.file, this.password, false);
        sen.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName()))
                this.bar.set(this.slot, (int) e.getNewValue());
        });
        this.active.add(sen);
        try {
            sen.execute();
            sen.get();
            this.bar.set(this.slot, 100);   // the writer stops at 99; mark the slot done once the worker returns
            if (sen.isCanceled())   // Ctrl+C: the hook prints the message and cleans up
                return false;
            return this.options.encrypting() ? this.reportEncrypted((EncryptingSenario) sen)
                                             : this.reportDecrypted((DecryptingSenario) sen);
        } finally {
            this.active.remove(sen);
        }
    }

    private boolean reportEncrypted(EncryptingSenario sen) {
        if (sen.NoEnoughFreeSpace()) {
            this.result = "not enough free space for: " + this.file.getName();
            return false;
        }
        this.result = "encrypted -> " + sen.OutputFile();   // ask the run: the name may carry a (n) collision suffix
        if (this.options.deleteOriginal)   // wipe the plaintext only once the .cr is safely written
            try {
                Tools.SecureDelete.wipe(this.file);
                this.result += "  (original deleted)";
            } catch (IOException e) {
                this.result += "  (could not delete original: " + e.getMessage() + ")";
            }
        return true;
    }

    private boolean reportDecrypted(DecryptingSenario sen) {
        if (sen.WrongPassword()) {
            this.result = "wrong password or corrupted/tampered file: " + this.file.getName();
            return false;
        }
        this.result = "decrypted " + this.file.getName();
        return true;
    }
}
