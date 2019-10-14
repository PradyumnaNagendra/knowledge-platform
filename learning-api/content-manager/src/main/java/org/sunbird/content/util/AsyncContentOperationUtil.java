package org.sunbird.content.util;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.common.exception.ClientException;
import org.ekstep.content.common.ContentErrorMessageConstants;
import org.ekstep.content.common.ContentOperations;
import org.ekstep.content.enums.ContentErrorCodeConstants;
import org.ekstep.content.enums.ContentWorkflowPipelineParams;
import org.ekstep.content.pipeline.initializer.InitializePipeline;
import org.ekstep.graph.dac.model.Node;
import org.ekstep.telemetry.logger.TelemetryManager;
import org.sunbird.common.exception.ClientException;
import org.sunbird.content.pipeline.InitializePipeline;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.telemetry.logger.TelemetryManager;

import java.io.File;
import java.util.Map;

public class AsyncContentOperationUtil {

	private static final String tempFileLocation = "/data/contentBundle/";

	/** The logger. */
	

	public static void makeAsyncOperation(String operation, String contentId, Map<String, Object> parameterMap) {

		if (null == operation)
			throw new ClientException(ContentErrorCodeConstants.INVALID_OPERATION.name(),
					ContentErrorMessageConstants.INVALID_OPERATION + " | [Invalid or 'null' operation.]");

		if (null == parameterMap)
			throw new ClientException(ContentErrorCodeConstants.INVALID_PARAMETER.name(),
					ContentErrorMessageConstants.INVALID_ASYNC_OPERATION_PARAMETER_MAP
							+ " | [Invalid or 'null' Parameter Map.]");

		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					switch (operation) {
					case "upload":
					case "UPLOAD": {
						Node node = (Node) parameterMap.get(ContentWorkflowPipelineParams.node.name());
						if (null == node)
							throw new ClientException(ContentErrorCodeConstants.INVALID_CONTENT.name(),
									ContentErrorMessageConstants.INVALID_CONTENT
											+ " | ['null' or Invalid Content Node (Object). Async Upload Operation Failed.]");
						try {
							InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId),
									contentId);
							pipeline.init(ContentParams.upload.name(), parameterMap);
						} catch (Exception e) {
							TelemetryManager.error("Something Went Wrong While Performing 'Content Upload' Operation in Async Mode. | [Content Id: "
											+ node.getIdentifier() + "]", e);
							node.getMetadata().put(ContentParams.uploadError.name(), e.getMessage());
							node.getMetadata().put(ContentParams.status.name(),
									ContentWorkflowPipelineParams.Failed.name());
							UpdateDataNodeUtil updateDataNodeUtil = new UpdateDataNodeUtil();
							updateDataNodeUtil.updateDataNode(node);
						}
					}
						break;

					case "publish":
					case "PUBLISH": {
						Node node = (Node) parameterMap.get(ContentParams.node.name());
						if (null == node)
							throw new ClientException(ContentErrorCodeConstants.INVALID_CONTENT.name(),
									ContentErrorMessageConstants.INVALID_CONTENT
											+ " | ['null' or Invalid Content Node (Object). Async Publish Operation Failed.]");
						try {
							InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId),
									contentId);
							pipeline.init(ContentParams.publish.name(), parameterMap);
						} catch (Exception e) {
							TelemetryManager.error(
									"Something Went Wrong While Performing 'Content Publish' Operation in Async Mode. | [Content Id: "
											+ contentId + "]", e);
							node.getMetadata().put(ContentParams.publishError.name(), e.getMessage());
							node.getMetadata().put(ContentParams.status.name(),
									ContentParams.Failed.name());
							UpdateDataNodeUtil updateDataNodeUtil = new UpdateDataNodeUtil();
							updateDataNodeUtil.updateDataNode(node);
						}
					}
						break;

					case "bundle":
					case "BUNDLE": {
						try {
							InitializePipeline pipeline = new InitializePipeline(tempFileLocation, "node");
							pipeline.init(ContentParams.bundle.name(), parameterMap);
						} catch (Exception e) {
							TelemetryManager.error(
									"Something Went Wrong While Performing 'Content Bundle' Operation in Async Mode.", e);
						}
					}
						break;

					case "review":
					case "REVIEW": {
						Node node = (Node) parameterMap.get(ContentParams.node.name());
						if (null == node)
							throw new ClientException(ContentErrorCodeConstants.INVALID_CONTENT.name(),
									ContentErrorMessageConstants.INVALID_CONTENT
											+ " | ['null' or Invalid Content Node (Object). Async Review Operation Failed.]");
						try {
							InitializePipeline pipeline = new InitializePipeline(getBasePath(contentId),
									contentId);
							pipeline.init(ContentParams.review.name(), parameterMap);
						} catch (Exception e) {
							TelemetryManager.error(
									"Something Went Wrong While Performing 'Content Review (Send For Review)' Operation in Async Mode. | [Content Id: "
											+ node.getIdentifier() + "]", e);
							node.getMetadata().put(ContentParams.reviewError.name(), e.getMessage());
							node.getMetadata().put(ContentParams.status.name(),
									ContentParams.Failed.name());
							UpdateDataNodeUtil updateDataNodeUtil = new UpdateDataNodeUtil();
							updateDataNodeUtil.updateDataNode(node);
						}
					}
						break;

					default:
						TelemetryManager.log("Invalid Async Operation.");
						break;
					}
				} catch (Exception e) {
					TelemetryManager.error("Error! While Making Async Call for Content Operation: " + operation.name(), e);
				}
			}
		};
		new Thread(task, "AsyncContentOperationThread").start();
	}

	private static String getBasePath(String contentId) {
		String path = "";
		if (!StringUtils.isBlank(contentId))
			path = tempFileLocation + File.separator + System.currentTimeMillis()
					+ "_temp" + File.separator + contentId;
		return path;
	}
}
