# Reviews

## About
Web server with an endpoint to query for the products with the best reviews, based on Amazon-style review data.

## Technical stack
Let's use Functional Programming in Scala, using:
* *Cats Effects*: pure asynchronous run-time
* *Http4s*: minimal, idiomatic Scala interface for HTTP service
* *Circe*: JSON (de)serialization
* *Ciris*: functional configurations
* *MUnit*: testing 

## Instructions
Prerequisite: you need to have *sbt* installed on your computer.

To start the server, run this command in the terminal/CLI:
```
sbt run
```
This will start a web server on port `8080`, and fetch data from the file `./src/main/resources/amazon-reviews.json`.
Note that both those properties are configurable.

Requests can be posted like this:
```
curl http://localhost:8080/amazon/best-rated \
-d '{"start": "01.01.2010", "end": "31.12.2020", "limit": 2, "min_number_reviews": 2}'
```

If you want to specify other values for the properties than the default, you can do as follows:
```
sbt run -Dfile.path="./src/test/resources/test-data.json" -Dserver.port=9000
```

## Assumptions/decisions

* the formats of the fields contained in a review are extrapolated from the data available in the example JSON files:
  * a product id ("asin") is a string matched by the regex `^[A-Z0-9]{1,64}$`.
  * a rating ("overall") is an integer from 0 to 5, represented as a float in the JSON files (doh!).
* the validation of the request body will accumulate errors in case of BadRequest (status 400), in order to be as descriptive as possible to the client.
* if any entry in the file is impossible to parse, it will result in an InternalServerError (status 500). Another possible choice would have been to ignore that entry and proceed with the deserialization of the file.
* to avoid the boilerplate of monad transformers, we'll use the error channel of the effect; i.e. instead of returning `F[Either[Err, A]]`, we return `F[A]`, with `F` raising errors. The equivalent in Java world to use unchecked exceptions instead of checked exceptions.
* "the file might be too large to fit into memory"; i.e. we cannot keep all reviews in memory. In order to spare with the  memory:
  * only deserialize what is needed (e.g. we do not care to deserialize "reviewText").
  * use an iterator when reading the file and parsing the reviews.

## To do/play with

* more tests! E.g. for the computation of best rated products; note that this is tested indirectly in the integration test, which could be populated with more examples.
* cache the list of average ratings between 2 dates.
* use ScalaCheck and generate invalid/valid inputs in order to test validation of request body.
* API documentation. Using Swagger maybe? But a bit overkill for one single and simple endpoint...
* authorization in the endpoint.
* dockerize it.
