# The F-Droid recipe

`com.retekey.yml` in this directory is a copy of what goes into the fdroiddata repository as
`metadata/com.retekey.yml`. It is kept here so the recipe and the build it drives change together,
and it is byte-for-byte what is submitted — F-Droid's reviewers asked for the file to carry no
comments of its own, so the explanation lives here instead.

Only the `modern` flavour is published through F-Droid. Both flavours carry the same
applicationId, so one package can serve only one of them, and `modern` is what the README's own
download link serves. The `legacy` build (Android 4.0 and up) stays on the releases page.

`Binaries:` together with `AllowedAPKSigningKeys` makes this a reproducible build: F-Droid compiles
the app on its own server, compares the result against the APK published on the releases page, and
on a match ships that file signed with this project's key rather than F-Droid's. That is only
possible because a clean clone builds byte-identically — `vcsInfo { include = false }` and the
disabled `dependenciesInfo` block in `app/build.gradle` are what make it so, by keeping the
builder's git checkout and an opaque Google-encrypted blob out of the APK. It also means the
signing key must be kept: losing it ends the ability to update the app.

## How this file is kept

`com.retekey.yml` is a **verbatim copy of what fdroiddata holds**, not a file we author. The app was
merged into fdroiddata on 2026-08-09, and since then their `checkupdates` bot adds a `Builds:` entry
and moves `CurrentVersion` on its own — `UpdateCheckMode: Tags` with `AutoUpdateMode: Version` means
**pushing a `v*` tag is the submission**. It takes the newest tag when it runs, so intermediate
versions are skipped rather than built.

So: do not hand-edit or bump this file per release. Refresh it when you want to see what F-Droid
currently has:

    curl -s https://gitlab.com/fdroid/fdroiddata/-/raw/master/metadata/com.retekey.yml \
      > docs/fdroid/com.retekey.yml
