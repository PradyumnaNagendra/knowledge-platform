package org.sunbird.content.mimetypes;

import org.apache.commons.lang3.BooleanUtils;
import org.sunbird.common.dto.Response;
import org.sunbird.common.exception.ErrorCodes;
import org.sunbird.common.exception.ResponseCode;
import org.sunbird.content.pipeline.InitializePipeline;
import org.sunbird.content.util.AsyncContentOperationUtil;
import org.sunbird.content.util.ContentParams;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.telemetry.logger.TelemetryManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * The Class HTMLMimeTypeMgrImpl is a implementation of IMimeTypeManager for
 * Mime-Type as <code>application/vnd.ekstep.html-archive</code> or for HTML
 * Content.
 * 
 * @author Azhar
 * 
 * @see IMimeTypeManager
 * @see ECMLMimeTypeMgrImpl
 * @see APKMimeTypeMgrImpl
 * @see CollectionMimeTypeMgrImpl
 * @see AssetsMimeTypeMgrImpl
 */
public class HTMLMimeTypeMgrImpl extends BaseMimeTypeManager implements IMimeTypeManager {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ekstep.taxonomy.mgr.IMimeTypeManager#upload(org.ekstep.graph.dac.model.
	 * Node, java.io.File, java.lang.String)
	 */
	@Override
	public Response upload(String contentId, Node node, File uploadFile, boolean isAsync) {
		TelemetryManager.log("Calling Upload Content For Node ID: " + node.getIdentifier() + " Uploaded File :" + uploadFile);
		if (hasGivenFile(uploadFile, "index.html")) {
			return uploadContentArtifact(contentId, node, uploadFile, false);
		} else {
			return ERROR(ErrorCodes.ERR_CONTENT_UPLOAD_FILE.name(), "Zip file doesn't have required files.", ResponseCode.CLIENT_ERROR);
		}
	}

	public Response upload(String contentId, Node node, String fileUrl) {
		File file = null;
		try {
			file = copyURLToFile(fileUrl);
			return upload(contentId, node, file, false);
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != file && file.exists()) file.delete();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ekstep.taxonomy.mgr.IMimeTypeManager#publish(org.ekstep.graph.dac.model
	 * .Node)
	 */
	@Override
	public Response publish(String contentId, Node node, boolean isAsync) {

		Response response = new Response();
		TelemetryManager.log("Preparing the Parameter Map for Initializing the Pipeline For Node ID: " + contentId);
		InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId), contentId);
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put(ContentParams.node.name(), node);
		parameterMap.put(ContentParams.ecmlType.name(), false);

		TelemetryManager.log("Adding 'isPublishOperation' Flag to 'true'");
		parameterMap.put(ContentParams.isPublishOperation.name(), true);

		
		TelemetryManager.log("Calling the 'Review' Initializer for Node Id: " + contentId);
		response = pipeline.init(ContentParams.review.name(), parameterMap);
		TelemetryManager.log("Review Operation Finished Successfully for Node ID: " + contentId);

		if (BooleanUtils.isTrue(isAsync)) {
			AsyncContentOperationUtil.makeAsyncOperation(ContentParams.PUBLISH.name(), contentId, parameterMap);
			TelemetryManager.log("Publish Operation Started Successfully in 'Async Mode' for Node Id: " + contentId);

			response.put(ContentParams.publishStatus.name(),
					"Publish Operation for Content Id '" + contentId + "' Started Successfully!");
		} else {
			TelemetryManager.log("Publish Operation Started Successfully in 'Sync Mode' for Node Id: " + contentId);
			response = pipeline.init(ContentParams.publish.name(), parameterMap);
		}

		return response;
	}
	
	@Override
	public Response review(String contentId, Node node, boolean isAsync) {

		TelemetryManager.log("Preparing the Parameter Map for Initializing the Pipeline For Node ID: " + node.getIdentifier());
		InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId), contentId);
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put(ContentParams.node.name(), node);
		parameterMap.put(ContentParams.ecmlType.name(), false);

		TelemetryManager.log("Calling the 'Review' Initializer for Node ID: " + node.getIdentifier());
		return pipeline.init(ContentParams.review.name(), parameterMap);
	}

}
