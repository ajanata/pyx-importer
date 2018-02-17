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

package net.socialgamer.pyx.importer.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import net.socialgamer.pyx.importer.RichTextToHtmlFormatHelper;


/**
 * Parse a sheet which has cards and decks indicated in either, or both, of two ways:
 * <ul><li>Deck name in the first row, cards under them ("heading named").</li>
 * <li>Cards in a column, with the deck they belong to in the next column ("next column named"). In
 * this case, the first row of each column is ignored.</li></ul>
 * <p>This does not know if the cards are
 * black or white, it simply parses the text.
 */
public class SheetParser implements Parser {

  private static final Logger LOG = Logger.getLogger(SheetParser.class);

  private final int headingNamedCount;
  private final int nextColNamedCount;
  private final Sheet sheet;
  private final RichTextToHtmlFormatHelper formatHelper;

  @Inject
  public SheetParser(@Assisted("sheet") final Sheet sheet,
      @Assisted("headingNamedCount") final int headingNamedCount,
      @Assisted("nextColNamedCount") final int nextColNamedCount,
      final RichTextToHtmlFormatHelper formatHelper) {
    this.headingNamedCount = headingNamedCount;
    this.nextColNamedCount = nextColNamedCount;
    this.sheet = sheet;
    this.formatHelper = formatHelper;
    LOG.trace(String.format("Created sheet parser for %s.", sheet.getSheetName()));
  }

  public interface Factory {
    SheetParser create(@Assisted("sheet") final Sheet sheet,
        @Assisted("headingNamedCount") final int headingNamedCount,
        @Assisted("nextColNamedCount") final int nextColNamedCount);
  }

  @Override
  public Map<String, Set<String>> getCards() {
    final List<String> columnHeadings = new ArrayList<>();
    final Map<String, Set<String>> values = new HashMap<>();

    for (final Row row : sheet) {
      final boolean firstRow = columnHeadings.isEmpty();

      for (int col = 0; col < row.getLastCellNum(); col++) {
        final Cell cell = row.getCell(col);
        if (null == cell) {
          continue;
        }
        if (firstRow) {
          final String cellValue = cell.getStringCellValue().trim();
          if (col < headingNamedCount) {
            columnHeadings.add(cellValue);
            values.put(cellValue, new HashSet<>());
          } else {
            LOG.trace(String.format(
                "Skipping heading for column %d as it is not heading-named (value=%s)", col,
                cellValue));
          }
        } else {
          if (col < headingNamedCount) {
            final XSSFRichTextString rtf = (XSSFRichTextString) cell.getRichStringCellValue();
            final String text = formatHelper.format(rtf);
            if (!text.isEmpty()) {
              values.get(columnHeadings.get(col)).add(text);
            }
          } else if (col < headingNamedCount + (nextColNamedCount * 2)) {
            final XSSFRichTextString rtf = (XSSFRichTextString) cell.getRichStringCellValue();
            final String text = formatHelper.format(rtf);
            final String deck = row.getCell(++col).getStringCellValue().trim();
            if (deck.isEmpty() && !text.isEmpty()) {
              LOG.warn(
                  String.format("Next-column-labeled cell row %d col %d (%s) has blank deck name!",
                      row.getRowNum(), col - 1, text));
            } else if (!text.isEmpty()) {
              if (!values.containsKey(deck)) {
                values.put(deck, new HashSet<>());
              }
              values.get(deck).add(text);
            }
          } else if (cell.getStringCellValue().trim() != null) {
            LOG.warn(String.format("Skipping value for row %d col %d (%s), don't know if it should"
                + " be heading-named or next-column-named!",
                row.getRowNum(), col, cell.getStringCellValue()));
          }
        }
      }
    }
    return values;
  }
}
