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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;


public class CardImporter {

  private static Properties loadProperties(final File file) throws IOException {
    final Properties props = new Properties();
    props.load(new FileInputStream(file));
    return props;
  }

  private static void showUsageAndExit(final OptionParser optParser, final PrintStream sink,
      final int exitCode) throws IOException {
    sink.println(String.format("USAGE: %s [options]", CardImporter.class.getSimpleName()));
    sink.println();
    optParser.printHelpOn(sink);
    System.exit(exitCode);
  }

  public static void main(final String[] args) throws IOException, InterruptedException {
    // Process command-line options
    final OptionParser optParser = new OptionParser(false);
    final OptionSpec<Void> help = optParser.acceptsAll(Arrays.asList("h", "help"),
        "Print this usage information.");
    final OptionSpec<File> conf = optParser
        .acceptsAll(Arrays.asList("c", "configuration"), "Configuration file to use.")
        .withRequiredArg()
        .describedAs("file")
        .ofType(File.class)
        .defaultsTo(new File("importer.properties"));
    final OptionSpec<File> excelFile = optParser
        .accepts("excel", "Excel input file")
        .withRequiredArg()
        .describedAs("file")
        .ofType(File.class);
    //    final OptionSpec<File> columnarWhite = optParser
    //        .accepts("columnar-white", "Columnar-format white cards")
    //        .withRequiredArg()
    //        .describedAs("file")
    //        .ofType(File.class);

    final OptionSet opts = optParser.parse(args);
    if (opts.has(help)) {
      showUsageAndExit(optParser, System.out, 0);
    }

    // TODO when we need properties file
    //    final File propsFile = opts.valueOf(conf);
    //    if (!propsFile.canRead()) {
    //      System.err.println(String.format("Unable to open configuration file %s for reading.",
    //          propsFile.getAbsolutePath()));
    //      System.err.println();
    //      showUsageAndExit(optParser, System.err, 1);
    //    }

    // TODO when other formats supported, make sure at least one of them is present
    //    if (!opts.has(columnarWhite)) {
    //      System.err.println("columnar-white option must be specified.");
    //      showUsageAndExit(optParser, System.err, 1);
    //    } else {
    //      for (final File file : opts.valuesOf(columnarWhite)) {
    //        final ColumnarParser parser = new ColumnarParser(file);
    //      }
    //    }
    if (!opts.has(excelFile)) {
      System.err.println("excel option must be specified.");
      showUsageAndExit(optParser, System.err, 1);
    } else {
      // make sure all of the files are valid first
      for (final File file : opts.valuesOf(excelFile)) {
        if (!file.canRead()) {
          System.err.println(
              String.format("Unable to open Excel file '%s' for reading.", file.getAbsolutePath()));
          showUsageAndExit(optParser, System.err, 1);
        }
      }

      final RichTextToHtmlFormatHelper helper = new RichTextToHtmlFormatHelper();
      // now that we know they're all valid, start doing stuff
      for (final File file : opts.valuesOf(excelFile)) {
        // TODO put this in its own class
        final FileInputStream is = new FileInputStream(file);
        final Workbook workbook = new XSSFWorkbook(is);
        final Sheet sheet = workbook.getSheetAt(0);

        final List<String> columnHeadings = new ArrayList<>();
        final List<List<String>> values = new ArrayList<>();
        // TODO this needs to handle empty rows and cells
        for (final Row row : sheet) {
          final boolean firstRow = columnHeadings.isEmpty();
          final List<String> rowValues = new ArrayList<>();

          for (final Cell cell : row) {
            if (firstRow) {
              columnHeadings.add(cell.getStringCellValue());
            } else {
              final XSSFRichTextString rtf = (XSSFRichTextString) cell.getRichStringCellValue();
              final String text = helper.format(rtf);
              rowValues.add(text);
            }
          }

          if (!firstRow) {
            values.add(rowValues);
          }
        }
        values.size();
        System.out.println(values);
      }
    }
  }
}
