package utils

import org.scalatest.{FunSpec, MustMatchers}
import scalaj.http.HttpResponse

trait HttpResponseAssertion extends FunSpec with MustMatchers with Logging {
  def assertResponseCode[T](response: HttpResponse[T], code: Int*): Unit = {
    if (!(code contains response.code)) {
      val clue = s"""
               | Unexpected response code: expected $code but found ${response.code}
               |
               | ${response.body}
               |
               | """.stripMargin
      log.error(clue)
      withClue(clue) {
        response.code must be (code)
      }
    }
  }
}
