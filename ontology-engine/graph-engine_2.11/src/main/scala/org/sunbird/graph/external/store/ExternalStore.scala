package org.sunbird.graph.external.store

import java.sql.Timestamp
import java.util
import java.util.stream.Collectors
import java.util.{Date, Map}

import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.{Insert, QueryBuilder}
import org.sunbird.cassandra.{CassandraConnector, CassandraStore}
import org.sunbird.common.exception.{ErrorCodes, ServerException}
import org.sunbird.telemetry.logger.TelemetryManager

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

class ExternalStore(keySpace: String , table: String , primaryKey: java.util.List[String]) extends CassandraStore(keySpace, table, primaryKey) {

    def insert(request: util.Map[String, AnyRef])(implicit ec: ExecutionContext): Unit = {
        val insertQuery: Insert = QueryBuilder.insertInto(keySpace, table)
        val identifier = request.get("identifier");
        insertQuery.value("identifier", identifier)
        request.remove("identifier")
        request.remove("last_updated_on")
        insertQuery.value("last_updated_on", new Timestamp(new Date().getTime))
        for ((key, value) <- request.asScala) {
            insertQuery.value(key, "textAsBlob(" + value + ")")
        }
        try {
            val session: Session = CassandraConnector.getSession
            session.execute(insertQuery)
        } catch {
            case e: Exception =>
                e.printStackTrace()
                TelemetryManager.error("Exception Occurred While Saving The Record. | Exception is : " + e.getMessage, e)
                throw new ServerException(ErrorCodes.ERR_SYSTEM_EXCEPTION.name, "Exception Occurred While Saving The Record. Exception is : " + e.getMessage)
        }
    }
}