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

package net.socialgamer.pyx.importer.filetypes;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import net.socialgamer.pyx.importer.data.ParseResult;
import net.socialgamer.pyx.importer.parsers.SheetParser;


public class ExcelFileType extends FileType {
  private static final Logger LOG = Logger.getLogger(ExcelFileType.class);

  private final SheetParser.Factory columnarParserFactory;

  @Inject
  public ExcelFileType(final Properties props, @Assisted("configIndex") final int configIndex,
      final SheetParser.Factory columnarParserFactory) {
    super(props, configIndex);
    this.columnarParserFactory = columnarParserFactory;
  }

  public interface Factory {
    ExcelFileType create(@Assisted("configIndex") final int configIndex);
  }

  protected String getSheetProp(final int sheet, final String name, final String defaultValue) {
    return getProp(String.format("sheet[%d].%s", sheet, name), defaultValue);
  }

  protected String getSheetProp(final int sheet, final String name) {
    return getProp(String.format("sheet[%d].%s", sheet, name));
  }

  @Override
  public void validate() throws ConfigurationException {
    final File file = new File(getProp("name"));
    if (!file.canRead()) {
      throw new ConfigurationException("Unable to read file " + getProp("name") + ".");
    }

    final int sheetCount;
    try {
      sheetCount = Integer.valueOf(getProp("sheet.count", "0"));
    } catch (final NumberFormatException e) {
      throw new ConfigurationException("Sheet count is not a number.");
    }

    if (sheetCount <= 0) {
      throw new ConfigurationException("Must specify positive number of sheets.");
    }

    try (final Workbook workbook = new XSSFWorkbook(file)) {
      if (sheetCount > workbook.getNumberOfSheets()) {
        throw new ConfigurationException(
            String.format("Workbook file has %d sheets; %d configured.",
                workbook.getNumberOfSheets(), sheetCount));
      }
    } catch (final InvalidFormatException e) {
      throw new ConfigurationException("Workbook file format invalid: " + e.getMessage());
    } catch (final IOException ioe) {
      // should be unreachable as we don't write to the workbook
      throw new ConfigurationException("I/O error", ioe);
    }

    for (int i = 0; i < sheetCount; i++) {
      final String sheetColor = getSheetProp(i, "color");
      if (!"white".equals(sheetColor) && !"black".equals(sheetColor)) {
        throw new ConfigurationException(
            "Invalid sheet color " + sheetColor + "; must be either white or black.");
      }

      try {
        final int headingNamedCount = Integer.parseInt(getSheetProp(i, "heading_named_count", "0"));
        final int nextColNamedCount = Integer
            .parseInt(getSheetProp(i, "next_column_named_count", "0"));

        if (headingNamedCount < 0) {
          throw new ConfigurationException("Heading named count cannot be negative.");
        }
        if (nextColNamedCount < 0) {
          throw new ConfigurationException("Next column named count cannot be negative.");
        }
        if (headingNamedCount + nextColNamedCount <= 0) {
          throw new ConfigurationException(
              "Sum of heading name count and next column named count must be positive.");
        }
      } catch (final NumberFormatException e) {
        throw new ConfigurationException("Naming count is not a number.");
      }
    }
  }

  @Override
  public ParseResult process() {
    final Map<String, Set<String>> blackCardsByDeck = new HashMap<>();
    final Map<String, Set<String>> whiteCardsByDeck = new HashMap<>();

    final File file = new File(getProp("name"));
    try (final Workbook workbook = new XSSFWorkbook(file)) {
      final int sheetCount = Integer.valueOf(getProp("sheet.count", "0"));

      for (int i = 0; i < sheetCount; i++) {
        final int headingNamedCount = Integer.parseInt(getSheetProp(i, "heading_named_count", "0"));
        final int nextColNamedCount = Integer
            .parseInt(getSheetProp(i, "next_column_named_count", "0"));
        final SheetParser parser = columnarParserFactory.create(workbook.getSheetAt(i),
            headingNamedCount, nextColNamedCount);
        final Map<String, Set<String>> newCards = parser.getCards();

        final String sheetColor = getSheetProp(i, "color");
        final Map<String, Set<String>> existingCards;
        if ("black".equals(sheetColor)) {
          existingCards = blackCardsByDeck;
        } else {
          existingCards = whiteCardsByDeck;
        }

        for (final Entry<String, Set<String>> newDeck : newCards.entrySet()) {
          if (existingCards.containsKey(newDeck.getKey())) {
            existingCards.get(newDeck.getKey()).addAll(newDeck.getValue());
          } else {
            existingCards.put(newDeck.getKey(), newDeck.getValue());
          }
        }
      }
    } catch (final InvalidFormatException e) {
      // we shouldn't get here as we already validated the workbook...
      LOG.error("Unexpected: Previously validated workbook no longer valid!", e);
      throw new RuntimeException(e);
    } catch (final IOException e) {
      LOG.error("I/O error while processing workbook", e);
      throw new RuntimeException(e);
    }

    return new ParseResult(blackCardsByDeck, whiteCardsByDeck);
  }
}
