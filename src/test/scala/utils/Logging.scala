package utils

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  def log: Logger = LoggerFactory.getLogger(this.getClass)
}
