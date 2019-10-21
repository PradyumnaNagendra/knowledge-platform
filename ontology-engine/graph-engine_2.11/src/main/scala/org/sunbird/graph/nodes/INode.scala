package org.sunbird.graph.nodes

import org.sunbird.common.dto.Request
import org.sunbird.graph.dac.model.Node

import scala.concurrent.{ExecutionContext, Future}

abstract class INode {

    /**
      * Creates Node with relations and external properties
      * @param request Request
      * @param ec ExecutionContext
      * @return Future[Node]
      */
    def create(request: Request)(implicit ec: ExecutionContext) : Future[Node]


    /**
      * Updates Node with relations and external properties
      * @param request Request
      * @param ec ExecutionContext
      * @return Future[Node]
      */
    def update(request: Request)(implicit ec: ExecutionContext) : Future[Node]


    /**
      * Fetches node with relations and fields
      * @param request Request
      * @param ec ExecutionContext
      * @return Future[Node]
      */
    def read(request: Request)(implicit ec: ExecutionContext) : Future[Node]

    /**
      * Deletes node, relation and external properties
      * @param request Request
      * @param ec ExecutionContext
      * @return Future[String]
      */
    def delete(request: Request)(implicit ec: ExecutionContext): Future[String]

}
