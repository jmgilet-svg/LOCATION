package com.location.client.ui.uikit;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Tiny logger facade over java.util.logging. */
public final class Log {
  private final Logger logger;

  private Log(Class<?> cls) {
    this.logger = Logger.getLogger(cls.getName());
  }

  public static Log get(Class<?> cls) {
    return new Log(cls);
  }

  public void info(String msg) {
    logger.log(Level.INFO, msg);
  }

  public void warn(String msg) {
    logger.log(Level.WARNING, msg);
  }

  public void error(String msg, Throwable t) {
    logger.log(Level.SEVERE, msg, t);
  }
}
