package org.sunbird.common.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This contains data (value objects) to be passed to middleware command
 * 
 * @author rayulu
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request implements Serializable {

    private static final long serialVersionUID = -2362783406031347676L;

    protected Map<String, Object> context;
    private String id;
    private String ver;
    private String ts;
    private RequestParams params;

	private Map<String, Object> request = new HashMap<String, Object>();

    private String managerName;
    private String operation;
    private String request_id;

    public Request() {
    	this.params = new RequestParams();
    	this.params.setMsgid(request_id);
   
    }
    
    public Request(Request request) {
    	this.params = request.getParams();
    	if (null == this.params)
    	    this.params = new RequestParams();
    	else if (StringUtils.isNotBlank(this.params.getMsgid())) {
    	    this.request_id = this.params.getMsgid();
    	}
    	if(StringUtils.isBlank(this.params.getMsgid()) && StringUtils.isNotBlank(request_id))
    		this.params.setMsgid(request_id);
        this.context.putAll(request.getContext());
    }
    
    public String getRequestId() {
        if (null != this.params)
            return this.params.getMsgid();
        return request_id;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * @return the requestValueObjects
     */
    public Map<String, Object> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request;
    }

    public Object get(String key) {
        return request.get(key);
    }
    
	public void setRequest_id(String request_id) {
		this.request_id = request_id;
	}

	public void put(String key, Object vo) {
        request.put(key, vo);
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void copyRequestValueObjects(Map<String, Object> map) {
        if (null != map && map.size() > 0) {
            this.request.putAll(map);
        }
    }

    @Override
    public String toString() {
        return "Request [" + (context != null ? "context=" + context + ", " : "")
                + (request != null ? "requestValueObjects=" + request : "") + "]";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public RequestParams getParams() {
        return params;
    }

    public void setParams(RequestParams params) {
        this.params = params;
    	if(this.params.getMsgid()==null&&request_id!=null)
    		this.params.setMsgid(request_id);

    }

}
