package com.jk.explore.security.securitylogging;

import java.io.IOException;

/** Where finished log lines go. */
interface LogSink {

    void write(String line) throws IOException;
}
