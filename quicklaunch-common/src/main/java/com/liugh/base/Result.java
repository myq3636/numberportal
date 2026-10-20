package com.liugh.base;



public class Result{
    private int code = -1;
    private String text = "";

    
    public static final Result SUCCESS = new Result(200, "Success");
    public static final Result DEFAULT_ERROR = new Result(999, "其他错误");
    public static final Result UNAUTHORIZED  = new Result(304, "获取登录用户信息失败");
    public static final Result ERROR   = new Result(299, "操作失败");
    public static final Result USERNAME_IS_EXISTED   = new Result(301, "用户名已存在");
    public static final Result WAIT_APPLY   = new Result(305, "管理员未注册或者正在审批，请稍候重新注册或等待审批通过");
    public static final Result SHORT_CODE_ERROR   = new Result(326, "验证码错误或已失效");
    
    //routing error
    public static final Result ROUTING_IS_CHANGING   = new Result(501, "有其他的路由操作正在执行");
    public static final Result ROUTING_FILE_CREATE_ERROR   = new Result(502, "路由生成文件失败");
    public static final Result ROUTING_UPLOAD_ERROR   = new Result(503, "路由上传失败");
    public static final Result ROUTING_RELOAD_ERROR   = new Result(504, "路由执行失败");
    

    public static Result getStatus(int code) {
        switch (code) {
            case 200:
                return SUCCESS; 
            case 299:
                return ERROR;
            case 301:
                return USERNAME_IS_EXISTED;
            case 304:
            	return UNAUTHORIZED;
            case 305:
            	return WAIT_APPLY;
            case 326:
            	return SHORT_CODE_ERROR;
            default:
                return DEFAULT_ERROR; 
        }
    }
    
    public static Result getStatus(String text) {
        switch (text) {
            case "Success":
                return SUCCESS; 
            case "操作失败":
                return ERROR;
            case "获取登录用户信息失败":
            	return UNAUTHORIZED;
            case "管理员未注册或者正在审批，请稍候重新注册或等待审批通过":
                return USERNAME_IS_EXISTED;
            case "用户名已存在":
                return WAIT_APPLY;
            case "验证码错误或已失效":
            	return SHORT_CODE_ERROR;
            default:
                return DEFAULT_ERROR; 
        }
    }

    // Default constructor
    public Result() {
    }

    public Result(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public boolean isItFinal() {
        if ( (this.code % 10) == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public int getCode() {
        return code;
    }

    protected void setCode(int code) {
        this.code = code;
    }

    public String getText() {
        return this.text;
    }

    protected void setText(String text) {
        this.text = text;
    }
}
