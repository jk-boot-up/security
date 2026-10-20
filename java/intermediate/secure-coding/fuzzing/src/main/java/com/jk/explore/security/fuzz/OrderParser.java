package com.jk.explore.security.fuzz;

import java.util.List;

/** A parser for the shop's order files. */
public interface OrderParser {

    List<OrderLine> parse(String text, Coverage cov) throws ParseException;
}
