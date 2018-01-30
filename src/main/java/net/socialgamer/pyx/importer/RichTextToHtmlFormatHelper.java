package net.socialgamer.pyx.importer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;


public class RichTextToHtmlFormatHelper {

  private static final Logger LOG = Logger.getLogger(RichTextToHtmlFormatHelper.class);

  // log about any cards that have any characters above this left in them after html entity
  // replacement
  private static final char LAST_ASCII_CHARACTER = '~';

  // replace these characters with their html entities
  // must be a linked hashmap cuz the iteration order matters
  private static final Map<String, String> SPECIAL_CHARACTER_REPLACEMENTS = new LinkedHashMap<String, String>() {
    private static final long serialVersionUID = 2444462073349412649L;
    {
      // must be first or it'll break everything else
      put("&", "&amp;");
      put("®", "&reg;");
      put("é", "&eacute;");
      put("£", "&pound;");
      put("ñ", "&ntilde;");
      put("™", "&trade;");
    }
  };

  public String format(final XSSFRichTextString rtf) {
    final String formatted;
    if (rtf.hasFormatting()) {
      LOG.trace(String.format("Processing formatting for %s", rtf.getString()));
      final StringBuilder builder = new StringBuilder();
      for (int i = 0; i < rtf.numFormattingRuns(); i++) {
        final String segment = rtf.getString().substring(rtf.getIndexOfFormattingRun(i),
            rtf.getIndexOfFormattingRun(i) + rtf.getLengthOfFormattingRun(i));
        // will be null for normal font
        final XSSFFont font = rtf.getFontOfFormattingRun(i);
        if (null == font) {
          builder.append(segment);
        } else {
          int formatsApplied = 0;
          // figure out how to format it
          if (font.getBold()) {
            formatsApplied++;
            builder.append("<b>");
          }
          if (font.getItalic()) {
            formatsApplied++;
            builder.append("<i>");
          }
          // TODO check??
          if (font.getUnderline() > 0) {
            formatsApplied++;
            builder.append("<u>");
          }

          builder.append(segment);

          // reverse order
          // TODO check??
          if (font.getUnderline() > 0) {
            builder.append("</u>");
          }
          if (font.getItalic()) {
            builder.append("</i>");
          }
          if (font.getBold()) {
            builder.append("</b>");
          }

          // there might still be unknown formatting, if it also had something else...
          if (0 == formatsApplied) {
            LOG.warn(String.format("Unknown formatting applied to segment '%s' of card '%s'.",
                segment, rtf.getString()));
          }
        }
      }
      formatted = builder.toString();
    } else {
      formatted = rtf.getString();
    }

    final String done = replaceSpecials(formatted);
    if (!done.equals(rtf.getString())) {
      LOG.trace(String.format("Adjusted input string '%s' to '%s'.", rtf.getString(), done));
    }
    return done;
  }

  private String replaceSpecials(final String str) {
    String specialsReplaced = str;
    for (final Entry<String, String> entry : SPECIAL_CHARACTER_REPLACEMENTS.entrySet()) {
      if (specialsReplaced.contains(entry.getKey())) {
        specialsReplaced = specialsReplaced.replace(entry.getKey(), entry.getValue());
      }
    }

    // see if there are any we don't know about
    // TODO we might want to fail spectacularly here
    for (int i = 0; i < specialsReplaced.length(); i++) {
      if (specialsReplaced.charAt(i) > LAST_ASCII_CHARACTER) {
        LOG.warn(String.format("Unhandled special character '%c' in string '%s'.",
            specialsReplaced.charAt(i), str));
      }
    }
    return specialsReplaced;
  }
}
