# What can I do with the algorithm?

Depends on what you want *out* of it. Here are the realistic directions, grouped by goal — with the concrete change and what it buys you.

## If the goal is: make it actually stronger (while keeping the grid idea)

These attack the specific weaknesses from the [Design analysis](README.md#design-analysis), roughly in bang-for-buck order:

1. **Add chaining/diffusion (biggest single win).** Right now it's ECB-like — identical input blocks encrypt identically. XOR each block with the previous ciphertext block (CBC-style feedback) before the grid stage. Kills the "repeated structure leaks" and "same file → same output" problems.
2. **Per-file salt + IV in the header.** Store 16 random bytes; mix them into the key schedule and the first block. Now the *same* file under the *same* password produces different `.cr` every time, and precomputation dies.
3. **Real KDF.** Replace `Order.getPassword` (raw base-256 number) with `PBKDF2` or `Argon2(password, salt)`. This alone removes the brute-force/dictionary ease *and* the "different passwords collide mod maxScales" issue.
4. **Rekey per block.** Derive fresh permutations every N bytes from the KDF stream so it stops being a single monoalphabetic table.
5. **Add a MAC (encrypt-then-MAC / HMAC).** Real integrity + genuine wrong-password/tamper detection, instead of "does the header happen to parse."
6. **Drop the blank-password default** — it's a publicly known key.

Do 1–3 and it goes from "toy" to "amateur but not embarrassing."

## If the goal is: securely encrypt real files

Don't polish this cipher — **wrap `javax.crypto` AES-GCM + PBKDF2/Argon2** and keep the grid transform as an optional obfuscation layer *on top*. You get confidentiality + integrity for ~40 lines, and the fun scheme survives as flavor. Honest and done.

## If the goal is: learn cryptography (arguably the most valuable)

**Attack your own cipher.** Concretely:

- Run frequency analysis on `.cr` output and measure how much the grid stage actually hides vs. a raw substitution.
- Mount a known-plaintext attack to recover the 256-permutation.
- Empirically estimate the real keyspace of the 16-step permutation (the `secretMatrix` constraint).

You'll *feel* why each weakness above matters, which is worth more than adding features blind.

## Other things you can do (not the cipher itself)

- **Spec the `.cr` format** — write down the exact header/terminator/bit layout so it's reproducible and re-implementable in another language.
- **Add a CLI mode** (no GUI) so it's scriptable and easier to fuzz/benchmark.
- **Cross-platform paths** — the code hardcodes Windows `\\`; a couple of `File.separator` fixes make it run on Linux/Mac.
- **Throughput benchmark** — measure MB/s across block sizes (it's already O(n)/O(1), so this is about constants; real parallelism only becomes possible *after* per-block independent keying).

---

**Suggested first step:** **block chaining + per-file IV** is a self-contained, high-impact change that the existing round-trip test already covers.
