package au.csiro.data61.magda.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.OK
import au.csiro.data61.magda.api.model.SearchResult
import au.csiro.data61.magda.model.misc.{DataSet, _}
import au.csiro.data61.magda.test.util.ApiGenerators._
import au.csiro.data61.magda.test.util.Generators
import org.scalacheck.{Shrink, _}

class FacetPublisherSpec extends FacetSpecBase {

  describe("facets publisher") {

      def queryGen(dataSets: List[DataSet]) = for {
        publishers <- Generators.smallSet(publisherQueryGen(dataSets))
      } yield new Query(publishers = publishers)

      describe("should have identifiers except user selected option with 0 hitCount") {
        implicit val stringShrink: Shrink[List[Agent]] = Shrink { _ =>
          Stream.empty
        }

        it("in general") {
          val gen = for {
            index <- indexGen
            textQuery <- textQueryGen(queryGen(index._2))
            facetSize <- Gen.posNum[Int]
          } yield (index, textQuery, facetSize)

          try {
            forAll(gen) {
              case (tuple, textQuery, facetSize) ⇒
                val (_, dataSets, routes) = tuple

                val publishers = dataSets.flatMap(_.publisher).distinct

                val publisherLookup = publishers
                  .groupBy(_.name.get.toLowerCase)

                Get(s"/v0/datasets?${textQuery._1}&start=0&limit=0&facetSize=${Math.max(facetSize, 1)}") ~> addSingleTenantIdHeader ~> routes ~> check {
                  status shouldBe OK

                  val result = responseAs[SearchResult]

                  val facet = result.facets.get.find(_.id.equals(Publisher.id)).get

                  withClue("publishers " + publisherLookup) {
                    facet.options.foreach { x =>
                      val matchedPublishers = publisherLookup.get(x.value.toLowerCase)
                      if(matchedPublishers.isDefined && ( !x.matched || x.hitCount != 0 )) {
                        matchedPublishers.get.exists(publisher => publisher.identifier.get.equals(x.identifier.get)) should be(true)
                      }
                    }
                  }
                }
            }
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              throw e
          }
        }
      }
    }
}
