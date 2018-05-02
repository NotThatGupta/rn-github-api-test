package utils

import scalaj.http.{Http, HttpRequest}

trait HttpWrapper {
  val connectionTimeoutMs: Int = 10000
  val socketTimeoutMs: Int = 30000

  def http (url: String): HttpRequest = {
    Http(url).timeout(connectionTimeoutMs, socketTimeoutMs)
  }
}
