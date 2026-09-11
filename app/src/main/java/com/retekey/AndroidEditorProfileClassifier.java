package com.retekey;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public final class AndroidEditorProfileClassifier {
    private static final int API_TIRAMISU = 33;

    /**
     * Apps whose editor is a window onto another machine. They show the IME a local dummy buffer,
     * so a surrounding-text delete only reaches the remote side while recently typed text still
     * sits in that buffer — the "backspace works right after typing and sometimes not" report.
     * For these, deletion goes out as backspace key events, which they always forward; everything
     * else (committing, composing) keeps the ordinary rich path, so Hangul still composes.
     */
    private static final java.util.Set<String> REMOTE_DESKTOP_PACKAGES =
        new java.util.HashSet<>(java.util.Arrays.asList(
            "com.microsoft.rdc.android",
            "com.microsoft.rdc.androidx",
            "com.google.chromeremotedesktop",
            "com.google.chromoting"
        ));

    /**
     * Apps whose editor is a terminal: a program on the other side of a pipe, not a text view.
     * They forward committed text and key events and swallow everything else, so there is no
     * composing region to underline and no buffer for a surrounding-text delete to reach.
     *
     * <p>A terminal that reports {@code TYPE_NULL} says so itself and needs no entry here. Termux
     * has a setting for that — {@code enforce-char-based-input} — and reports an ordinary
     * visible-password text field when it is off, which by input type alone is indistinguishable
     * from a login form that has "show password" ticked. This list names the terminals known here;
     * {@link #looksLikeATerminal} recognises the rest by shape (issue #7).
     */
    private static final java.util.Set<String> TERMINAL_PACKAGES =
        new java.util.HashSet<>(java.util.Arrays.asList(
            "com.termux",
            "com.termux.window",
            "jackpal.androidterm",
            "org.connectbot",
            "com.sonelli.juicessh",
            "com.server.auditor.ssh.client"
        ));

    private AndroidEditorProfileClassifier() {
    }

    public static EditorProfile classify(EditorInfo editorInfo, int platformApi) {
        if (editorInfo == null) {
            return EditorProfile.unsupported();
        }
        EditorProfile profile = classifyFields(
            editorInfo.inputType,
            editorInfo.imeOptions,
            editorInfo.actionLabel != null,
            editorInfo.actionId,
            platformApi
        );
        if (isTerminal(editorInfo.packageName)
                || looksLikeATerminal(editorInfo, profile)) {
            return profile.asTerminal();
        }
        if (isRemoteDesktop(editorInfo.packageName)
                && profile.capabilities().deletionMode()
                    == EditorCapabilities.DeletionMode.RICH_TEXT) {
            return profile.withDeleteByKeyEvents();
        }
        return profile;
    }

    /**
     * Whether this editor looks like a terminal without being one this build knows by name.
     *
     * <p>A terminal that reports a text field is indistinguishable from a login form by input
     * type alone — both say "visible password, no suggestions" — but not by what else they say. A
     * text field knows where its cursor is and puts it in {@code initialSelStart}; a terminal has
     * no buffer to have a cursor in and leaves it at -1, which is the same thing this keyboard
     * has always used to recognise one (manual §7). Taking the two signals together names the
     * shape instead of naming the app, which is what makes it work in the terminals nobody
     * thought to list (issue #7).
     *
     * <p>Being wrong in this direction is cheap: an ordinary field treated as a terminal is typed
     * into by commits rather than composition and deleted with a key event, which it also
     * understands. Being wrong the other way is the bug.
     */
    private static boolean looksLikeATerminal(EditorInfo editorInfo, EditorProfile profile) {
        if (profile.capabilities().deletionMode() != EditorCapabilities.DeletionMode.RICH_TEXT) {
            return false;
        }
        boolean noCursor = editorInfo.initialSelStart < 0 || editorInfo.initialSelEnd < 0;
        int variation = editorInfo.inputType & InputType.TYPE_MASK_VARIATION;
        boolean terminalShape =
            (editorInfo.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
                && variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                && (editorInfo.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0;
        return noCursor && terminalShape;
    }

    /** Whether this package's editor is a terminal (see the set above). */
    static boolean isTerminal(String packageName) {
        return packageName != null && TERMINAL_PACKAGES.contains(packageName);
    }

    /** Whether this package's editor is a remote-desktop window (see the set above). */
    static boolean isRemoteDesktop(String packageName) {
        return packageName != null && REMOTE_DESKTOP_PACKAGES.contains(packageName);
    }

    static EditorProfile classifyFields(
        int inputType,
        int imeOptions,
        boolean customActionPresent,
        int customActionId,
        int platformApi
    ) {
        if (platformApi < 1) {
            throw new IllegalArgumentException("platformApi must be positive");
        }
        boolean noEnterAction = (imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        int maskedAction = imeOptions & EditorInfo.IME_MASK_ACTION;
        int standardActionId = maskedAction == EditorInfo.IME_ACTION_NONE
            ? -1
            : maskedAction;
        int inputClass = inputType & InputType.TYPE_MASK_CLASS;

        if (inputType == InputType.TYPE_NULL) {
            return EditorProfile.typeNull(
                noEnterAction,
                customActionPresent,
                customActionId,
                standardActionId
            );
        }

        boolean multiline = inputClass == InputType.TYPE_CLASS_TEXT
            && (inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        boolean sensitive = isSensitive(inputType, inputClass);
        return EditorProfile.richText(
            sensitive,
            platformApi < API_TIRAMISU,
            platformApi < API_TIRAMISU,
            multiline,
            noEnterAction,
            customActionPresent,
            customActionId,
            standardActionId
        );
    }

    private static boolean isSensitive(int inputType, int inputClass) {
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (inputClass == InputType.TYPE_CLASS_TEXT) {
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        }
        return inputClass == InputType.TYPE_CLASS_NUMBER
            && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
    }
}
