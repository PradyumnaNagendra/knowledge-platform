package org.sunbird.graph.schema

import org.sunbird.graph.engine.dto.ProcessingNode
import org.sunbird.graph.schema.validator.{BaseDefinitionNode, FrameworkValidator, RelationValidator, SchemaValidator, VersioningNode}
import org.sunbird.graph.validator._

class DefinitionNode(graphId: String, objectType: String, version: String = "1.0") extends BaseDefinitionNode(graphId , objectType, version) with VersioningNode with RelationValidator with FrameworkValidator with SchemaValidator {

    def getOutRelationObjectTypes: List[String] = outRelationObjectTypes
}
