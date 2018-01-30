package net.socialgamer.pyx.importer.parsers;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;


public class ColumnarSheetParser {

  private static final Logger LOG = Logger.getLogger(ColumnarSheetParser.class);

  private final Sheet sheet;

  public ColumnarSheetParser(final Sheet sheet) {
    this.sheet = sheet;
    LOG.trace(String.format("Created columnar Excel sheet parser for %s.", sheet.getSheetName()));
  }

}
