package com.jk.explore.security.sqli;

/** A statement the database refused: bad syntax, an unknown table or column, or a table the database user may not read. */
public class SqlException extends RuntimeException {

    public SqlException(String message) {
        super(message);
    }
}
