# Scalable App #

### What is this repository for? ###

* A sample multiuser application with a scalable architecture.

### Technology Stack ###

* ScalaFX client
* Akka in both client and server
* Akka I/O used for client server communications
* Message serialization in BSON format
* Akka Persistence with Cassandra
* Akka Testkit and Specs2 testing

### How do I get set up? ###

* Requires Java 8
* To run the server:  `./activator server/run`
* To run a ScalaFX client:  `./activator client/run`
* Configuration is in application.conf
* Dependencies: akka , cassandra, scalafx/scalafxml, reactive-mongo-bson
* Database Configuration: TBD
* How to run tests: `./activator test`
* Deployment instructions: TBD

### Who do I talk to? ###

* Repo owner and admin: [ezoerner](https://bitbucket.org/ezoerner)
