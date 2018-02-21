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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import net.socialgamer.pyx.importer.filetypes.ConfigurationException;
import net.socialgamer.pyx.importer.filetypes.ExcelFileType;
import net.socialgamer.pyx.importer.filetypes.FileType;
import net.socialgamer.pyx.importer.filetypes.FileType.ParseResult;
import net.socialgamer.pyx.importer.inject.ImporterModule;


public class CardImporter {
  private static final Logger LOG = Logger.getLogger(CardImporter.class);

  public static void main(final String[] args) throws IOException, InterruptedException {
    // Process command-line options
    final Options opts = new Options(args);
    if (opts.wantsHelp()) {
      opts.showUsageAndExit(System.out, 0);
    }

    // Create injector
    final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ImporterModule(opts));
    // TODO make this better?
    final Properties appProps = injector.getInstance(Properties.class);

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
      LOG.info("Decks:");
      for (final String deck : decks) {
        final int blackCount;
        if (result.getBlackCards().containsKey(deck)) {
          blackCount = result.getBlackCards().get(deck).size();
        } else {
          blackCount = 0;
        }
        final int whiteCount;
        if (result.getWhiteCards().containsKey(deck)) {
          whiteCount = result.getWhiteCards().get(deck).size();
        } else {
          whiteCount = 0;
        }
        LOG.info(String.format(">%s (black: %d, white: %d)", deck, blackCount, whiteCount));
      }

      LOG.trace("White cards:");
      for (final Entry<String, Set<String>> entry : result.getWhiteCards().entrySet()) {
        LOG.trace(">" + entry.getKey());
        for (final String card : entry.getValue()) {
          LOG.trace(">>" + card);
        }
      }

      LOG.trace("Black cards:");
      for (final Entry<String, Set<String>> entry : result.getBlackCards().entrySet()) {
        LOG.trace(">" + entry.getKey());
        for (final String card : entry.getValue()) {
          LOG.trace(">>" + card);
        }
      }
    }
  }
}
