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
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;

import net.socialgamer.pyx.importer.data.ParseResult;
import net.socialgamer.pyx.importer.filetypes.ConfigurationException;
import net.socialgamer.pyx.importer.filetypes.ExcelFileType;
import net.socialgamer.pyx.importer.filetypes.FileType;
import net.socialgamer.pyx.importer.inject.ImporterModule;
import net.socialgamer.pyx.importer.inject.ImporterModule.OutputSchemaOnly;
import net.socialgamer.pyx.importer.inject.ImporterModule.Schema;
import net.socialgamer.pyx.importer.output.HibernateOutputter;


public class CardImporter {
  private static final Logger LOG = Logger.getLogger(CardImporter.class);

  private final Properties appProps;
  private final boolean schemaOnly;
  private final String schema;
  private final ExcelFileType.Factory excelFactory;
  private final ImportHandler.Factory importHandlerFactory;
  private final HibernateOutputter outputter;

  @Inject
  public CardImporter(final Properties appProps, @OutputSchemaOnly final boolean schemaOnly,
      @Schema final String schema,
      final ExcelFileType.Factory excelFactory, final ImportHandler.Factory importHandlerFactory,
      final HibernateOutputter outputter) {
    this.appProps = appProps;
    this.schemaOnly = schemaOnly;
    this.schema = schema;
    this.excelFactory = excelFactory;
    this.importHandlerFactory = importHandlerFactory;
    this.outputter = outputter;
  }

  public static void main(final String[] args) throws IOException, InterruptedException {
    // Process command-line options
    final Options opts = new Options(args);
    if (opts.wantsHelp()) {
      opts.showUsageAndExit(System.out, 0);
    }

    // Create injector
    final Injector injector = Guice.createInjector(Stage.PRODUCTION, new ImporterModule(opts));
    injector.getInstance(CardImporter.class).doImport();
  }

  private void doImport() {
    if (schemaOnly) {
      System.out.println(schema);
      return;
    }

    final int fileCount = Integer.valueOf(appProps.getProperty("import.file.count", "0"));
    if (fileCount <= 0) {
      System.err.println("Configuration file must specify positive import.file.count.");
      System.exit(1);
    }

    // make sure all of the files are valid before we start doing anything
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

    final ImportHandler handler = importHandlerFactory.create(fileTypes);

    final ParseResult result = handler.process();
    outputter.output(result);
  }
}
