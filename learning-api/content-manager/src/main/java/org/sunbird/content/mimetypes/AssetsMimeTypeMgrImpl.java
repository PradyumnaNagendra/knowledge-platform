package org.sunbird.content.mimetypes;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.ekstep.common.Platform;
import org.ekstep.common.dto.Response;
import org.ekstep.common.enums.TaxonomyErrorCodes;
import org.ekstep.common.exception.ClientException;
import org.ekstep.common.exception.ServerException;
import org.ekstep.content.common.ContentOperations;
import org.ekstep.content.pipeline.initializer.InitializePipeline;
import org.ekstep.content.util.AsyncContentOperationUtil;
import org.ekstep.graph.dac.model.Node;
import org.ekstep.kafka.KafkaClient;
import org.ekstep.learning.common.enums.ContentAPIParams;
import org.ekstep.learning.common.enums.ContentErrorCodes;
import org.ekstep.telemetry.logger.TelemetryManager;
import org.ekstep.telemetry.util.LogTelemetryEventUtil;
import org.sunbird.common.Platform;
import org.sunbird.common.dto.Response;
import org.sunbird.common.exception.ClientException;
import org.sunbird.common.exception.ErrorCodes;
import org.sunbird.common.exception.ServerException;
import org.sunbird.content.pipeline.InitializePipeline;
import org.sunbird.content.util.AsyncContentOperationUtil;
import org.sunbird.content.util.ContentParams;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.telemetry.logger.TelemetryManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class AssetsMimeTypeMgrImpl is a implementation of IMimeTypeManager for
 * Mime-Type as <code>assets</code> or for Asset type Content.
 *
 * @author Azhar
 *
 * @see IMimeTypeManager
 * @see HTMLMimeTypeMgrImpl
 * @see APKMimeTypeMgrImpl
 * @see ECMLMimeTypeMgrImpl
 * @see CollectionMimeTypeMgrImpl
 */
public class AssetsMimeTypeMgrImpl extends BaseMimeTypeManager implements IMimeTypeManager {

	private static String actorId = "Asset Enrichment Samza Job";
	private static String actorType = "System";
	private static String pdataId = "org.ekstep.platform";
	private static String pdataVersion = "1.0";
	private static String action = "assetenrichment";
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.ekstep.taxonomy.mgr.IMimeTypeManager#upload(org.ekstep.graph.dac.model.
	 * Node, java.io.File, java.lang.String)
	 */
	@Override
	public Response upload(String contentId, Node node, File uploadFile, boolean isAsync) {
		TelemetryManager.log("Uploaded File: " + uploadFile.getName());

		Response response = new Response();
		try {
			TelemetryManager.log("Verifying the MimeTypes.");
			Tika tika = new Tika(new MimeTypes());
			String mimeType = tika.detect(uploadFile);
			String nodeMimeType = (String) node.getMetadata().get(ContentParams.mimeType.name());
			TelemetryManager.log("Uploaded Asset MimeType: "+ mimeType);
			if (!StringUtils.equalsIgnoreCase(mimeType, nodeMimeType))
				TelemetryManager.log("Uploaded File MimeType is not same as Node (Object) MimeType. [Uploaded MimeType: "
						+ mimeType + " | Node (Object) MimeType: " + nodeMimeType + "]");

			TelemetryManager.log("Calling Upload Content Node For Node ID: " + node.getIdentifier());
			String[] urlArray = uploadArtifactToAWS(uploadFile, node.getIdentifier());

			TelemetryManager.log("Updating the Content Node for Node ID: "+ node.getIdentifier());
			node.getMetadata().put(ContentParams.s3Key.name(), urlArray[0]);
			node.getMetadata().put(ContentParams.artifactUrl.name(), urlArray[1]);
			node.getMetadata().put(ContentParams.downloadUrl.name(), urlArray[1]);
			node.getMetadata().put(ContentParams.size.name(), getCloudStoredFileSize(urlArray[0]));
			if (StringUtils.equalsIgnoreCase(node.getMetadata().get("mediaType").toString(), "image") ||
					StringUtils.equalsIgnoreCase(node.getMetadata().get("mediaType").toString(), "video")) {
				node.getMetadata().put(ContentParams.status.name(), "Processing");
				response = updateContentNode(contentId, node, urlArray[1]);
				if (!checkError(response)) {
					pushInstructionEvent(node, contentId);
				}else {
					throw new ServerException(ContentErrorCodes.ERR_CONTENT_UPLOAD_FILE.name(),
							"Error occured during content Upload");
				}
			} else {
				node.getMetadata().put(ContentParams.status.name(), "Live");
				response = updateContentNode(contentId, node, urlArray[1]);
			}

		} catch (IOException e) {
			throw new ServerException(ContentParams.FILE_ERROR.name(),
					"Error! While Reading the MimeType of Uploaded File. | [Node Id: " + node.getIdentifier() + "]");
		} catch (ClientException e) {
			throw e;
		} catch (ServerException e) {
			throw e;
		} catch (Exception e) {
			throw new ServerException(ContentParams.SERVER_ERROR.name(),
					"Error! Something went Wrong While Uploading an Asset. | [Node Id: " + node.getIdentifier() + "]");
		}

		return response;
	}

	@Override
	public Response upload(String contentId, Node node, String fileUrl) {
		File file = null;
		try {
			file = copyURLToFile(fileUrl);
			node.getMetadata().put(ContentParams.artifactUrl.name(), fileUrl);
			node.getMetadata().put(ContentParams.downloadUrl.name(), fileUrl);
			node.getMetadata().put(ContentParams.size.name(), getFileSize(file));
			Response response;
			if (StringUtils.equalsIgnoreCase(node.getMetadata().get("mediaType").toString(), "image") ||
					StringUtils.equalsIgnoreCase(node.getMetadata().get("mediaType").toString(), "video")) {
				node.getMetadata().put(ContentParams.status.name(), "Processing");
				response = updateContentNode(node.getIdentifier(), node, fileUrl);
				if (!checkError(response)) {
					pushInstructionEvent(node, contentId);
				}else {
					throw new ServerException(ErrorCodes.ERR_CONTENT_UPLOAD_FILE.name(),
							"Error occured during content Upload");
				}
			} else {
				node.getMetadata().put(ContentParams.status.name(), "Live");
				response = updateContentNode(contentId, node, fileUrl);
			}
			if (null != file && file.exists()) file.delete();
			return response;
		} catch (IOException e) {
			throw new ClientException(ErrorCodes.ERR_INVALID_UPLOAD_FILE_URL.name(), "fileUrl is invalid.");
		} catch (ClientException e) {
			throw e;
		} catch (ServerException e) {
			throw e;
		} catch (Exception e) {
			throw new ServerException(ContentParams.SERVER_ERROR.name(),
					"Error! Something went Wrong While Uploading an Asset. | [Node Id: " + node.getIdentifier() + "]");
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

		TelemetryManager.log("Calling the 'Review' Initializer for Node Id: "+ contentId);
		response = pipeline.init(ContentParams.review.name(), parameterMap);
		TelemetryManager.log("Review Operation Finished Successfully for Node ID: "+ contentId);

		if (BooleanUtils.isTrue(isAsync)) {
			AsyncContentOperationUtil.makeAsyncOperation(ContentParams.PUBLISH.name(), contentId, parameterMap);
			TelemetryManager.log("Publish Operation Started Successfully in 'Async Mode' for Node Id: "+ contentId);

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
		TelemetryManager.log("Preparing the Parameter Map for Initializing the Pipeline For Node ID: " + contentId);
		InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId), contentId);
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put(ContentParams.node.name(), node);
		parameterMap.put(ContentParams.ecmlType.name(), false);

		TelemetryManager.log("Calling the 'Review' Initializer for Node ID: " + contentId);
		return pipeline.init(ContentParams.review.name(), parameterMap);
	}
	
	private void pushInstructionEvent(Node node, String contentId) throws Exception{
		Map<String,Object> actor = new HashMap<String,Object>();
		Map<String,Object> context = new HashMap<String,Object>();
		Map<String,Object> object = new HashMap<String,Object>();
		Map<String,Object> edata = new HashMap<String,Object>();
		
		generateInstructionEventMetadata(actor, context, object, edata, node.getMetadata(), contentId);
		String beJobRequestEvent = LogTelemetryEventUtil.logInstructionEvent(actor, context, object, edata);
		String topic = Platform.config.getString("kafka.topics.instruction");
		if(StringUtils.isBlank(beJobRequestEvent)) {
			throw new ClientException("BE_JOB_REQUEST_EXCEPTION", "Event is not generated properly.");
		}
		if(StringUtils.isNotBlank(topic)) {
			KafkaClient.send(beJobRequestEvent, topic);
		} else {
			throw new ClientException("BE_JOB_REQUEST_EXCEPTION", "Invalid topic id.");
		}
	}
	
	private void generateInstructionEventMetadata(Map<String,Object> actor, Map<String,Object> context, 
			Map<String,Object> object, Map<String,Object> edata, Map<String, Object> metadata, String contentId) {
		
		actor.put("id", actorId);
		actor.put("type", actorType);
		
		context.put("channel", metadata.get("channel")); 
		Map<String, Object> pdata = new HashMap<>();
		pdata.put("id", pdataId); 
		pdata.put("ver", pdataVersion);
		context.put("pdata", pdata);
		if (Platform.config.hasPath("cloud_storage.env")) {
			String env = Platform.config.getString("cloud_storage.env");
			context.put("env", env);
		}
		
		object.put("id", contentId);
		object.put("ver", metadata.get("versionKey"));
		
		edata.put("action", action);
		edata.put("status", metadata.get("status"));
		edata.put("mediaType", metadata.get("mediaType"));
		edata.put("contentType", metadata.get("contentType"));
	}

}
