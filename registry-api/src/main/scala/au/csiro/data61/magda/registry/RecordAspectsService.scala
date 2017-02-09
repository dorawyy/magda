package au.csiro.data61.magda.registry

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import scalikejdbc._
import akka.http.scaladsl.model.StatusCodes
import io.swagger.annotations._
import gnieh.diffson.sprayJson._
import spray.json.JsObject

import scala.util.Failure
import scala.util.Success

@Path("/records/{recordId}/aspects")
@io.swagger.annotations.Api(value = "record aspects", produces = "application/json")
class RecordAspectsService(system: ActorSystem, materializer: Materializer) extends Protocols with SprayJsonSupport {
  @ApiOperation(value = "Get a list of all aspects of a record", nickname = "getAll", httpMethod = "GET", response = classOf[JsObject], responseContainer = "Map")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "recordId", required = true, dataType = "string", paramType = "path", value = "ID of the record for which to fetch aspects.")
  ))
  def getAll = get { path(Segment / "aspects") { (recordId: String) =>
    DB readOnly { session =>
      RecordPersistence.getById(session, recordId) match {
        case Some(result) => complete(result.aspects)
        case None => complete(StatusCodes.NotFound, BadRequest("No record exists with that ID."))
      }
    }
  } }

  @Path("/{aspectId}")
  @ApiOperation(value = "Get a record aspect by ID", nickname = "getById", httpMethod = "GET", response = classOf[JsObject])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "recordId", required = true, dataType = "string", paramType = "path", value = "ID of the record for which to fetch an aspect."),
    new ApiImplicitParam(name = "aspectId", required = true, dataType = "string", paramType = "path", value = "ID of the aspect to fetch.")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "No record or aspect exists with the given IDs.", response = classOf[BadRequest])
  ))
  def getById = get { path(Segment / "aspects" / Segment) { (recordId: String, aspectId: String) => {
    DB readOnly { session =>
      RecordPersistence.getRecordAspectById(session, recordId, aspectId) match {
        case Some(recordAspect) => complete(recordAspect)
        case None => complete(StatusCodes.NotFound, BadRequest("No record aspect exists with that ID."))
      }
    }
  } } }

  @Path("/{aspectId}")
  @ApiOperation(value = "Modify a record aspect by ID", nickname = "putById", httpMethod = "PUT", response = classOf[JsObject],
    notes = "Modifies a record aspect.  If the aspect does not yet exist on this record, it is created.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "recordId", required = true, dataType = "string", paramType = "path", value = "ID of the record for which to fetch an aspect."),
    new ApiImplicitParam(name = "aspectId", required = true, dataType = "string", paramType = "path", value = "ID of the aspect to fetch."),
    new ApiImplicitParam(name = "aspect", required = true, dataType = "spray.json.JsObject", paramType = "body", value = "The record aspect to save.")
  ))
  def putById = put { path(Segment / "aspects" / Segment) { (recordId: String, aspectId: String) => {
    entity(as[JsObject]) { aspect =>
      DB localTx { session =>
        RecordPersistence.putRecordAspectById(session, recordId, aspectId, aspect) match {
          case Success(result) => complete(result)
          case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
        }
      }
    }
  } } }

  @Path("/{aspectId}")
  @ApiOperation(value = "Modify a record aspect by applying a JSON Patch", nickname = "patchById", httpMethod = "PATCH", response = classOf[JsObject],
    notes = "The patch should follow IETF RFC 6902 (https://tools.ietf.org/html/rfc6902).")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "recordId", required = true, dataType = "string", paramType = "path", value = "ID of the record for which to fetch an aspect."),
    new ApiImplicitParam(name = "aspectId", required = true, dataType = "string", paramType = "path", value = "ID of the aspect to fetch."),
    new ApiImplicitParam(name = "aspectPatch", required = true, dataType = "gnieh.diffson.JsonPatchSupport$JsonPatch", paramType = "body", value = "The RFC 6902 patch to apply to the aspect.")
  ))
  def patchById = patch { path(Segment / "aspects" / Segment) { (recordId: String, aspectId: String) => {
    entity(as[JsonPatch]) { aspectPatch =>
      DB localTx { session =>
        RecordPersistence.patchRecordAspectById(session, recordId, aspectId, aspectPatch) match {
          case Success(result) => complete(result)
          case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
        }
      }
    }
  } } }

  val route =
      getAll ~
      getById ~
      putById ~
      patchById
}
