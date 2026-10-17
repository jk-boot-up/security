package com.jk.explore.security.attacks;

import java.util.Map;

/**
 * What an attack achieved.
 *
 * @param taken accounts the attacker got into, and the tick at which each fell
 * @param guesses guesses that were really tried (an attempt refused by a lock or a limit is not counted)
 * @param ticks the tick at which the attack finished
 */
public record Found(Map<String, Integer> taken, int guesses, int ticks) {
}
