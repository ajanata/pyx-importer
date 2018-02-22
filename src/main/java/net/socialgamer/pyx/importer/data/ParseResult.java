package net.socialgamer.pyx.importer.data;

import java.util.Map;
import java.util.Set;

public class ParseResult {
  private final Map<String, Set<String>> blackCards;
  private final Map<String, Set<String>> whiteCards;

  public ParseResult(final Map<String, Set<String>> blackCards,
      final Map<String, Set<String>> whiteCards) {
    this.blackCards = blackCards;
    this.whiteCards = whiteCards;
  }

  public Map<String, Set<String>> getBlackCards() {
    return blackCards;
  }

  public Map<String, Set<String>> getWhiteCards() {
    return whiteCards;
  }
}