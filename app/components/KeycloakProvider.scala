package components

// We should try and push this file upstream into Silhouette itself so it's less of a maintenance/test burden for us
// This is largely the VK provider copied and pasted but with the userInfo http get turned into a post and slightly
// different social profile fields to accommodate Keycloak.

import io.github.honeycombcheesecake.play.silhouette.api.LoginInfo
import io.github.honeycombcheesecake.play.silhouette.api.util.HTTPLayer
import io.github.honeycombcheesecake.play.silhouette.impl.exceptions.ProfileRetrievalException
import io.github.honeycombcheesecake.play.silhouette.impl.providers.OAuth2Provider._
import io.github.honeycombcheesecake.play.silhouette.impl.providers._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.Future

trait BaseKeycloakProvider extends OAuth2Provider {

  override type Content = JsValue

  override val id = KeycloakProvider.ID

  override protected val urls = Map("api" -> settings.apiURL.getOrElse(throw new Exception("apiURL must be set on OAuth2Settings for KeycloakProvider")))
  override implicit protected val accessTokenReads: Reads[OAuth2Info] = KeycloakProvider.infoReads

  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    println(s"posting to ${urls("api")}")
    httpLayer.url(urls("api")).post(Map(
      "access_token" -> authInfo.accessToken
    )).flatMap { response =>
      println(s"Raw response from keycloak: ${response.body} (status code: ${response.status})")
      val json = response.json
      (json \ "error").asOpt[JsObject] match {
        case Some(error) =>
          val errorCode = (error \ "error").as[Int]
          val errorMsg = (error \ "error_description").as[String]

          throw new ProfileRetrievalException(KeycloakProvider.SpecifiedProfileError.format(id, errorCode, errorMsg))
        case _ => profileParser.parse(json, authInfo)
      }
    }
  }
}

class KeycloakProfileParser extends SocialProfileParser[JsValue, CommonSocialProfile, OAuth2Info] {

  // If you're exporting more data from Keycloak, e.g. permission settings, birthdays, and whatever else
  // you can write the function below to make it available to you.

  override def parse(json: JsValue, authInfo: OAuth2Info) = Future.successful {
    println(s"Raw Profile JSON: ${json}")
    CommonSocialProfile(
      loginInfo = LoginInfo(KeycloakProvider.ID, (json \ "email").as[String]),
      firstName = (json \ "given_name").asOpt[String],
      lastName = (json \ "family_name").asOpt[String],
      fullName = (json \ "name").asOpt[String],
      email = (json \ "email").asOpt[String],
      avatarURL = (json \ "avatarURL").asOpt[String]
    )
  }
}

class KeycloakProvider(
  protected val httpLayer: HTTPLayer,
  protected val stateHandler: SocialStateHandler,
  val settings: OAuth2Settings)
  extends BaseKeycloakProvider with CommonSocialProfileBuilder {

  override type Self = KeycloakProvider
  override val profileParser = new KeycloakProfileParser
  override def withSettings(f: (Settings) => Settings) = new KeycloakProvider(httpLayer, stateHandler, f(settings))
}

object KeycloakProvider {

  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error code: %s, message: %s"
  val ID = "keycloak"

  implicit val infoReads: Reads[OAuth2Info] = (
    (__ \ AccessToken).read[String] and
      (__ \ TokenType).readNullable[String] and
      (__ \ ExpiresIn).readNullable[Int] and
      (__ \ RefreshToken).readNullable[String] and
      (__ \ "email").readNullable[String]
    )((accessToken: String, tokenType: Option[String], expiresIn: Option[Int], refreshToken: Option[String], email: Option[String]) =>
    new OAuth2Info(accessToken, tokenType, expiresIn, refreshToken, email.map(e => Map("email" -> e)))
  )
}

