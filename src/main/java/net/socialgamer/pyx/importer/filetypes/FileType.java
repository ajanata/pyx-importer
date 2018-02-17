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

package net.socialgamer.pyx.importer.filetypes;

import java.util.Map;
import java.util.Properties;
import java.util.Set;


public abstract class FileType {
  private static final String PROP_PREFIX = "import.file";

  private final Properties props;
  private final int configIndex;

  public FileType(final Properties props, final int configIndex) {
    this.props = props;
    this.configIndex = configIndex;
  }

  /**
   * Validate that the configuration for this file type is valid.
   * @throws ConfigurationException Configuration is invalid; the exception will contain a reason
   * why.
   */
  public abstract void validate() throws ConfigurationException;

  public abstract ParseResult process();

  protected String getProp(final String name) {
    return props.getProperty(String.format("%s[%d].%s", PROP_PREFIX, configIndex, name));
  }

  protected String getProp(final String name, final String defaultValue) {
    return props.getProperty(String.format("%s[%d].%s", PROP_PREFIX, configIndex, name),
        defaultValue);
  }

  public static class ParseResult {
    private final Map<String, Set<String>> blackCards;
    private final Map<String, Set<String>> whiteCards;

    public ParseResult(final Map<String, Set<String>> blackCards,
        final Map<String, Set<String>> whiteCards) {
      this.blackCards = blackCards;
      this.whiteCards = whiteCards;
    }

    public Map<String, Set<String>> getBlackCards() {
      return blackCards;
    }

    public Map<String, Set<String>> getWhiteCards() {
      return whiteCards;
    }
  }
}
