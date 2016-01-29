# Scalable Chat #

* A demo multiuser application with a scalable architecture.
* A chat room application
* Demonstrates akka-persistence by storing chat history
* Demonstrates akka-http for client/server communications with WebSockets

### Technology Stack ###

* ScalaFX front end
* Akka in both client and server
* Akka Streams and Akka I/O used for client server communications
  * Currently there is an `akka-streams` branch in progress for introducing akka-http and akka-streams to replace direct use of akka-io.
* Akka Cluster with cluster-aware routing
* Event-sourceing using Akka Persistence with Cassandra
* Akka Testkit and Specs2 testing

### Future Plans and Possibilities ###

Work on packaging as chat room libraries to enable embedding in other applications.

Instead of using cluster-aware routing for users and chatrooms,
use Akka Cluster Sharding in combination with akka-persistence.

Also, instead of using Akka TCP sockets, use Akka HTTP with websockets for
communication between client and server.

Build chatroom functionality into libraries that can be embedded in other applications.

Build an additional browser-based JavaScript front end, perhaps using Scala.js.

Make logins more secure.

See the [Issues](https://github.com/ezoerner/scalable-chat/issues) for other planned improvements.

### Running ###

* Requires Java 8 (though the project is written entirely in Scala)
* Configuration files are in `server/src/main/resources/application.conf` and `client/src/main/resources/application.conf` for server and client respectively.
  This is where the IP address and/or port can be changed for the server socket.
* To run the server:  `./activator server/run` (or use your own activator installation)
* To run a ScalaFX client:  `./activator client/run`
    * Note that there currently is no separate registration step. If this is the first time logging in for a user, just type in a new password at the login prompt.
* Dependencies: akka , cassandra, scalafx, scalafxml
* Database Configuration: TBD
* How to run tests: `./activator test`
* Deployment instructions: TBD
