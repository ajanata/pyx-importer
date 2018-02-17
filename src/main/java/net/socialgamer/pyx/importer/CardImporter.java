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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.socialgamer.pyx.importer.filetypes.ConfigurationException;
import net.socialgamer.pyx.importer.filetypes.ExcelFileType;
import net.socialgamer.pyx.importer.filetypes.FileType;
import net.socialgamer.pyx.importer.filetypes.FileType.ParseResult;
import net.socialgamer.pyx.importer.inject.ImporterModule;


public class CardImporter {
  // TODO this class needs to use logger
  private static final Logger LOG = Logger.getLogger(CardImporter.class);

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
    //    final OptionSpec<File> excelFile = optParser
    //        .accepts("excel", "Excel input file")
    //        .withRequiredArg()
    //        .describedAs("file")
    //        .ofType(File.class);
    //    final OptionSpec<File> columnarWhite = optParser
    //        .accepts("columnar-white", "Columnar-format white cards")
    //        .withRequiredArg()
    //        .describedAs("file")
    //        .ofType(File.class);

    final OptionSet opts = optParser.parse(args);
    if (opts.has(help)) {
      showUsageAndExit(optParser, System.out, 0);
    }

    final File propsFile = opts.valueOf(conf);
    if (!propsFile.canRead()) {
      System.err.println(String.format("Unable to open configuration file %s for reading.",
          propsFile.getAbsolutePath()));
      System.err.println();
      showUsageAndExit(optParser, System.err, 1);
    }

    // Load configuration
    final Properties appProps;
    try {
      appProps = loadProperties(propsFile);
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load properties", e);
    }

    // Create injector
    final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ImporterModule(appProps));

    final int fileCount = Integer.valueOf(appProps.getProperty("import.file.count", "0"));
    if (fileCount <= 0) {
      System.err.println("Configuration file must specify positive import.file.count.");
      System.exit(1);
    }

    // make sure all of the files are valid before we start doing anything
    final ExcelFileType.Factory excelFactory = injector.getInstance(ExcelFileType.Factory.class);
    final List<FileType> fileTypes = new ArrayList<>(fileCount);
    for (int i = 0; i < fileCount; i++) {
      final String fileType = appProps.getProperty(String.format("import.file[%d].type", i));
      final FileType impl;
      switch (fileType) {
        case "excel":
          impl = excelFactory.create(i);
          break;
        default:
          LOG.error(String.format("Unknown file type %s for file %d.", fileType, i));
          System.exit(1);
          impl = null;
      }
      try {
        impl.validate();
      } catch (final ConfigurationException e) {
        LOG.error(
            String.format("File %d configuration validation failed: %s", i, e.getMessage()));
        System.exit(1);
      }
      fileTypes.add(impl);
    }

    // and now process them
    for (final FileType fileType : fileTypes) {
      final ParseResult result = fileType.process();

      final Set<String> decks = new HashSet<>();
      decks.addAll(result.getWhiteCards().keySet());
      decks.addAll(result.getBlackCards().keySet());
      System.out.println("Decks:");
      for (final String deck : decks) {
        System.out.println(">" + deck);
      }

      System.out.println("White cards:");
      for (final Entry<String, Set<String>> entry : result.getWhiteCards().entrySet()) {
        System.out.println(">" + entry.getKey());
        for (final String card : entry.getValue()) {
          System.out.println(">>" + card);
        }
      }

      System.out.println("Black cards:");
      for (final Entry<String, Set<String>> entry : result.getBlackCards().entrySet()) {
        System.out.println(">" + entry.getKey());
        for (final String card : entry.getValue()) {
          System.out.println(">>" + card);
        }
      }
    }
  }
}
