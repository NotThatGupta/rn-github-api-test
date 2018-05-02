package com.github.api.repositories

import org.scalatest.{FunSpec, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import scalaj.http.HttpResponse
import utils.{HttpResponseAssertion, HttpWrapper, Logging}

import scala.util.{Failure, Success, Try}

class BasicRepositorySearchTests extends FunSpec
  with MustMatchers
  with HttpWrapper
  with HttpResponseAssertion
  with Logging {

  private val githubApiBaseURL = "api.github.com"
  private val searchRepositoriesEndpoint = s"https://$githubApiBaseURL/search/repositories"
  private val reposEndpoint = s"https://$githubApiBaseURL/repos"

  describe("A basic, unauthenticated keyword search (with over 1000 results expected)") {
    val searchRequestURL: String = s"$searchRepositoriesEndpoint?q=tetris"
    val keywordSearchResponseString: HttpResponse[String] = http(searchRequestURL).asString
    val keywordSearchResponseJson: JsValue = Json.parse(keywordSearchResponseString.body).as[JsValue]
    val itemsList: List[JsValue] = (keywordSearchResponseJson \ "items").as[List[JsValue]]
    val lastPageURL: String = keywordSearchResponseString.headers.get("Link").get(0).split(",")(1).split(";")(0)
    val numberOfPages: Int = lastPageURL takeRight 3 dropRight 1 toInt

    it("should return no results that do not match the keyword in the name, description, or readme") {
      for (item <- itemsList) {
        val repoName: String = (item \ "name").validate[String].getOrElse("NAME NOT FOUND").toLowerCase
        val repoDescription: String = (item \ "description").validate[String].getOrElse("DESCRIPTION NOT FOUND").toLowerCase
        val repoFullName: String = (item \ "full_name").validate[String].getOrElse("DESCRIPTION NOT FOUND").toLowerCase
        withClue(s"https://github.com/$repoFullName\n") {
          assert(
            (repoName contains "tetris")
              || (repoDescription contains "tetris")
              || keywordPresentInReadme("tetris", repoFullName)
          )
        }
      }
    }

    it("should return 30 repositories per page") {
      itemsList.size must be(30)
    }

    it("should return 34 pages") {
      numberOfPages must be(34)
    }

    it("should return results sorted by score (desc)") {
      val scoresList: List[Double] = (keywordSearchResponseJson \ "items" \\ "score").map(_.as[Double]).toList
      scoresList.reverse mustBe sorted
    }

    it("should return both forked and unforked repos") {
      assertBothForkedAndUnforkedReposReturned(searchRequestURL, numberOfPages)
    }

    it("should return no private repos") {
      for (page <- 1 to numberOfPages) {
        val keywordSearchResponseJson = getResponseFromNextPage(searchRequestURL, page)
        val publicRepoList: List[Boolean] = (keywordSearchResponseJson \ "items" \\ "private").map(_.as[Boolean]).toList
        publicRepoList.filter(_ == true).length must be(0)
      }
    }

    it("should return both archived and unarchived repos") {
      assertBothArchivedAndUnarchivedReposReturned(searchRequestURL, numberOfPages)
    }
  }

  def assertBothArchivedAndUnarchivedReposReturned(searchRequestURL: String, numberOfPages: Int): Boolean = {
    for (page <- 1 to numberOfPages) {
      val keywordSearchResponseJson = getResponseFromNextPage(searchRequestURL, page)
      val archivedList: List[Boolean] = (keywordSearchResponseJson \ "items" \\ "archived").map(_.as[Boolean]).toList
      Try(assert(
        (archivedList.filter(_ == true).length > 0)
          && (archivedList.filter(_ == false).length > 0)
      )) match {
        case Success(s) => return true
        case Failure(e) =>
      }
    }
    false
  }

  def assertBothForkedAndUnforkedReposReturned(searchRequestURL: String, numberOfPages: Int): Boolean = {
    for (page <- 1 to numberOfPages) {
      val keywordSearchResponseJson = getResponseFromNextPage(searchRequestURL, page)
      val forkedList: List[Int] = (keywordSearchResponseJson \ "items" \\ "forks_count").map(_.as[Int]).toList
      Try(assert(
        (forkedList.filter(_ == 0).length > 0)
          && (forkedList.filter(_ != 0).length > 0)
      )) match {
        case Success(s) => return true
        case Failure(e) =>
      }
    }
    false
  }

  def assertRepoReadmeResponse(response: HttpResponse[String]): Try[Unit] = {
    Try(assertResponseCode(response, 200))
  }

  def getResponseFromNextPage(searchRequestURL: String, pageNumber: Int): JsValue = {
    val pageUrl: String = s"$searchRequestURL&page=$pageNumber"
    val keywordSearchResponseJson: JsValue = Json.parse(http(pageUrl).asString.body).as[JsValue]
    return keywordSearchResponseJson
  }

  def keywordPresentInReadme(keyword: String, repoFullName: String): Boolean = {
    val readmeResponse: HttpResponse[String] = http(s"$reposEndpoint/$repoFullName/readme").asString
    val readmeResponseString: String = assertRepoReadmeResponse(readmeResponse) match {
      case Success(s) => readmeResponse.body
      case Failure(e) => return false
    }
    val readmeResponseJson: JsValue = Json.parse(readmeResponseString).as[JsValue]
    val readmeDownloadURL: String = (readmeResponseJson \ "download_url").validate[String].getOrElse("NO DOWNLOAD URL FOUND")
    val readmeText: String = http(readmeDownloadURL).asString.body
    readmeText contains keyword
  }
}
