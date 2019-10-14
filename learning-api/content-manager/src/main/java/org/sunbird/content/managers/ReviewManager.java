package org.sunbird.content.managers;

import akka.dispatch.Futures;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.DateUtils;
import org.sunbird.common.Platform;
import org.sunbird.common.dto.Request;
import org.sunbird.common.dto.Response;
import org.sunbird.common.exception.ClientException;
import org.sunbird.common.exception.ErrorCodes;
import org.sunbird.content.mimetypes.IMimeTypeManager;
import org.sunbird.content.mimetypes.MimeTypeManagerFactory;
import org.sunbird.content.util.YouTubeUrlUtil;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.graph.schema.DefinitionFactory;
import org.sunbird.graph.schema.DefinitionNode;
import org.sunbird.telemetry.logger.TelemetryManager;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public class ReviewManager {

    public static final String graphId = "domain";

    public static Future<Response> review(Request reviewRequest, ExecutionContextExecutor dispatcher) {
        DefinitionNode definition = DefinitionFactory.getDefinition(graphId, reviewRequest.getObjectType(), "1.0");
        String identifier = (String) reviewRequest.getContext().get("identifier");
        Node node = definition.getNode(identifier, "review", "edit");

        String body = "body";//getContentBody(node.getIdentifier());
        node.getMetadata().put("body", body);

        node.getMetadata().put("lastSubmittedOn", DateUtils.formatCurrentDate());

        String mimeType = StringUtils.isBlank((String) node.getMetadata().get("mimeType")) ?"assets": (String) node.getMetadata().get("mimeType");
        String artifactUrl = (String) node.getMetadata().get("artifactUrl");
        String license = (String) node.getMetadata().get("license");
        if (StringUtils.equals("video/x-youtube", mimeType) && StringUtils.isNotBlank(artifactUrl) && StringUtils.isBlank(license))
            checkYoutubeLicense(artifactUrl, node);
        TelemetryManager.log("Getting Mime-Type Manager Factory. | [Content ID: " + identifier + "]");

        String contentType = (String) node.getMetadata().get("contentType");
        IMimeTypeManager mimeTypeManager = MimeTypeManagerFactory.getManager(contentType, mimeType);

        Response response = mimeTypeManager.review(identifier, node, false);

        TelemetryManager.log("Returning 'Response' Object: ", response.getResult());
        return Futures.successful(response);

    }

    protected static void checkYoutubeLicense(String artifactUrl, Node node) {
        Boolean isValReq = Platform.config.hasPath("learning.content.youtube.validate.license")
                ? Platform.config.getBoolean("learning.content.youtube.validate.license") : false;

        if (isValReq) {
            String licenseType = YouTubeUrlUtil.getLicense(artifactUrl);
            if (equalsIgnoreCase("youtube", licenseType))
                node.getMetadata().put("license", "Standard YouTube License");
            else if (equalsIgnoreCase("creativeCommon", licenseType))
                node.getMetadata().put("license", "CC-BY 4.0");
            else {
                TelemetryManager.log("Got Unsupported Youtube License Type : " + licenseType + " | [Content ID: "
                        + node.getIdentifier() + "]");
                throw new ClientException(ErrorCodes.ERR_YOUTUBE_LICENSE_VALIDATION.name(),
                        "Unsupported Youtube License!");
            }
        }
    }
}
