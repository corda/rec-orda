# REC-orda Client

## Quickstart

1. Run `./gradlew clean deployNodes` in the root directory.
2. Run all nodes using `./build/nodes/runnodes`
3. Update `resources/application.properties` to point to the absolute filepath of PartyA's CorDapps folder.
4. Run `Server.kt` from Intellij, this will connect to the node PartyA as specified in `resources/application.properties`.
5. Import Postman collection in the root of the Client project, `RECorda.postman_collection.json`.
6. Execute GET requests to test.

## TO-DO

The `Issue Tokens` POST request will fail on the response due to the response being of type `SignedTransaction`.  Return a `String` (or something JSON like)
back and this should work fine - I've tested it to work when returning a `notarised.toString()`, however it broke some of the flow tests so you'll probably
need to change these too.
