# Third-Party Notices

ReteKey includes work derived from the projects below. Their license notices are
retained here and ship with distributed packages.

## AOSP LatinIME and the `SoftKeyboard` sample — reference only, no code

ReteKey contains **no** Android Open Source Project code. AOSP, including
`packages/inputmethods/LatinIME` and the `development/samples/SoftKeyboard`
sample, is licensed under the **Apache License 2.0**, not MIT.

LatinIME is used here only as a **behavioural reference**: it is read, and its
design is described in `docs/android-ime-manual.md`. One design idea is followed
— the passive cursor cache that the editor's `onUpdateSelection` always
overrides, noted in `InputSessionController` — but the implementation is
original. Reading source and re-implementing an idea is not a derivative work,
so no Apache-2.0 obligation attaches to anything ReteKey ships today, and the
project stays MIT throughout.

If AOSP code is ever copied in, that changes. Apache-2.0 and MIT are compatible
in the sense that Apache-2.0 code may be included in an MIT-licensed project,
but the Apache terms travel with those files and **cannot be relicensed as
MIT**. Taking any AOSP source would require all of the following, in the same
change as the code:

1. keep the Apache-2.0 header on every copied file, unmodified;
2. ship the full Apache-2.0 licence text (as `LICENSE-APACHE-2.0`) and, if the
   upstream tree carries a `NOTICE` file, that file's relevant contents;
3. mark modified files as changed (Apache-2.0 §4(b));
4. record the copy here — project, revision, files, and what was changed;
5. state in `README.md` that the project is MIT **except** for the listed
   Apache-2.0 components, so users are not told the whole APK is MIT when it is
   not.

Apache-2.0 also grants a patent licence that terminates on patent litigation
(§3), and forbids using the upstream project's trademarks (§6). Neither affects
ordinary use; both are reasons the copied files must keep their own header
rather than being folded into ReteKey's licence.

## Hanja conversion data (`assets/hanja.txt`, `assets/hanja_hunum.txt`)

ReteKey's Hanja conversion table (`hanja.txt`) and 훈음 gloss table
(`hanja_hunum.txt`) are the redistributable files from the Jamotong project. Its readings are derived from the Unicode® Unihan Database and
supplemented with a public-domain court name-Hanja list; the file's own header
carries the full provenance and the required copyright notice.

- Reading data: **Unicode® Unihan Database** (`kHangul` field). Copyright ©
  1991–2026 Unicode, Inc., used under the **Unicode License v3**
  (https://www.unicode.org/license.txt). Modification and redistribution are
  permitted provided the copyright notice is retained, which the bundled file
  does in its header.
- Name-Hanja supplement: Republic of Korea Supreme Court name-Hanja list (public
  data; a factual list, not subject to copyright), routed via
  rutopio/Korean-Name-Hanja-Charset (**MIT**).
- Word mappings: Jamotong project curation, supplemented with the Korean-word →
  Hanja mappings (not the glosses) from jemdiggity/hanja-wordlist (**MIT**).
- 훈음 glosses (`hanja_hunum.txt`): original Jamotong curation of traditional
  representative glosses (a factual, centuries-old body of knowledge, not copied
  from any dictionary), with rarer entries derived from the Unicode Unihan
  `kDefinition` field (**Unicode License v3**).

The Unicode® word mark and the Unihan database name are trademarks of Unicode,
Inc. Use here is nominative, for attribution only.

## Jamotong

ReteKey's Hangul 2-beolsik composition automaton (`HangulComposer` and
`HangulTables`) is a pure-Java port of the state machine and jamo tables in the
Jamotong project's `src/fsm.c`, `src/fsm.h`, and the 2-beolsik/Unicode portions
of `src/layout.c`. The key mapping, output model (ordered editor actions instead
of a preedit overlay), reversible compound backspace, and no-loss behavior are
ReteKey's own; the transition design and the jamo/combination tables are derived
from Jamotong.

- Project: Jamotong (https://github.com/rubidus-api/jamotong_ime)
- Derived from revision: 90d6eb5ea60d54d320e42656da8b703432bb6d9f
- License: MIT

```
MIT License

Copyright (c) 2026 rubidus-api

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
