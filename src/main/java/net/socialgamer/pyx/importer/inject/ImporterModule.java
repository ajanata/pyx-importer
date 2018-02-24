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

package net.socialgamer.pyx.importer.inject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

import net.socialgamer.cah.db.PyxBlackCard;
import net.socialgamer.cah.db.PyxCardSet;
import net.socialgamer.cah.db.PyxWhiteCard;
import net.socialgamer.pyx.importer.ImportHandler;
import net.socialgamer.pyx.importer.Options;
import net.socialgamer.pyx.importer.data.DeckInfo;
import net.socialgamer.pyx.importer.filetypes.ExcelFileType;
import net.socialgamer.pyx.importer.parsers.SheetParser;


public class ImporterModule extends AbstractModule {

  private final Options opts;
  private Properties props;

  public ImporterModule(final Options opts) throws IOException {
    this.opts = opts;

    final File propsFile = opts.getConfFile();
    if (!propsFile.canRead()) {
      System.err.println(String.format("Unable to open configuration file %s for reading.",
          propsFile.getAbsolutePath()));
      System.err.println();
      opts.showUsageAndExit(System.err, 1);
    }
  }

  private static Properties loadProperties(final File file) throws IOException {
    final Properties props = new Properties();
    try (Reader reader = new InputStreamReader(new FileInputStream(file),
        Charset.forName("UTF-8"))) {
      props.load(reader);
    }
    return props;
  }

  @Override
  protected void configure() {
    // Load configuration
    try {
      props = loadProperties(opts.getConfFile());
    } catch (final IOException e) {
      throw new RuntimeException("Unable to load properties", e);
    }

    install(ThrowingProviderBinder.forModule(this));
    install(new FactoryModuleBuilder().build(ExcelFileType.Factory.class));
    install(new FactoryModuleBuilder().build(SheetParser.Factory.class));
    install(new FactoryModuleBuilder().build(ImportHandler.Factory.class));

    Names.bindProperties(binder(), props);
    bind(Properties.class).toInstance(props);
  }

  @Provides
  @Singleton
  public SessionFactory provideSessionFactory(@Named("hibernate.dialect") final String dialect,
      @Named("hibernate.driver_class") final String driverClass,
      @Named("hibernate.url") final String connectionUrl,
      @Named("hibernate.username") final String username,
      @Named("hibernate.password") final String password,
      @Named("hibernate.sql.show") final String showSql,
      @Named("hibernate.sql.format") final String formatSql) {
    final Configuration config = new Configuration();

    config.setProperty("hibernate.dialect", dialect);
    config.setProperty("hibernate.connection.driver_class", driverClass);
    config.setProperty("hibernate.connection.url", connectionUrl);
    config.setProperty("hibernate.connection.username", username);
    config.setProperty("hibernate.connection.password", password);
    config.setProperty("hibernate.cache.provider_class",
        "org.hibernate.cache.HashtableCacheProvider");
    config.setProperty("transaction.factory_class",
        "org.hiberante.transaction.JDBCTransactionFactory");
    config.setProperty("show_sql", showSql);
    config.setProperty("format_sql", formatSql);

    config.addAnnotatedClass(PyxBlackCard.class);
    config.addAnnotatedClass(PyxWhiteCard.class);
    config.addAnnotatedClass(PyxCardSet.class);

    return config.buildSessionFactory();
  }

  @Provides
  public Session provideSession(final SessionFactory sessionFactory) {
    return sessionFactory.openSession();
  }

  @Provides
  @Singleton
  @SpecialCharacterReplacements
  public LinkedHashMap<String, String> provideSpecialCharacterReplacements() {
    // iteration order matters for this
    final LinkedHashMap<String, String> map = new LinkedHashMap<>();
    final int count = Integer.parseInt(props.getProperty("replace.count", "0"));
    for (int i = 0; i < count; i++) {
      final String from = props.getProperty(String.format("replace[%d].from", i), "");
      if (from.isEmpty()) {
        throw new RuntimeException(
            "Special character replacement index " + i + " not found or is empty.");
      }
      final String to = props.getProperty(String.format("replace[%d].to", i), "");
      map.put(from, to);
    }
    return map;
  }

  @Provides
  @Singleton
  public Map<String, DeckInfo> provideDeckInfo() {
    final Map<String, DeckInfo> map = new HashMap<>();
    final int count = Integer.parseInt(props.getProperty("deckinfo.count", "0"));
    for (int i = 0; i < count; i++) {
      final String id = props.getProperty(String.format("deckinfo[%d].id", i), "");
      if (id.isEmpty()) {
        throw new RuntimeException("Deck info id for index " + i + " not found or is empty.");
      }
      final String name = props.getProperty(String.format("deckinfo[%d].name", i), id);
      final String watermark = props.getProperty(String.format("deckinfo[%d].watermark", i), "");
      final int weight = Integer
          .parseInt(props.getProperty(String.format("deckinfo[%d].weight", i), "0"));
      final DeckInfo info = new DeckInfo(id, name, watermark, weight);
      map.put(id, info);
      // include it under any remapped name as well
      map.put(name, info);
    }
    return map;
  }

  @Provides
  @Singleton
  @FormatText
  public boolean provideFormatText() {
    return opts.wantsFormatText();
  }

  @Provides
  @Singleton
  @SaveToDatabase
  public boolean provideSaveToDatabase() {
    return opts.wantsSaveToDatabase();
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SpecialCharacterReplacements {
    //
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FormatText {
    //
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SaveToDatabase {
    //
  }
}
