package controllers.v3

import akka.actor.ActorSystem
import com.google.inject.Singleton
import controllers.BaseController
import javax.inject.Inject
import play.api.mvc.ControllerComponents

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

@Singleton
class ContentController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends BaseController(cc) {

    val objectType = "content"

    def create() = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrElse(objectType, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
        content.putAll(headers)
        getResult("org.sunbird.content.create", getRequest(content, headers, "createContent"))
    }

    def review(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrElse(objectType, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
        content.putAll(headers)
        val request = getRequest(content, headers, "reviewContent")
        request.getContext.put("identifier", identifier)
        getResult("org.sunbird.content.review", request)
    }
}
