# Third-Party Notices

ReteKey includes work derived from the projects below. Their license notices are
retained here and ship with distributed packages.

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
