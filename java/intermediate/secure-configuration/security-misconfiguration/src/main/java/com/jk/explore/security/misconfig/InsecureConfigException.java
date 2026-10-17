package com.jk.explore.security.misconfig;

/** Thrown by the startup gate: the store refuses to start with a setting that must never reach production. */
final class InsecureConfigException extends Exception {

    InsecureConfigException(String message) {
        super(message);
    }
}
