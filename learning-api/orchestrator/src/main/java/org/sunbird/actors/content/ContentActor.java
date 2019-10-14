package org.sunbird.actors.content;

import akka.pattern.Patterns;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.Platform;
import org.sunbird.common.dto.Request;
import org.sunbird.common.dto.Response;
import org.sunbird.graph.engine.NodeManager;
import org.sunbird.graph.mgr.BaseGraphManager;
import org.sunbird.content.managers.ReviewManager;
import scala.concurrent.Future;


@ActorConfig(tasks = {"createContent"})
public class ContentActor extends BaseGraphManager {

    public static String objectType = "Content";
    public static String version = "1.0";

    private static final String CONTENT_KEYSPACE_NAME = Platform.config.hasPath("content.keyspace.name") ? Platform.config.getString("content.keyspace.name") : "content_store";
    private static final String CONTENT_TABLE_NAME = Platform.config.hasPath("content.keyspace.table") ? Platform.config.getString("content.keyspace.table") : "content_data";

    @Override
    public void onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        switch (operation) {
            case "createContent":
                create(request);
                break;

            case "reviewContent":
                review(request);
                break;
            default:
                ERROR(operation);
                break;
        }

    }

    private void create(Request request) throws Exception {
        Request createRequest = new Request(request, objectType);
        createRequest.setRequest(request.getRequest());
        createRequest.getContext().put("keyspace",CONTENT_KEYSPACE_NAME);
        createRequest.getContext().put("table",CONTENT_TABLE_NAME);
        Future<Response> result = NodeManager.createDataNode(createRequest, getContext().dispatcher());
        Patterns.pipe(result, getContext().getDispatcher()).to(sender());
    }

    private void update(Request request) {
        Request createRequest = new Request(request, objectType);
//        Patterns.pipe(getResult(createRequest), getContext().getDispatcher()).to(sender());
    }

    private void review(Request request) {
        Request reviewRequest = new Request(request, objectType);
        reviewRequest.setRequest(request.getRequest());
        reviewRequest.getContext().put("keyspace",CONTENT_KEYSPACE_NAME);
        reviewRequest.getContext().put("table",CONTENT_TABLE_NAME);
        Future<Response> result = ReviewManager.review(reviewRequest, getContext().dispatcher());
        Patterns.pipe(result, getContext().getDispatcher()).to(sender());
    }

}
