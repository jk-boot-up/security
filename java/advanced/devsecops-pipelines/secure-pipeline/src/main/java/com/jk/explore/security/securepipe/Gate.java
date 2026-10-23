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

    private Gate() {
    }

    /** Exit code 0 passes, the exit code the tool uses for "found something" fails, anything else (or no tool at all) is an error. */
    static Outcome tool(Path dir, List<String> command, int foundExit) {
        try {
            int exit = Sh.run(dir, Map.of(), command).exit();
            if (exit == 0) {
                return Outcome.PASS;
            }
            return exit == foundExit ? Outcome.FAIL : Outcome.ERROR;
        } catch (IOException e) {
            return Outcome.ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.ERROR;
        } catch (RuntimeException e) {
            return Outcome.ERROR;
        }
    }

    /** Whether the pipeline goes on. A pipeline that fails closed stops on an error; one that fails open treats it as a pass. */
    static boolean goesOn(Outcome outcome, boolean failClosed) {
        return outcome == Outcome.PASS || outcome == Outcome.ERROR && !failClosed;
    }
}
