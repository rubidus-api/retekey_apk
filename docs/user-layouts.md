# Writing a ReteKey layout file

**English** · [한국어](user-layouts.ko.md)

A ReteKey layout is a text file. It says which letters are on the three rows of keys and what each
key holds; everything else — the bottom row, Shift, backspace, Enter, the layout key — is the
keyboard's own and is not described in the file. That is deliberate: a format published in a release
has to keep working for ever, so this one says as little as it can get away with. What it does say,
it will keep saying.

You can write one on the phone itself. Open **ReteKey settings → Your own layout**, copy the example
shown there into any notes app, change the letters, then share it back with **Install a ReteKey
layout**. No computer, no build, no account.

## Table of contents

- [The shortest layout that works](#the-shortest-layout-that-works)
- [The lines](#the-lines)
- [Keys, and what a key holds](#keys-and-what-a-key-holds)
- [How many keys fit](#how-many-keys-fit)
- [What Shift does](#what-shift-does)
- [Installing it](#installing-it)
- [Turning it on](#turning-it-on)
- [When it says it could not be read](#when-it-says-it-could-not-be-read)
- [What a layout file cannot do](#what-a-layout-file-cannot-do)
- [Worked examples](#worked-examples)

## The shortest layout that works

```rkl
retekey-layout 1
name: Greek phonetic
cap: grk
row: α β γ|ϝ δ ε|έ ζ η|ή θ ι|ί κ
row: λ μ ν ξ ο|ό π ρ σ|ς τ
row: υ|ύ φ χ ψ ω|ώ
```

Four kinds of line, and only two of them are compulsory: the header must be there, `name:` must be
there, and there must be exactly three `row:` lines. `cap:` is optional.

Save it with the extension **`.rkl`** if you are going to open it as a file. If you are going to
share it as text, the extension does not matter — there is no file.

## The lines

| Line | Required | What it does |
| --- | --- | --- |
| `retekey-layout 1` | yes | The first line of the file. Nothing may come before it, not even a comment. The `1` is the format's version; a later ReteKey will still read a file that says `1`. |
| `name: …` | yes | What the layout is called in settings and in the toast the layout key shows. Longer than 40 characters and the rest is dropped, not refused. |
| `cap: …` | no | The three letters the layout key wears. Left out, ReteKey takes the first three non-space characters of the name. Shorter than three and it is padded with `·`. |
| `row: …` | exactly 3 | One row of keys. The fourth and later `row:` lines are ignored. Fewer than three and the file is refused. |
| `# …` | no | A comment. Blank lines are fine too. |
| anything else | no | Ignored, never refused — that is how a file written for a later version still works in this one. |

The file is read as **UTF-8**. Windows, Mac and Unix line endings are all accepted. When it is
opened as a file rather than shared as text, only the first 64 KiB are read; a layout is a few
hundred bytes, so this only matters if you point ReteKey at something that is not a layout.

## Keys, and what a key holds

A `row:` line is keys separated by **spaces**. A key is what it types, then — after a `|` — what it
holds, and more `|` for more of them:

```rkl
retekey-layout 1
name: Holds
row: a e|é|è|ê|ë i o u
row: ch sh th ng
row: · — “|”
```

- `e|é|è|ê|ë` types **e**, and holds four accented forms.
- A key can type **more than one character**: `ch` is one key that types two letters. The key shows
  `ch` on its face.
- A key cannot type a **space** (spaces are what separates keys) or a **`|`** (it separates the
  holds). Everything else is fair game, including combining marks: a key that types `U+0303` adds a
  nasal tilde to whatever came before it, which is how the phonetic page does nasal vowels.
- At most **five** holds per key; any after that are dropped silently.
- An empty hold is skipped, so `a||b` is a key that types `a` and holds `b`.

**How holds behave on the glass.** Press and keep a key down for about half a second. If it holds
exactly one thing, that one thing is typed the moment the half-second passes — nothing to aim at. If
it holds several, a strip of them appears *above* the key; slide the finger sideways onto the one you
want and lift. Do not move and you get the first. The key's face tells you in advance: the first
hold is printed small in its top-right corner, and a dot in the bottom-right corner means there is
more than one.

## How many keys fit

The keyboard is a ten-column grid, and three of those columns are already spoken for:

| Row | Keys from your file | What takes the rest |
| --- | --- | --- |
| first | up to **10** | nothing |
| second | up to **9** | backspace, on the right |
| third | up to **8** | Shift on the left, Enter on the right |

Write fewer and the row is padded with blanks — the page keeps its shape. Write more and the extras
are dropped from the end, quietly. Twenty-seven keys is the whole alphabet plus one, so this is
rarely the binding constraint; when it is, the answer is usually a hold rather than another key.

## What Shift does

Shift types the **capital** of whatever the key types, as the platform spells it — `α` becomes `Α`,
`ch` becomes `CH`. For a script with no capitals, Shift changes nothing. A layout file cannot give
Shift a second set of symbols of its own; the phonetic page does that, but it is built into the app
rather than written in a file.

Holds do not change under Shift: a held `é` is `é` either way.

## Installing it

Two roads, and they are deliberately separate from the one that keeps text private:

**As text.** Select the whole layout in whatever app it is in — a notes app, a message, a web page —
and choose **Share**, then **Install a ReteKey layout**. ReteKey says `Layout installed: <name>.
Turn it on in settings.`

**As a file.** Save it with a `.rkl` extension and open it — from a file manager, a download, a mail
attachment — then choose ReteKey. Nothing needs storage permission: the app that opens the file
grants ReteKey that one file and nothing else.

**Not** by sharing to plain **ReteKey**. That entry keeps text for typing later and never installs
anything, even when the text begins with `retekey-layout 1`. The two entries look alike in the share
sheet and do entirely different things; the one that installs says so by name.

There is **one slot**. Installing another layout replaces the one before it, and ReteKey says the
new name so you can see that it happened. **Remove** in settings empties the slot.

## Turning it on

Installing puts the layout in the app; it does not put it on the keyboard. Go to **ReteKey settings
→ Keyboard layouts**, find your layout in the **etc** group — it is listed under its own name — and
tick it. It then joins the layouts the layout key walks through, wearing the three letters from
`cap:`.

A layout that is installed but not ticked costs nothing and shows nowhere. A layout that is ticked
but then removed leaves the slot empty, and the entry reads `Your layout(none yet)`.

## When it says it could not be read

`That layout file could not be read.` means the text is not a layout this version can use. There are
exactly four reasons:

| What is wrong | What it looks like |
| --- | --- |
| The header is not the first thing in the file | a comment, a blank note, or a mail quote (`>`) above `retekey-layout 1` |
| No `name:` line, or an empty one | `name:` with nothing after it |
| Fewer than three `row:` lines | two rows, or three lines that say `rows:` |
| A `row:` line with no keys on it | `row:` followed by nothing, or by only `|` |

Anything else is not an error: unknown lines are ignored, extra keys and extra holds are dropped,
and a fourth row is skipped. If a key is missing from the page, count the keys in that row against
the table above rather than looking for a message — there will not be one.

## What a layout file cannot do

Said plainly, because guessing at this wastes an evening:

- It cannot change the bottom row, Shift, backspace, Enter or the layout key.
- It cannot give Shift its own symbols, only capitals.
- It cannot add a fourth row, or make the grid wider than ten columns.
- It cannot type a space or a `|` from a key.
- It cannot define flicks, a second page, a language for the composer, or a name for a key.
- It cannot be one of several: there is one slot.

If one of these is in the way of a layout you actually want to use, that is worth
[an issue](https://github.com/rubidus-api/retekey_apk/issues) — with the line you wanted to write in
it. The format is deliberately small, not finished.

## Worked examples

**A phonetic Russian layout**, where the letters sit near the Latin sounds they stand for. The
second row stops at nine keys because backspace takes the tenth, so `ъ` rides along as a hold on `ь`
rather than asking for a column that is not there.

```rkl
retekey-layout 1
name: Russian phonetic
cap: рус
row: я ш е р т ы у и о п
row: а с д ф г ч й к л
row: з ж ц в б н м ь|ъ
```

**A row of one-key digraphs**, for a transcription system that treats them as single letters:

```rkl
retekey-layout 1
name: Digraphs
cap: dig
row: ch dz dž gj kj lj nj ts
row: a e i o u
row: š ž č ć đ
```

**A layout with a comment and a line from the future**, which this version ignores rather than
refuses:

```rkl
retekey-layout 1
# The vowels are on the home row, where the fingers rest.
name: Vowels first
cap: vwl
flick: a=left
row: q w f p b j l u y ;
row: a r s t g m n e i o
row: z x c d v k h
```

The format's own rationale — why it says so little, and why unknown lines are ignored rather than
refused — is in the implementation manual, [§15.47](android-ime-manual.md#1547-a-format-is-a-promise-so-say-as-little-as-possible).
