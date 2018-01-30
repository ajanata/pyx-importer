package net.socialgamer.pyx.importer.parsers;

import java.io.File;

import org.apache.log4j.Logger;


public class ColumnarParser {

  private static final Logger LOG = Logger.getLogger(ColumnarParser.class);

  private final File inFile;

  public ColumnarParser(final File inFile) {
    this.inFile = inFile;
    LOG.trace(String.format("Created columnar parser for %s", inFile.getName()));
  }
}
