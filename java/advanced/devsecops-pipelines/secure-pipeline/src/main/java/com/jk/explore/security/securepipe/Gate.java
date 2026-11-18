package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** How a gate that runs an outside tool turns what happened into pass, fail or error. */
final class Gate {

    /** PASS lets the change go on, FAIL means the tool found a problem, ERROR means the tool could not give an answer. */
    enum Outcome {
        PASS, FAIL, ERROR
    }

    /** What the tool gave back: its exit code (-1 when no program could be started) and the outcome that the code means. */
    record Answer(Outcome outcome, int exit) {

        /** In words, for example "exit 7" or "no program". */
        String exitText() {
            return exit < 0 ? "no program" : "exit " + exit;
        }
    }

    private Gate() {
    }

    /** Exit code 0 passes, the exit code the tool uses for "found something" fails, anything else (or no tool at all) is an error. */
    static Answer run(Path dir, List<String> command, int foundExit) {
        try {
            int exit = Sh.run(dir, Map.of(), command).exit();
            return new Answer(outcome(exit, foundExit), exit);
        } catch (IOException e) {
            return new Answer(Outcome.ERROR, -1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Answer(Outcome.ERROR, -1);
        } catch (RuntimeException e) {
            return new Answer(Outcome.ERROR, -1);
        }
    }

    static Outcome outcome(int exit, int foundExit) {
        if (exit == 0) {
            return Outcome.PASS;
        }
        return exit == foundExit ? Outcome.FAIL : Outcome.ERROR;
    }

    static Outcome tool(Path dir, List<String> command, int foundExit) {
        return run(dir, command, foundExit).outcome();
    }

    /** Whether the pipeline goes on. A pipeline that fails closed stops on an error; one that fails open treats it as a pass. */
    static boolean goesOn(Outcome outcome, boolean failClosed) {
        return outcome == Outcome.PASS || outcome == Outcome.ERROR && !failClosed;
    }
}
