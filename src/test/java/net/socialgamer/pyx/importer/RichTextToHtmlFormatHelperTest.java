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

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.junit.Before;
import org.junit.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;


public class RichTextToHtmlFormatHelperTest {

  RichTextToHtmlFormatHelper helper;

  @SuppressWarnings("serial")
  @Before
  public void beforeTest() {
    helper = new RichTextToHtmlFormatHelper(true, new LinkedHashMap<String, String>() {
      {
        put("&", "&amp;");
        put("<", "&lt;");
        put(">", "&gt;");
        put("ñ", "&ntilde;");
        put("\n", "<br>");
      }
    });
  }

  @Test
  public void testFormat_Underscores() {
    final XSSFRichTextString rtf = new XSSFRichTextString("a1_b2__c3___d4____e5_____f8________g");
    assertEquals("a1_b2__c3___d4____e5____f8____g", helper.format(rtf));
  }

  @Test
  public void testFormat_Simple() {
    final XSSFRichTextString rtf = new XSSFRichTextString("Simple string!");
    assertEquals("Simple string!", helper.format(rtf));
  }

  @Test
  public void testFormat_Replacements() {
    final XSSFRichTextString rtf = new XSSFRichTextString(
        "Hello & welcome!\nPlease come back mañana. :>");
    assertEquals("Hello &amp; welcome!<br>Please come back ma&ntilde;ana. :&gt;",
        helper.format(rtf));
  }

  @Test
  public void testFormat_BoldOnly() {
    final XSSFRichTextString rtf = new XSSFRichTextString();
    final XSSFFont bold = newFont();
    bold.setBold(true);
    rtf.append("This is bold!", bold);
    assertEquals("<b>This is bold!</b>", helper.format(rtf));
  }

  @Test
  public void testFormat_MixedStyle() {
    final XSSFRichTextString rtf = new XSSFRichTextString();
    final XSSFFont italic = newFont();
    italic.setItalic(true);
    final XSSFFont underline = newFont();
    underline.setUnderline(FontUnderline.SINGLE);
    rtf.append("Plain. ");
    rtf.append("Italic. ", italic);
    rtf.append("Underline.", underline);
    rtf.append(" More plain.");
    assertEquals("Plain. <i>Italic.</i> <u>Underline.</u> More plain.", helper.format(rtf));
  }

  @Test
  public void testFormat_Overlapped() {
    final XSSFRichTextString rtf = new XSSFRichTextString();
    final XSSFFont boldItalic = newFont();
    boldItalic.setBold(true);
    boldItalic.setItalic(true);
    rtf.append("You said ");
    rtf.append("what", boldItalic);
    rtf.append(" happened?!");
    assertEquals("You said <b><i>what</i></b> happened?!", helper.format(rtf));
  }

  @Test
  public void testFormat_ReplacementsInBold() {
    final XSSFRichTextString rtf = new XSSFRichTextString();
    final XSSFFont bold = newFont();
    bold.setBold(true);
    rtf.append("More fun & awesome bold! :>", bold);
    assertEquals("<b>More fun &amp; awesome bold! :&gt;</b>", helper.format(rtf));
  }

  private XSSFFont newFont() {
    return new XSSFFont(CTFont.Factory.newInstance());
  }
}
