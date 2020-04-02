
# keycloak-seed

*The annotated Silhouette keycloak example project*

This is an annotated example program that shows how you can add a "single sign on" feature to your application using the Silhouette library, a Keycloak server and an experimental KeycloakProvider plugin.

This code was developed by using `sbt new playframework/play-scala-seed.g8` and then adding Silhouette to it which I feel more accurately represents how people actually go about creating new Play applications.

Keycloak supports many different ways of connecting including OpenId Connect and SAML. It includes implicit flows and many variations. The way this library works we're just using the OAuth2 subset of OpenId Connect.

This implementation uses an OAuth2 mechanism to keycloak to ensure the user is succesfully logged in, and then takes the email address and maps it to a harcoded list of email and permissions inside this app. You may prefer a solution that relies more heavily on Keycloak but I'm sure any keen developer can remove those pieces and take the most interesting aspects out of the application.

#### Prerequisites

In order to use this application you need to setup a real Keycloak server with some minimal configuration. I have written a blog post here:

https://blog.philliptaylor.net/quickly-configure-a-keycloak-server-for-single-sign-on/

I will be writing a blog post about this application on my website which I will link to when it is done.

#### Starting the demo

1. Follow the guide in the blog post above to get a Keycloak server available
2. There is some hardcoded config in SilhouetteModule.scala line ~80 where you must enter the URLs to keycloak as well as your `clientId` and `clientSecret`.

Then you're ready to start the application with `sbt run` and try the website out in your browser at http://localhost:9000/.

If the demo needs some more tweaking:

1. Further down in SilhouetteModule.scala line ~151 are some hardcoded emails addresses you might want to change to match your keycloak accounts email addresses'.
2. In KeycloakProvider.scala line ~51 is where we parse the user data from Keycloak so if you had more data coming back from Keycloak (permissions, department, etc) you may want to look at that code.

#### Versions

This was written against Play 2.8 and Silhouette 7.0

Phillip Taylor