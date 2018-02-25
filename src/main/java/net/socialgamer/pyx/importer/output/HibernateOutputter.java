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

package net.socialgamer.pyx.importer.output;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.inject.Inject;

import net.socialgamer.cah.db.PyxBlackCard;
import net.socialgamer.cah.db.PyxCardSet;
import net.socialgamer.cah.db.PyxWhiteCard;
import net.socialgamer.pyx.importer.BlackCardHelper;
import net.socialgamer.pyx.importer.data.DeckInfo;
import net.socialgamer.pyx.importer.data.ParseResult;
import net.socialgamer.pyx.importer.inject.ImporterModule.SaveToDatabase;


public class HibernateOutputter {
  private static final Logger LOG = Logger.getLogger(HibernateOutputter.class);

  private final BlackCardHelper blackCardHelper;
  private final Map<String, DeckInfo> deckInfos;
  private final boolean saveToDatabase;
  private final Session session;

  @Inject
  public HibernateOutputter(final BlackCardHelper blackCardHelper,
      final Map<String, DeckInfo> deckInfos, @SaveToDatabase final boolean saveToDatabase,
      final Session session) {
    this.blackCardHelper = blackCardHelper;
    this.deckInfos = deckInfos;
    this.saveToDatabase = saveToDatabase;
    this.session = session;
  }

  public void output(final ParseResult result) {
    if (!saveToDatabase) {
      LOG.info("Not saving to database.");
      return;
    }

    final Transaction transaction = session.beginTransaction();
    transaction.begin();

    try {
      final Map<String, PyxBlackCard> blackCards = new HashMap<>();
      final Map<String, PyxWhiteCard> whiteCards = new HashMap<>();
      final Map<String, PyxCardSet> decks = new HashMap<>();

      LOG.info("Saving black cards...");
      for (final Entry<String, Set<String>> entry : result.getBlackCards().entrySet()) {
        LOG.info("Saving black cards for deck " + entry.getKey());
        final PyxCardSet deck = getOrMakeDeck(decks, entry.getKey());

        for (final String cardText : entry.getValue()) {
          final PyxBlackCard card;
          if (blackCards.containsKey(cardText)) {
            card = blackCards.get(cardText);
          } else {
            card = new PyxBlackCard();
            card.setText(cardText);
            card.setDraw(blackCardHelper.draw(cardText));
            card.setPick(blackCardHelper.pick(cardText));
            final String watermark;
            if (deckInfos.containsKey(deck.getName())) {
              watermark = deckInfos.get(deck.getName()).getWatermark();
            } else {
              LOG.warn(String.format(
                  "No deck info for deck %s, unable to determine watermark for card %s.",
                  deck.getName(), cardText));
              watermark = "";
            }
            card.setWatermark(watermark);
            session.save(card);
            blackCards.put(cardText, card);
          }
          deck.getBlackCards().add(card);
          session.save(deck);
        }
      }

      LOG.info("Saving white cards...");
      for (final Entry<String, Set<String>> entry : result.getWhiteCards().entrySet()) {
        LOG.info("Saving white cards for deck " + entry.getKey());
        final PyxCardSet deck = getOrMakeDeck(decks, entry.getKey());

        for (final String cardText : entry.getValue()) {
          final PyxWhiteCard card;
          if (whiteCards.containsKey(cardText)) {
            card = whiteCards.get(cardText);
          } else {
            card = new PyxWhiteCard();
            card.setText(cardText);
            final String watermark;
            if (deckInfos.containsKey(deck.getName())) {
              watermark = deckInfos.get(deck.getName()).getWatermark();
            } else {
              LOG.warn(String.format(
                  "No deck info for deck %s, unable to determine watermark for card %s.",
                  deck.getName(), cardText));
              watermark = "";
            }
            card.setWatermark(watermark);
            session.save(card);
            whiteCards.put(cardText, card);
          }
          deck.getWhiteCards().add(card);
          session.save(deck);
        }
      }

      transaction.commit();
    } catch (final Exception e) {
      LOG.error("Unable to save.", e);
      transaction.rollback();
    }
  }

  private PyxCardSet getOrMakeDeck(final Map<String, PyxCardSet> decks, final String name) {
    final PyxCardSet deck;
    if (decks.containsKey(name)) {
      deck = decks.get(name);
    } else {
      deck = new PyxCardSet();
      deck.setActive(true);
      deck.setName(name);
      deck.setDescription(name);
      deck.setWeight(deckInfos.get(name).getWeight());
      decks.put(name, deck);
    }
    return deck;
  }
}
