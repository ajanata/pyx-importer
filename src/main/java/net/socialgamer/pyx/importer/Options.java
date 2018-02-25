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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;


public class Options {

  private final OptionParser parser;
  private final OptionSpec<File> conf;
  private final OptionSpec<Boolean> format;
  private final OptionSpec<Void> help;
  private final OptionSpec<Boolean> saveToDb;
  private final OptionSpec<Void> schemaOnly;
  private final OptionSet opts;

  public Options(final String[] args) {
    parser = new OptionParser(false);
    help = parser.acceptsAll(Arrays.asList("h", "help"),
        "Print this usage information.");
    conf = parser.acceptsAll(Arrays.asList("c", "configuration"), "Configuration file to use.")
        .withRequiredArg()
        .describedAs("filename")
        .ofType(File.class)
        .defaultsTo(new File("importer.properties"));
    format = parser.accepts("format", "Process rich-text formatting for card text.")
        .withOptionalArg()
        .ofType(Boolean.class)
        .defaultsTo(Boolean.TRUE);
    saveToDb = parser.accepts("save", "Save parse results to database.")
        .withOptionalArg()
        .ofType(Boolean.class)
        .defaultsTo(Boolean.TRUE);
    schemaOnly = parser.accepts("schema", "Output the required database schema and exit.");

    opts = parser.parse(args);
  }

  public void showUsageAndExit(final PrintStream sink, final int exitCode)
      throws IOException {
    sink.println(String.format("USAGE: %s [options]", CardImporter.class.getSimpleName()));
    sink.println();
    parser.printHelpOn(sink);
    System.exit(exitCode);
  }

  public boolean wantsHelp() {
    return opts.has(help);
  }

  public File getConfFile() {
    return opts.valueOf(conf);
  }

  public boolean wantsFormatText() {
    return opts.valueOf(format);
  }

  public boolean wantsSaveToDatabase() {
    return opts.valueOf(saveToDb);
  }

  public boolean outputScheamOnly() {
    return opts.has(schemaOnly);
  }
}
