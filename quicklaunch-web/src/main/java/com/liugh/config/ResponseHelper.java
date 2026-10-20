package com.liugh.config;

import org.springframework.http.HttpStatus;

import com.liugh.base.Result;

/**
 * 统一返回相应参数
 * @author liugh 53182347@qq.com
 */
@SuppressWarnings("rawtypes")
public class ResponseHelper {

    public ResponseHelper() {
    }
    
	public static <T> ResponseModel<T> successSimpleResponse() {
		ResponseModel response = new ResponseModel();
        response.setCode(Result.SUCCESS.getCode());
        response.setMessage(Result.SUCCESS.getText());
        return response;
    }
    
    public static <T> ResponseModel<T> successSimpleResponse(String message) {
        ResponseModel response = new ResponseModel();
        response.setCode(Result.SUCCESS.getCode());
        response.setResult("SUCCESS");
        response.setMessage(message);
        return response;
    }
    
    public static <T> ResponseModel<T> errorSimpleResponse(String message) {
        ResponseModel response = new ResponseModel();
        response.setCode(Result.getStatus(message).getCode());
        response.setResult("FAIL");
        response.setMessage(message);
        return response;
    }
    
    public static <T> ResponseModel<T> validationFailure(String error){
    	ResponseModel response = new ResponseModel();
        response.setCode(Result.ERROR.getCode());
        response.setMessage(error);
        return response;
    }
    public static <T> ResponseModel<T> errorResponse(Result result) {
        ResponseModel response = new ResponseModel();
        response.setCode(result.getCode());
        response.setMessage(result.getText());
        return response;
    }

    public static <T> ResponseModel<T> buildSuccessfulResponseModel(T result) {
        ResponseModel response = new ResponseModel();
        response.setCode(Result.SUCCESS.getCode());
        response.setMessage(Result.SUCCESS.getText());
        response.setData(result);
        return response;
    }
}
