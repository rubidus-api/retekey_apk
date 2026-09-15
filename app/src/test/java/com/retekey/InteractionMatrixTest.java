package com.retekey;

import static org.junit.Assert.assertTrue;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;

/**
 * The interaction matrix: every editor kind × every way of typing, with one expected screen per
 * scenario that every editor must end up showing. Most defects this keyboard shipped lived in a
 * cell nobody typed into — a syllable redrawn in a terminal, a key passed through mid-syllable, a
 * backspace after a closed syllable in a remote desktop — so the cells are typed together, and the
 * whole table is printed with each failing cell named rather than stopping at the first.
 *
 * <p>The keys go through the real dispatcher, composer and executor; the editors are the measured
 * models in {@link SimulatedEditors}; and the parts of the service a plain JVM cannot run are
 * reproduced in {@link Rig} with a pointer to the method each copies. The device matrix
 * (scripts/interaction-matrix.sh, instrumentation build) covers what only a real editor can.
 */
public final class InteractionMatrixTest {
    private static final CheckedEditorExecutor EXECUTOR = new CheckedEditorExecutor();

    /** One editor kind: how the classifier sees it, and the model that stands in for it. */
    private static final class Kind {
        final String name;
        final Supplier<EditorProfile> profile;
        final Supplier<SimulatedEditors.Screen> editor;

        Kind(String name, Supplier<EditorProfile> profile, Supplier<SimulatedEditors.Screen> editor) {
            this.name = name;
            this.profile = profile;
            this.editor = editor;
        }
    }

    private static EditorInfo info(String pkg, int inputType, int selection) {
        EditorInfo info = new EditorInfo();
        // A multi-line EditText asks for no Enter action; Termux reports IME_FLAG_NO_FULLSCREEN
        // alone (measured on 0.118.3).
        info.imeOptions = (inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            ? EditorInfo.IME_FLAG_NO_ENTER_ACTION : EditorInfo.IME_FLAG_NO_FULLSCREEN;
        info.packageName = pkg;
        info.inputType = inputType;
        info.initialSelStart = selection;
        info.initialSelEnd = selection;
        return info;
    }

    private static final int MULTILINE_TEXT =
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
    private static final int TERMUX_TEXT_MODE = InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    private static final List<Kind> KINDS = Arrays.asList(
        new Kind("rich text",
            () -> AndroidEditorProfileClassifier.classify(info("app", MULTILINE_TEXT, 0), 33),
            SimulatedEditors::richText),
        new Kind("remote desktop",
            () -> AndroidEditorProfileClassifier.classify(
                info("com.microsoft.rdc.androidx", MULTILINE_TEXT, 0), 33),
            SimulatedEditors::remoteDesktop),
        new Kind("terminal, strip",
            () -> AndroidEditorProfileClassifier.classify(
                info("com.termux", InputType.TYPE_NULL, -1), 33).composingOffScreen(),
            SimulatedEditors::terminal),
        new Kind("terminal, drawn",
            () -> AndroidEditorProfileClassifier.classify(
                info("com.termux", InputType.TYPE_NULL, -1), 33),
            SimulatedEditors::terminal),
        new Kind("terminal text mode, strip",
            () -> AndroidEditorProfileClassifier.classify(
                info("com.termux", TERMUX_TEXT_MODE, -1), 33).composingOffScreen(),
            SimulatedEditors::terminal));

    /** One way of typing: the keys, and what every editor must show afterwards. */
    private static final class Scenario {
        final String name;
        final String keys;
        final String screen;
        final List<String> chords;

        Scenario(String name, String keys, String screen, String... chords) {
            this.name = name;
            this.keys = keys;
            this.screen = screen;
            this.chords = Arrays.asList(chords);
        }
    }

    // Keys: a–z are the physical 2-beolsik keys; BS backspace; ENTER; SPACE (soft); T:x soft text;
    // KEY:ENTER a key event from the bar or a pad; PASS:SPACE a physical key the composer does not
    // take; CTRL+C / SHIFT+LEFT raw chords;
    // MOVE:n the user moves the cursor; END leaves the field. Every scenario ends with END.
    private static final List<Scenario> SCENARIOS = Arrays.asList(
        new Scenario("one syllable", "r k END", "가"),
        new Scenario("long word", "g k s r m f d l q f u r e o g k s a l s r n r END", "한글입력대한민국"),
        new Scenario("backspace inside a syllable", "r k s BS END", "가"),
        new Scenario("backspace after a closed syllable", "r k SPACE BS END", "가"),
        new Scenario("backspace twice across syllables", "r k s k BS BS END", "가"),
        new Scenario("backspace with nothing composing", "T:a T:b BS END", "a"),
        new Scenario("enter after a syllable", "r k ENTER END", "가\n"),
        new Scenario("a key event right after a syllable", "r k KEY:ENTER END", "가\n"),
        new Scenario("an arrow right after a syllable, then more", "r k KEY:LEFT s k END", "가나"),
        new Scenario("soft space between syllables", "r k SPACE s k END", "가 나"),
        new Scenario("physical key passed through mid-syllable", "r k PASS:SPACE s k END", "가 나"),
        new Scenario("latin text after hangul", "r k T:a END", "가a"),
        new Scenario("ctrl chord ends the syllable first", "r k CTRL+C s k END", "가나",
            "[CTRL]+C"),
        new Scenario("shift+arrow leaves the text alone", "r k SHIFT+LEFT END", "가"));

    @Test
    public void everyEditorShowsTheSameScreenForEveryScenario() {
        List<String> failures = new ArrayList<>();
        StringBuilder table = new StringBuilder("\n");
        table.append(String.format("%-44s", "scenario \\ editor"));
        for (Kind kind : KINDS) {
            table.append(String.format(" | %-26s", kind.name));
        }
        table.append('\n');
        for (Scenario scenario : SCENARIOS) {
            table.append(String.format("%-44s", scenario.name));
            for (Kind kind : KINDS) {
                Rig rig = new Rig(kind.profile.get(), kind.editor.get());
                String cell;
                try {
                    rig.type(scenario.keys);
                    String screen = rig.editor.screen();
                    boolean ok = screen.equals(scenario.screen)
                        && rig.editor.chords().equals(scenario.chords)
                        && rig.refusals.isEmpty();
                    cell = ok ? "PASS" : "FAIL [" + screen.replace("\n", "⏎") + "]";
                    if (!ok) {
                        failures.add(kind.name + " / " + scenario.name + ": screen ["
                            + screen.replace("\n", "⏎") + "] expected ["
                            + scenario.screen.replace("\n", "⏎") + "], chords "
                            + rig.editor.chords() + " expected " + scenario.chords
                            + (rig.refusals.isEmpty() ? "" : ", refused " + rig.refusals));
                    }
                } catch (RuntimeException crash) {
                    cell = "CRASH " + crash.getClass().getSimpleName();
                    failures.add(kind.name + " / " + scenario.name + ": " + crash);
                }
                table.append(String.format(" | %-26s", cell));
            }
            table.append('\n');
        }
        System.out.println(table);
        assertTrue("interaction matrix failures:\n" + String.join("\n", failures)
            + "\n" + table, failures.isEmpty());
    }

    /** The keyboard in front of one editor: dispatcher, composer, executor and service rules. */
    private static final class Rig {
        final EditorProfile profile;
        final SimulatedEditors.Screen editor;
        final HangulInputProcessor processor;
        final InputDispatcher dispatcher;
        final List<String> refusals = new ArrayList<>();

        Rig(EditorProfile profile, SimulatedEditors.Screen editor) {
            this.profile = profile;
            this.editor = editor;
            this.processor = new HangulInputProcessor(() -> profile);
            this.dispatcher = new InputDispatcher(processor);
        }

        void type(String keys) {
            for (String key : keys.trim().split("\\s+")) {
                press(key);
                reportSelection();
            }
        }

        private void press(String key) {
            if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                String id = "hardware.key." + key;
                run(dispatcher.dispatch(hardwareDown(id,
                    DubeolsikHardwareMapper.INSTANCE.map(id, false))));
            } else if (key.equals("BS")) {
                run(dispatcher.dispatch(hardwareDown("hardware.edit.backspace",
                    SemanticInput.deleteBackward())));
            } else if (key.equals("ENTER")) {
                run(soft(SemanticInput.primaryAction()));
            } else if (key.equals("SPACE")) {
                run(soft(SemanticInput.text(" ")));
            } else if (key.startsWith("T:")) {
                run(soft(SemanticInput.text(key.substring(2))));
            } else if (key.startsWith("KEY:")) {
                run(soft(SemanticInput.rawKey(RawKey.valueOf(key.substring(4)))));
            } else if (key.startsWith("PASS:")) {
                passThrough(RawKey.valueOf(key.substring(5)));
            } else if (key.startsWith("MOVE:")) {
                SimulatedEditors.moveCursor(editor, Integer.parseInt(key.substring(5)));
            } else if (key.contains("+")) {
                String[] parts = key.split("\\+");
                run(soft(SemanticInput.rawKey(RawKey.valueOf(parts[1]),
                    EnumSet.of(KeyModifier.valueOf(parts[0])))));
            } else if (key.equals("END")) {
                leaveTheField();
            } else {
                throw new IllegalArgumentException("unknown key " + key);
            }
        }

        private DispatchResult soft(SemanticInput input) {
            return dispatcher.dispatch(ProjectKeyEvent.softwareDown("touch.matrix", input));
        }

        /** ReteKeyImeService#endSyllableBeforeDelegating, then the framework hands the app the key. */
        private void passThrough(RawKey key) {
            boolean endsFirst = TerminalHardwareKeys.endSyllableFirst(
                profile.capabilities().deleteByKeyEvents()
                    || profile.capabilities().composesOffScreen(),
                processor.isComposing(), true, false);
            if (endsFirst) {
                run(soft(SemanticInput.flush()));
            }
            editor.sendRawKey(RawEditorKey.hardware(key,
                EnumSet.noneOf(KeyModifier.class), RawEditorKey.Action.DOWN));
            editor.sendRawKey(RawEditorKey.hardware(key,
                EnumSet.noneOf(KeyModifier.class), RawEditorKey.Action.UP));
        }

        /** ReteKeyImeService#onFinishInput: the held syllable is written, the region finished. */
        private void leaveTheField() {
            if (profile.capabilities().composesOffScreen() && processor.isComposing()) {
                run(soft(SemanticInput.flush()));
            }
            editor.finishComposingText();
            processor.reset();
        }

        /** ReteKeyImeService#onUpdateSelection, for the editors that report a selection. */
        private void reportSelection() {
            EditorBounds b = editor.bounds();
            if (!b.hasSelection() || profile.capabilities().deleteByKeyEvents()) {
                return;
            }
            boolean abandon = CursorMovePolicy.shouldAbandonComposition(
                processor.isComposing(), b.selectionStart(), b.selectionEnd(),
                b.composingStart(), b.composingEnd());
            if (abandon) {
                editor.finishComposingText();
                processor.reset();
            }
        }

        private void run(DispatchResult result) {
            if (result.actions().isEmpty()) {
                return;
            }
            TransitionPlan<String> plan = TransitionPlan.of(1, 0,
                DispatchResult.Disposition.HANDLED, "state", editor.bounds(), result.actions());
            ExecutionResult outcome = EXECUTOR.execute(plan,
                ExecutionContext.active(1, 0, editor.bounds(), profile.capabilities()),
                () -> EditorEndpoint.of(1, editor));
            if (MaterializedCompositionPolicy.shouldForgetWhatWasWritten(
                    profile.capabilities().deleteByKeyEvents(), outcome.outcome())) {
                processor.forgetMaterialized();
            }
            if (outcome.outcome() != ExecutionResult.Outcome.DISPATCHED
                    && outcome.outcome() != ExecutionResult.Outcome.NO_EDITOR_ACTIONS) {
                refusals.add(outcome.outcome() + ":" + outcome.reason());
            }
        }
    }

    private static ProjectKeyEvent hardwareDown(String stableKeyId, SemanticInput mapped) {
        return HardwareEventNormalizer.normalize(
            RawHardwareKeyEvent.builder(InputAction.DOWN, stableKeyId.hashCode() & 0xff | 1)
                .stableKeyId(stableKeyId)
                .deviceId(3)
                .mappedInput(mapped)
                .build());
    }
}
