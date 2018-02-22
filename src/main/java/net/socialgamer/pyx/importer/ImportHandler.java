/**
 * Copyright (c) 2018, Andy Janata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.socialgamer.pyx.importer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import net.socialgamer.pyx.importer.data.DeckInfo;
import net.socialgamer.pyx.importer.data.ParseResult;
import net.socialgamer.pyx.importer.filetypes.FileType;


public class ImportHandler {

  private final Logger LOG = Logger.getLogger(ImportHandler.class);

  private final Map<String, DeckInfo> deckInfos;
  private final List<FileType> fileTypes;

  @Inject
  public ImportHandler(final Map<String, DeckInfo> deckInfos,
      @Assisted("fileTypes") final List<FileType> fileTypes) {
    this.deckInfos = deckInfos;
    this.fileTypes = fileTypes;
  }

  public interface Factory {
    ImportHandler create(@Assisted("fileTypes") List<FileType> fileTypes);
  }

  public ParseResult process() {
    final Set<String> decks = new HashSet<>();
    final Map<String, Set<String>> blackCards = new HashMap<>();
    final Map<String, Set<String>> whiteCards = new HashMap<>();

    for (final FileType fileType : fileTypes) {
      final ParseResult result = fileType.process();

      for (final Entry<String, Set<String>> e : result.getBlackCards().entrySet()) {
        final String deck;
        if (!deckInfos.containsKey(e.getKey())) {
          LOG.warn(String.format("Deck info not found for deck %s.", e.getKey()));
          deck = e.getKey();
        } else {
          deck = deckInfos.get(e.getKey()).getName();
        }
        if (blackCards.containsKey(deck)) {
          blackCards.get(deck).addAll(e.getValue());
        } else {
          blackCards.put(deck, e.getValue());
        }
        decks.add(deck);
      }

      for (final Entry<String, Set<String>> e : result.getWhiteCards().entrySet()) {
        final String deck;
        if (!deckInfos.containsKey(e.getKey())) {
          LOG.warn(String.format("Deck info not found for deck %s.", e.getKey()));
          deck = e.getKey();
        } else {
          deck = deckInfos.get(e.getKey()).getName();
        }
        if (whiteCards.containsKey(deck)) {
          whiteCards.get(deck).addAll(e.getValue());
        } else {
          whiteCards.put(deck, e.getValue());
        }
        decks.add(deck);
      }
    }

    // dump to output
    LOG.info("Decks:");
    for (final String deck : decks) {
      final int blackCount;
      if (blackCards.containsKey(deck)) {
        blackCount = blackCards.get(deck).size();
      } else {
        blackCount = 0;
      }
      final int whiteCount;
      if (whiteCards.containsKey(deck)) {
        whiteCount = whiteCards.get(deck).size();
      } else {
        whiteCount = 0;
      }
      LOG.info(String.format(">%s (black: %d, white: %d)", deck, blackCount, whiteCount));
    }

    LOG.trace("White cards:");
    for (final Entry<String, Set<String>> entry : whiteCards.entrySet()) {
      LOG.trace(">" + entry.getKey());
      for (final String card : entry.getValue()) {
        LOG.trace(">>" + card);
      }
    }

    LOG.trace("Black cards:");
    for (final Entry<String, Set<String>> entry : blackCards.entrySet()) {
      LOG.trace(">" + entry.getKey());
      for (final String card : entry.getValue()) {
        LOG.trace(">>" + card);
      }
    }

    return new ParseResult(blackCards, whiteCards);
  }
}
