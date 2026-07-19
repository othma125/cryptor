# Cryptor

Cryptor is a Java Swing desktop application that encrypts any file into a password-protected `.cr` file, and decrypts it back to the original. It was first shared [on LinkedIn](https://www.linkedin.com/feed/update/urn:li:activity:6471397156392697856/).

**Runs on:** Windows, Linux and macOS — output paths use the platform file separator. The bundled `dist/Cryptor.exe` is Windows-only; elsewhere (and for scripting) run the jar or the [command-line mode](#running).

> ⚠️ **Cryptor uses a custom, unaudited cipher. It is an educational project, not a replacement for AES.** Don't use it to protect anything you'd mind losing. Several real weaknesses have since been fixed — see [Design analysis](#design-analysis) — but that does not make it trustworthy, and [Why the warning still stands](#why-the-warning-still-stands) explains why not.

## Features

- **Encrypt any file** — produces a `<name>.cr` file next to the source, embedding the original file name. *Advantage:* decryption restores the exact original name and bytes, nothing to remember.
- **PBKDF2 key derivation** — the password is stretched with PBKDF2-HMAC-SHA256 (100k iterations) into the key number. *Advantage:* brute-force and dictionary guessing cost 100k hashes per attempt instead of one, and the uniform digest spreads keys evenly over the key schedule.
- **Per-file random salt** — a fresh 16-byte salt is generated per encryption and stored in the clear at the head of the `.cr`. *Advantage:* the same file under the same password encrypts differently every time, so precomputed tables are useless and identical files can't be spotted by comparing ciphertexts.
- **Counter-mixed CBC chaining** — each grid byte is XORed with the previous ciphertext byte *and* the byte's position before it goes through the substitution (`c = S[V ⊕ prev ⊕ ctr]`). *Advantage:* one changed plaintext bit rewrites ~97% of the ciphertext bytes (48.9% of its bits, ideal 50%) after it, instead of a change that heals a few bytes later — and, unlike a plain `S[V] ⊕ prev`, constant plaintext cannot collapse into a repeating pattern.
- **Per-block rekeying** — the 256-byte substitution table is rebuilt from a fresh `SHA-256(masterKey ‖ block)` sub-key every 64 ciphertext bytes. *Advantage:* no single table governs more than 64 bytes, so a frequency attack gets 64 samples per table instead of the whole file. Note this did *not* fix the constant-plaintext leak it was originally credited with fixing — the counter in the chaining step did; see [Design analysis](#design-analysis).
- **Encrypt-then-MAC integrity** — an HMAC-SHA256 tag over the salt and the whole ciphertext is appended to the `.cr`, keyed separately from the substitution via `SHA-256(masterKey ‖ "mac")`. *Advantage:* a tampered or corrupted file is caught instead of decrypting to silent garbage, and the tag is checked *before* any of the file is interpreted, so a forged `.cr` never reaches the header decoder.
- **Exact wrong-password detection** — a wrong password derives a different MAC key, so the tag cannot match. *Advantage:* a typo reports itself rather than writing a broken file over your data — and unlike the old header-parse check, the answer is conclusive rather than probabilistic (that check is kept as a second line behind the MAC).
- **No default password** — encryption refuses an empty password rather than falling back to a built-in one. *Advantage:* nothing is ever encrypted under a key that anyone with a copy of the program already knows.
- **Background processing** — encryption/decryption runs on worker threads with a progress bar and cancel support. *Advantage:* the UI stays responsive and a long run on a big file can be stopped.
- **Streamed I/O** — the file moves through 1024-byte blocks and bounded queues, never fully into memory. *Advantage:* file size is limited by disk, not RAM.
- **Safety checks** — refuses to start without enough free disk space; can optionally open the output when done. *Advantage:* no half-written output from a full disk.
- **Command-line mode** — `cli encrypt|decrypt <file> [<file> ...]` runs headless, no GUI, prompting for the password without echo (once for a whole batch of files) and drawing a progress bar as it goes. A batch runs its files concurrently on a `ForkJoinPool` sized to the CPU. *Advantage:* scriptable and easy to fuzz/benchmark; multi-file runs spread across the available cores; it drives the *same* worker classes as the UI, so there is one cipher to trust, not two.
- **Cross-platform paths** — output paths are built with `File.separator` instead of a hardcoded `\`. *Advantage:* the jar and CLI run on Linux and macOS, not just Windows.

## How it works

The cipher is a custom bit-level substitution scheme:

1. A per-file random 16-byte salt is generated and the key number is derived with PBKDF2-HMAC-SHA256(password, salt) (`Encryption.Order.getPassword`). The salt is written in the clear at the head of the `.cr` so decryption can re-derive the key.
2. Two key-derived permutations are built: a 16-element order (grid step) and a 256-element order (byte substitution step).
3. Each source byte is processed bit by bit: runs of zeros/ones are mapped through a secret index matrix onto a 4×4 grid, and the resulting moves are re-packed into output bytes. Each packed byte `V` is then XORed with the previous ciphertext byte and with its own position counter, and *that* value indexes the 256-permutation: `c = S[V ⊕ prev ⊕ ctr]`. The XOR goes inside the table lookup rather than after it, which is what makes the chaining real (see [Design analysis](#design-analysis)); decryption inverts it as `V = S⁻¹[c] ⊕ prev ⊕ ctr`.
4. Every 64 ciphertext bytes the 256-permutation is rebuilt from a fresh `SHA-256(masterKey ‖ block)` sub-key (`Encryption.Order.subKey`), so the substitution is polyalphabetic across the file rather than one fixed table.
5. An HMAC-SHA256 tag over the salt and the whole ciphertext is appended last (`Encryption.Order.mac`), keyed by `SHA-256(masterKey ‖ "mac")` so the MAC key is separate from the substitution keys. This costs no second PBKDF2 stretch, and the 3-byte `"mac"` suffix cannot collide with `subKey`'s always-8-byte block index.
6. Decryption verifies that tag first: a mismatch means a wrong password or a tampered file, and nothing further is read. Only then does it undo the chaining and apply the inverse permutations to reverse both steps, rebuilding the same per-block substitution at the same 64-byte boundaries.

A `.cr` file is therefore laid out as `[salt: 16][ciphertext][tag: 32]`. The tag goes last so encryption never has to seek backwards to patch a placeholder, and verifying it up front costs one extra read pass — worth it, because the header decoder resolves and deletes a path built from the bytes it decodes, and must never see unverified input.

Because the two directions differ only in *which* permutation maps each grid index and *whether* the output byte is substituted, both share one implementation: the abstract `Senario` base holds the common bit-packing loops, and `EncryptingSenario`/`DecryptingSenario` supply just those two direction-specific hooks.

Reading and writing are pipelined: `FileReader`/`FileWriter` stream the file in 1024-byte blocks through bounded queues, so large files are handled without loading them into memory.

## Design analysis

### The idea

The construction is genuinely original: password → two permutations → a bit-repacking substitution. The bitstream of each byte is decomposed into runs of zeros/ones, each run is mapped through a fixed index matrix onto a 4×4 grid, and the resulting grid "moves" are re-packed into output bytes. The only password-derived secrets are a **16-element permutation** (grid step) and a **256-element permutation** (byte substitution). As a puzzle/learning cipher the grid-move encoding is a neat idea.

### Cryptographic assessment

The salt, PBKDF2, counter-mixed chaining, and the MAC listed under [Features](#features) close the precomputation, cheap-brute-force, diffusion, repetition, and integrity holes — `test/CryptanalysisTest.java` reports **survived** on all seven tests. The ✅ items below were the fatal holes and how they were closed. What remains open is listed under [Known weaknesses](#known-weaknesses), and it is the reason the warning at the top of this README is not a formality.

- **✅ Diffusion (fixed by chaining).** The core transform is *not* a fixed block→block map: run-length state (`previous`, `zeroCounter`) crosses byte boundaries and variable-length grid moves leave output unaligned to input. On its own that state is a tiny finite-state transducer (`n=4`, `m=3` → ~160 reachable states), so its diffusion is bounded and self-healing. A CBC-style chaining step (XOR each output byte with the previous ciphertext byte) lifts this to *forward* diffusion — flipping one early plaintext bit changes ~97% of the downstream ciphertext (`CryptanalysisTest` test [1]).
- **✅ Periodic on constant plaintext (fixed by the counter, *not* by rekeying).** The 256-permutation used to be one table for the whole file, so classic frequency analysis applied to the repacked stream. Rekeying limits each table to one 64-byte block, and for two commits this README credited it with fixing the constant-plaintext leak as well. **It never did.** The bug was in the *order* of operations: `c = S[V] ⊕ prev` substitutes and then XORs, so for constant plaintext — which packs to a constant grid byte `V` — the value `s = S[V]` is constant within a block, and XORing a constant into a running value merely toggles it: `c₂ = s ⊕ c₁ = c₀`. Period 2. A fresh table per block changes `s` every 64 bytes (which is why entropy read ~4.9 rather than ~1) but never touches the alternation inside a block. The fix moves the XOR *inside* the lookup and mixes in the byte position: `c = S[V ⊕ prev ⊕ ctr]`. Both halves are needed — `S[V ⊕ prev]` alone is a permutation of `prev`, so iterating it walks a cycle that is short by chance for some keys, and the counter is what varies the map per byte so no fixed cycle exists. 64 KB of `0x00` now encrypts to **8.00 bits/byte, 256/256 distinct values, no period**.
- **Why the false claim survived two commits (worth knowing).** The leak was *key-dependent*, and test [2] used to sample exactly one key per run, because it encrypted with a fresh random salt. Sweeping 16 fixed salts against the pre-fix cipher shows the old chaining collapsing for **6/16 keys (38%)** — three of them to an exact 2-byte period at ~4.35 bits/byte — while the other 62% look fine. So a single run had a ~62% chance of printing a healthy number, and whoever recorded "~7.95 bits/byte, no detectable period" almost certainly ran it once and got a good salt. The same sweep against the counter-mixed cipher collapses **0/16 keys**, worst-case entropy 7.97 (7.98 is the finite-sample ceiling at that size). Test [2] now sweeps fixed keys itself, so the verdict is reproducible instead of a coin flip. The lesson generalises past this bug: for a per-file-randomised cipher, one sample of a key-dependent property is an anecdote, not a measurement.
- **✅ Integrity and password verification (fixed by encrypt-then-MAC).** The old "wrong-password detection" was not integrity: it only checked that the header parsed and the terminator lined up. That made it *probabilistic* — random bytes from a wrong password hit that condition often enough to slip through — and blind to the body, so a tampered `.cr` decrypted to silent garbage. An HMAC-SHA256 tag over salt + ciphertext is now appended and verified before any decoding, keyed separately from the substitution, and compared with `MessageDigest.isEqual` (constant-time, so a forgery can't be steered by timing). A wrong password derives a different MAC key, so detection is now exact rather than lucky. Re-running the suite with the tag check disabled makes the point: the header sniff alone lets 1 of the 32 wrong-password cases through, and misses a flipped body bit entirely (`RoundTripTest` covers both).
- **✅ Hard-coded default password (dropped).** The UI used to substitute the literal `"password"` when both password fields were left empty — a publicly known key, which PBKDF2 cannot help with: such a file falls in seconds. Encryption now refuses an empty password outright, and decryption needs no special case for one, since it simply fails the MAC.
### Known weaknesses

These are open. None is closed by the fixes above, and the first one is the important one.

- **The confidentiality core is unaudited, and that is not a disclaimer — it is the actual risk.** Every primitive here that is known-good was borrowed: PBKDF2, HMAC-SHA256, `SecureRandom`, `MessageDigest.isEqual`. They handle key derivation, integrity and randomness. Nothing standard does the *encrypting* — that is the grid scheme, invented here and reviewed by no cryptographer. A cipher is not trustworthy because no one present can break it; it is trustworthy after people whose job is breaking ciphers have tried for years and failed. That has not happened to this one, and no amount of work in this repo can substitute for it.
- **The chain state is 8 bits wide.** `prevCipher` is a single byte, so everything a ciphertext byte carries forward about all prior plaintext is squeezed through 256 possible values (plus ~160 run-length transducer states and the block index). AES-CBC chains 128 bits. A byte-wide chain means chain-state collisions are routine rather than astronomically rare, and any attack that wants to force a chain-state repeat starts from a very small search. The counter widens the *map* per byte, not the state.
- **The grid stage contributes no key-dependent mixing.** `indexMatrix`, `binaryCoding` and `pairs` are fixed constants compiled into the program, so an attacker knows the entire grid transform exactly. All secrecy rests on the two permutations and the chaining — the bit-repacking, which is the visually impressive half of the design, is public and adds obscurity, not strength.
- **Small effective keyspace on the 16-step — now demonstrated.** The allowed swaps are constrained by a secret matrix, leaving exactly **138,240 ≈ 2¹⁷** possibilities for that permutation rather than 16!. A strong KDF can't lift this — it's a structural ceiling of the cipher, not of the key derivation. What was a sketch here is now `CryptanalysisTest` test [C]: under known plaintext it enumerates all 138,240 grid orders and tests each for consistency — within a 64-byte block, the packed output byte maps through `idx = V ⊕ prev ⊕ ctr` to the observed ciphertext, and that map must stay a partial permutation; a wrong order collides. **~320 known ciphertext bytes narrow 138,240 candidates to the one true order.** This does not hand over the plaintext — each block's 256-table is fresh, and 64 samples is thin for frequency analysis — but it peels a whole secret layer off with a few hundred known bytes and a fraction of a second of compute.
- **Structural attacks are only partly explored.** `CryptanalysisTest` [1]–[7] measure distributions (avalanche, periodicity, uniformity, correlation); [A]–[C] add the first *structural* results — the 16-step keyspace count, the KDF guessing cost, and the known-plaintext grid-order recovery above. But the attacks most likely to actually break the confidentiality core are still untried: differential and linear cryptanalysis, algebraic recovery, and known-plaintext recovery of the *master key* or the per-block 256-tables. The grid-order break is a real one, but it is the *easiest* layer; the suite is still silent on the hard ones.
- **The chaining IV is fixed at 0.** `prevCipher` starts at 0 every file. This is mostly covered by the per-file salt, which makes the *keys* fresh per encryption, so two files never share a keystream — but the guarantee rests entirely on the salt, with no independent IV behind it.
- **PBKDF2 at 100k iterations is adequate, not strong.** It is GPU- and ASIC-friendly by design; a memory-hard KDF (Argon2id, scrypt) buys far more per unit of user-visible delay. This only sets the cost of guessing a *weak* password, and against a modern cracking rig 100k SHA-256 iterations is a speed bump.

### Why the warning still stands

None of the work above changed any of this:

- **Smoke tests prove a floor, not a ceiling.** Avalanche, periodicity and randomization catch *gross* failure. A cipher that fails them is certainly broken; one that passes them has cleared a very low bar — ROT13 with a chaining step would pass test [1]. `CryptanalysisTest`'s verdict reads "survived these tests" deliberately: it is a statement about the tests, not about the cipher.
- **Closing known holes says nothing about unknown ones.** Every weakness fixed above was one already written down in this README. Removing them moved the cipher from "demonstrably broken" to "not broken by the seven checks in `CryptanalysisTest`". Essentially all of cryptography lives in the gap between that and "secure".
- **The suite was wrong once already, in the reassuring direction.** For two commits this README reported that per-block rekeying had fixed the constant-plaintext leak, with a measured number attached. It had not, and the number came from a single run of a test that samples one key. That error was caught only because the suite was re-run and questioned — not by anything structural. Treat a passing verdict here as a claim that has so far survived, not as a fact.

If actual security is ever a goal, the boring answer wins: `javax.crypto` with **AES-GCM + PBKDF2/Argon2**, with this grid scheme as an optional fun layer on top.

### Complexity

This part is solid:

- **Time: O(n)** in the file size — each byte costs a bounded number of bit operations; the 256-substitution is rebuilt every 64 bytes (an O(256) build, plus an O(256²) inverse on the decrypt side), which is a constant per-byte overhead. Decryption reads the file twice (once to verify the MAC, once to decode), which is a factor of 2, not a change in order.
- **Space: O(1)** in the file size — the `FileReader`/`FileWriter` stream in 1024-byte blocks through bounded queues, so arbitrarily large files work without loading them into memory.
- `Order.Inverse` is O(n²) on n ≤ 256; on decrypt it now runs once per 64-byte block rather than once per file, still a small constant per byte.

## Project layout

| Path | Description |
|---|---|
| `src/Main.java` | Entry point; sets the system look-and-feel and opens the main window |
| `src/cli.java` | Headless command-line front end (`encrypt`/`decrypt`), driving the same workers as the GUI |
| `src/MainFrame.java` | Swing UI: Encrypt / Decrypt / About tabs, file chooser, progress, dialogs |
| `src/Encryption/` | The cipher and I/O engine (`Encryption` package, see below) |
| `src/Encryption/Senario.java` | Abstract base for both workers; holds the shared bit-packing state and the encode/decode logic that is identical in both directions |
| `src/Encryption/EncryptingSenario.java` | Encryption worker (file → `.cr`); extends `Senario` |
| `src/Encryption/DecryptingSenario.java` | Decryption worker (`.cr` → original file), with MAC verification and wrong-password detection; extends `Senario` |
| `src/Encryption/BlockIO.java` | Abstract base for both I/O workers; holds the bounded block queue, the EOF sentinel and the cancellation plumbing they share |
| `src/Encryption/FileReader.java` / `src/Encryption/FileWriter.java` | Threaded block I/O with progress and cancellation; extend `BlockIO` |
| `src/Encryption/Order.java` | Password-derived permutations, PBKDF2 key derivation, per-block sub-keys and the MAC key |
| `src/Tools/` | `InputParameters` (cipher config and lookup tables), `ExchangeMove` (swaps) |
| `InputParameters` | Required data file, loaded at startup from the working directory |
| `test/RoundTripTest.java` | Standalone encrypt → decrypt round-trip check |
| `test/CryptanalysisTest.java` | Cryptanalysis smoke tests [1]–[7] (avalanche, periodicity, uniformity, correlation) plus attacks [A]–[C] (16-step keyspace, KDF cost, known-plaintext grid-order recovery) |
| `test/BenchmarkTest.java` | Encrypt/decrypt throughput benchmark across file sizes |
| `dist/` | Build output produced by `ant jar` (`Cryptor.jar` / `Cryptor.exe`); git-ignored |

## Running

Build the artifacts first (see [Building](#building)), then run the jar:

```
ant jar
java -jar dist/Cryptor.jar
```

On Windows you can instead run the generated `dist/Cryptor.exe`.

> **Note:** the `InputParameters` file must be present in the working directory, otherwise the app blocks encryption/decryption.

### Command-line mode

For scripting or headless use, `cli` encrypts/decrypts without the GUI:

```
javac -d out src/*.java src/Encryption/*.java src/Tools/*.java
java -cp out cli encrypt path/to/file [more files ...]   # prompts for the password
java -cp out cli decrypt path/to/file.cr [more .cr ...]  # prompts for the password
```

Several files can be given in one call (handy for a drag-and-drop selection): the password is asked once and applied to the whole batch, and a file that fails — missing, wrong password, no free space — is reported and skipped while the rest continue, with a non-zero exit if any failed.

A batch is processed in parallel on a `ForkJoinPool` sized to the number of CPU cores — but never more than three files at once, because each file drives three worker threads (crypto + reader + writer) out of a shared pool of ten, and a fourth concurrent file could starve a reader and deadlock. A single progress bar on stderr shows the mean progress across the batch (the same 0–100 the GUI shows for one file), and the per-file results are printed together once it fills, so they never tear the bar mid-draw.

The password is always read interactively, never taken as an argument — a command-line password leaks into shell history and the process list. It is read without echo from the console (or from stdin when piped). Encryption prompts twice and requires a match, refuses an empty password, and a wrong password or tampered file exits non-zero with an error.

Pressing **Ctrl+C** during a run is the same operation as the GUI's Cancel button: a shutdown hook calls `Cancel()` on every file still in flight and waits for each to remove its partial output, so an interrupted run never leaves a half-written `.cr` (or a half-written decrypted file) behind. Ctrl+C *at the password prompt* — before any file is touched — just prints `cancelled` and exits, with no stack trace.

## Building

This is a NetBeans/Ant project:

```
ant jar
```

## Testing

Run the round-trip test from the project root (so the `InputParameters` file is found):

```
javac -d out src/*.java src/Encryption/*.java src/Tools/*.java test/*.java
java -ea -cp out RoundTripTest
```

It encrypts and decrypts random files of various sizes with several passwords (including empty and very long ones), asserts byte-for-byte equality, checks that a wrong password is detected, that a tampered `.cr` is rejected under the *right* password with nothing written, that a cancelled run leaves no partial output, and that encryption is randomized per run (the per-file salt makes the same file + password differ every time).

The empty-password case covers the engine, which still derives a key from one; it is the UI that refuses to encrypt without a password.

To reproduce the [Design analysis](#design-analysis) numbers, run the cryptanalysis smoke test the same way:

```
java -ea -cp out CryptanalysisTest
```

Each of tests [1]–[7] prints a `BROKEN`/`survived` verdict. Tests [1]–[3] cover diffusion, constant-input periodicity and randomization; [4]–[7] are the standard statistical battery: bit-level avalanche, key avalanche, uniformity (NIST-style monobit + byte chi-square) and serial correlation. After the verdict, tests [A]–[C] print the *attacks* from [ALGORITHM_DIRECTIONS.md](ALGORITHM_DIRECTIONS.md): [A] the exact 16-step keyspace, [B] the measured PBKDF2 guessing cost, and [C] a known-plaintext recovery of the grid order (run with `-ea` — the attack's success doubles as its correctness check, asserting the true order is recovered).

To measure throughput, run the benchmark the same way (from the project root, no `-ea` needed):

```
java -cp out BenchmarkTest
```

It encrypts and decrypts 1/4/16 MB files and prints MB/s for each direction (decryption trails encryption because it reads the file twice — once to verify the MAC, once to decode).

### Latest results

Run on 2026-07-15 (Windows 10). `RoundTripTest` prints `OK: 35 round-trips.`; `CryptanalysisTest`:

| Test | Measured | Verdict |
|---|---|---|
| [1] Avalanche, bytes (1-bit flip in first 32 B, 30 trials, 2048 B each) | changed mean 97.4%, min 96.4%, max 98.3%; 0/30 self-healed; 0/30 shifted length | `ok` |
| [2] Constant input (8192 B of `0x00`, swept over 8 fixed keys) | worst-case entropy 7.98 bits/byte (baseline 7.98); 0/8 keys leaked; no period at any key | `ok` |
| [3] Randomization (per-file salt) | same file + password → identical ciphertext: false | `ok` |
| [4] Bit avalanche (1-bit plaintext flip, 30 trials, ideal 50%) | bits changed mean 48.9%, min 47.9%, max 49.6% | `ok` |
| [5] Key avalanche (1-bit password flip, same plaintext+salt, 16 trials) | bits changed mean 49.6%, min 48.8%, max 50.2% | `ok` |
| [6] Uniformity (65536 B random plaintext) | monobit 0.4998 (0.3 σ); byte chi-square 303.1 / 255 df | `ok` |
| [7] Serial correlation of adjacent bytes | +0.0046 (ideal 0.0000) | `ok` |

Overall: `VERDICT: survived these tests.` Test [2] went from leaking on 2/8 keys (worst 4.36 bits/byte, exact 2-byte period) to 0/8 when the chaining counter landed — see [Design analysis](#design-analysis). The entropy ceiling in [2] is 7.98, not 8.00, because 8192 samples over 256 bins cannot reach 8; [6] varies run to run (chi-square has been seen between ~226 and ~303 against a uniform expectation of 255 ± 22.6) since the salt is random.

Two notes on reading this table, both learned the hard way here:

- **The tests were checked against a known-broken cipher, not just a passing one.** A suite nobody has watched fail proves nothing. Pointed at the pre-counter cipher, test [2] reports `BROKEN` (2/8 keys, worst 4.36 bits/byte) and so does test [4] — bits changed swing from 24.8% to 85.1%, a mean of 50.5% that hides two useless extremes. That last one is precisely what test [1] misses: byte-level counting saturates near 97% and cannot tell a good cipher from a mediocre one, which is why [4] exists alongside it. Tests [2] and [4] therefore have demonstrated teeth; [5]–[7] pass on both ciphers and so are, for now, only guarding against regressions.
- **Test [2] sweeps fixed keys rather than encrypting once under a random salt.** The leak it hunts is key-dependent, so one random key per run turned a consistent break into a coin-flip verdict — and a single lucky run is exactly how the false claim got into this README. Fixed salts make the verdict reproducible; when a property depends on the key, sweep keys.

The same binary also prints the attack results (run 2026-07-18, single core):

| Attack | Result |
|---|---|
| [A] 16-step grid keyspace | 138,240 reachable orders (~2¹⁷·¹, vs 16! = 2⁴⁴·³) |
| [B] KDF guessing cost | ~122 ms/guess → ~8 guesses/sec; 26⁸ ≈ 808 years, 10M-word dictionary ≈ 14 days (single core; a GPU rig cuts these by orders of magnitude) |
| [C] Known-plaintext grid-order recovery | 320 known ciphertext bytes narrow 138,240 candidates to **1**; true order recovered |

[C] is the first structural break in the suite: it confirms the [small-keyspace weakness](#known-weaknesses) is real and cheap to exploit, while leaving the master key and per-block tables intact. See [ALGORITHM_DIRECTIONS.md](ALGORITHM_DIRECTIONS.md) for the attacks still untried.

Any suggestions to improve it are welcome.
