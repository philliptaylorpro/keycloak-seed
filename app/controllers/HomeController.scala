package controllers

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import components.{DefaultEnv, EditPermission, HasRole, KeycloakProvider, User}
import javax.inject._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject
  (
    // use this on all Controllers to get the actions
    silhouette: Silhouette[DefaultEnv],
    // only required for login controller
    userService :IdentityService[User],
    keycloakProvider :KeycloakProvider
  )(implicit ec: ExecutionContext, val controllerComponents: ControllerComponents) extends BaseController {

  def index() = silhouette.UserAwareAction { implicit request =>

    // a user aware action has an optional request.identity and request.authenticator set.
    // we can look at these to determine if a user is logged in and who the user is.

    (request.authenticator, request.identity) match {
      case (None, None) =>
        println("Serving index page: User isnt authenticated!")
      case (Some(_), Some(user)) =>
        println(s"Serving index page: Welcome back ${user.name}.")
        println(s"Your email is ${user.email}")
        println(s"Your loginInfo object reads: ${user.loginInfo}")
    }

    Ok(views.html.index(request.identity))
  }

  def login = silhouette.UserAwareAction.async { implicit request =>
    if (request.identity.isDefined) {
      println(s"User ${request.identity.map(_.email).getOrElse("")} turned up at login page so we quickly log them out before showing it to them.")
      Future.successful(Redirect(routes.HomeController.logout()))
    } else {
      keycloakProvider.authenticate().flatMap {
        // Left(_) may not denote failure, it can be recursive redirects to more and more upstream keycloak servers,
        // therefore we have to return the result to the user in case it is a redirect.
        case Left(result) => Future.successful(result)
        case Right(authInfo) =>
          println(s"We checked the user's code and now we have a token: ${authInfo.accessToken}")
          keycloakProvider.retrieveProfile(authInfo).flatMap { userInfo =>
            println(s"Using userInfo endpoint, now we have their details: ${userInfo}")
            // map it to a user with a permission in our system.
            userInfo.email match {
              case Some(userEmail) =>
                val loginInfo = LoginInfo("keycloak", userEmail)
                println(s"Users identity token: ${loginInfo}")
                userService.retrieve(loginInfo).flatMap {    // this is giving me a change to check the user's email ties up with the ones in my hardcoded list
                                                             // you can skip this is you want any sso user to be able to log in.
                  case Some(fullUserObject) =>
                    silhouette.env.authenticatorService.create(loginInfo).flatMap { authenticator =>
                      silhouette.env.authenticatorService.init(authenticator).flatMap { result =>
                        println(s"User ${fullUserObject.name} is now officially logged in.")
                        /* embed and discard are how you "bind" the cookie/auth mechanism to the user's browser
                           by means of creating a redirect/content/response you want to give them but letting
                           the authenticator apply it's cookie/whatever stuff to it.
                         */
                        silhouette.env.authenticatorService.embed(result, Redirect(routes.HomeController.index()).flashing("success" -> "Login Successful"))
                      }
                    }
                  case None =>
                    println(s"You are ${userEmail} ok but we dont have that entry hardcoded in SilhouetteModule.scala. Hack this app to make this demo work!")
                    Future.successful(Ok("You are ${userEmail} ok but we dont have that entry hardcoded in SilhouetteModule.scala. Hack this app!"))
                }
              case None =>
                println(s"Login failed. You're single signed in okay but the account doesn't have an email address. Go back to keycloak and add one")
                Future.successful(Ok("Your keycloak user doesn't have an associated email address. Go back to keycloak and add one."))
            }
          }
        }
    }
  }

  def logout = silhouette.SecuredAction.async { implicit request =>
    println(s"logging out: ${request.identity.email}")

    /* The typical implementation just logs the user out locally from our app by discarding
       the authenticator:

          silhouette.env.authenticatorService.discard(
            request.authenticator,
            Redirect(routes.HomeController.login())
          )

       Logging out doesn't appear to be a part of the design of Silhouette as far as I can see
       and not properly and recursively in Keycloak but you can logout of the immediate provider
       if you do redirect the logged out user. In my own experience it's useful for allowing them
       to switch between identity providers (o365, ldap etc). This url is the "End Session Endpoint"
       taken from my setup blog post (in the readme).
     */

    val finalLocationEncoded = java.net.URLEncoder.encode("http://localhost:9000/", "UTF-8")

    silhouette.env.authenticatorService.discard(
      request.authenticator,
      Redirect(s"http://localhost:8080/auth/realms/master/protocol/openid-connect/logout?client_id=master&redirect_uri=${finalLocationEncoded}")
        .flashing("success" -> "Logout successful")
    )
  }

  def viewPage = silhouette.SecuredAction { implicit request =>
    Ok(views.html.vip())
  }

  // The HasRole() function in the title here is how you can easily lock down Actions
  // Test upgrading and downgrading your users by editing SilhouetteModule.UserIdentityServiceImpl where
  // we assign some permissions to keycloak users.

  def editPage = silhouette.SecuredAction(HasRole(EditPermission)) { implicit request =>
    println(s"User ${request.identity.email} was allowed to post here. saving changes might happen here if we implemented it!")
    Redirect(routes.HomeController.viewPage()).flashing("success" -> "Update applied by authorised user")
  }

}
