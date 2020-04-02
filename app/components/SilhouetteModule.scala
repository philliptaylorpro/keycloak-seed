package components

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, IdentityService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, SecureRandomIDGenerator}
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.{CookieHeaderEncoding, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class SilhouetteModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {

    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  @Provides
  def provideEnvironment(
    userService: IdentityService[User],
    authenticatorService: AuthenticatorService[CookieAuthenticator],
    eventBus: EventBus
  ): Environment[DefaultEnv] = {

    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-signer") signer: Signer,
    @Named("authenticator-crypter") crypter: Crypter,
    cookieHeaderEncoding: CookieHeaderEncoding,
    fingerprintGenerator: FingerprintGenerator,
    idGenerator: IDGenerator,
    configuration: Configuration,
    clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    //Tweak your cookie handling here.

    val config = CookieAuthenticatorSettings(
      cookieName = "auth",
      cookiePath = "/",
      cookieDomain = None,
      secureCookie = false, // needs to follow whether you've got https on or off. currently would be off for localhost.
      httpOnlyCookie = true,
      useFingerprinting = true,
      cookieMaxAge = None,
      authenticatorIdleTimeout = Some(FiniteDuration(30, MINUTES)),  // idle timeout if user doesn't make any requests
      authenticatorExpiry = FiniteDuration(12, HOURS)                // absolute timeout of session regardless of any activity
    )

    val authenticatorEncoder = new CrypterAuthenticatorEncoder(crypter)
    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoding, authenticatorEncoder, fingerprintGenerator, idGenerator, clock)
  }

  @Provides
  def provideKeycloakProvider(httpLayer: HTTPLayer, socialStateHandler: SocialStateHandler, configuration :Configuration) :KeycloakProvider =
    /* YOU DEFINITELY NEED TO PROVIDE AT LEAST THE CLIENT SECRET HERE */
    /* You can always use: configuration.underlying.getString("my.config.value") if you need config access in this file */
    new KeycloakProvider(httpLayer, socialStateHandler, OAuth2Settings(
      authorizationURL = Some("http://localhost:8080/auth/realms/master/protocol/openid-connect/auth"),
      accessTokenURL = "http://localhost:8080/auth/realms/master/protocol/openid-connect/token",
      apiURL = Some("http://localhost:8080/auth/realms/master/protocol/openid-connect/userinfo"),
      redirectURL = Some("http://localhost:9000/login"),
      clientID = "keycloak-seed",
      clientSecret = "f1e36b35-8b7f-4a83-8292-c3166559736f",
      scope = Some("email")  // list what data you want.
    ))

  @Provides
  def socialProviderRegistry(keycloakProvider: KeycloakProvider) :SocialProviderRegistry =
    SocialProviderRegistry(Seq(keycloakProvider))

  @Provides
  def provideSocialStateHandler(@Named("authenticator-signer") signer: Signer) :SocialStateHandler =
    new DefaultSocialStateHandler(Set.empty, signer)

  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  @Named("authenticator-signer")
  def provideAuthenticatorSigner(): Signer = { // you should replace these with config'd secrets.
    val config = JcaSignerSettings("HIhisdhiOHHh*6&&*6d786s786a78d6a78a8dsadHUIHIUHDUAHUI*")
    new JcaSigner(config)
  }

  @Provides
  @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(): Crypter = {
    val config = JcaCrypterSettings("HiohhHHIohioho665^&%^&%D^&%S^%D%A&d5&^A%HHUHHUIIHUDWWWW&^")
    new JcaCrypter(config)
  }

  @Provides
  def userService() :IdentityService[User] = {
    new UserIdentityServiceImpl()
  }

}

trait DefaultEnv extends Env {    // This is the most important trait in the app. It tells Silhouette what Type our
                                  // User object will be and what our session management mechanism is. Any method signature
                                  // in Silhouette that says DefaultEnv#I you know is synonymous with your User class.
  type I = User
  type A = CookieAuthenticator
}

                                   // This is my permissions model. There's Edit for super users and View for normies.
sealed trait Permission
case object ViewPermission extends Permission
case object EditPermission extends Permission

case class User (                  // This describes a User in my system. It's available with every request on SecuredActions.
  name :String,
  email :String,
  permission :Permission,
  // any properties about user could go here.
  // you're free to model them as you wish
  // such as favourite day of the week, avatar etc.
  loginInfo: LoginInfo            // Mandatory minimum description of a user. Thing that ends up encrypted inside the user's cookie.
) extends Identity

// Given a LoginInfo (which is the data stored in a cookie), which is just the provider and key such as "keycloak", "phill.taylor@example.com"
// this class expands it to a full on User object for each request.

class UserIdentityServiceImpl extends IdentityService[User] {
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future {
    loginInfo match {
      // normally you'd match on providerId and lookup the user in a db or something.
      case LoginInfo("keycloak", "ben@example.com") => Some(User("Ben", "ben@example.com", EditPermission, loginInfo))
      case LoginInfo("keycloak", "karen@example.com") => Some(User("Karen", "karen@example.com", ViewPermission, loginInfo))
                    // when I set up my test user in keycloak I used this email so login from that system would tie up a record in
                    // this fake database.
      case LoginInfo("keycloak", "sinclair@example.com") => Some(User("Sinclair", "sinclair@example.com", EditPermission, loginInfo))
      case _ => None
    }
  }
}

                                   // This function can be called from our Controller Actions to check user permissions
                                   // and do fine grained access control before the Action actually runs.
                                   // See an example of permissions with the endpoint: HomeController.editPage
case class HasRole(permissionRequired :Permission) extends Authorization[User, CookieAuthenticator] {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B]) = Future {

    val userHasPermission = (permissionRequired == identity.permission)
    val editIsGreaterThanViewPermission = ((permissionRequired == ViewPermission) && (identity.permission == EditPermission))
    userHasPermission || editIsGreaterThanViewPermission

  }
}
