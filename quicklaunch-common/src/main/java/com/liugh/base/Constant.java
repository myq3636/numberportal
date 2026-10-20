package com.liugh.base;

import java.util.HashSet;
import java.util.Set;

/**
 * @author liugh
 * @since 2018-05-03
 */
public class Constant {

	public static final String USER_KEY = "userId";
	public static final String COMMA_SEPARATOR = ",";
	
	/**
	 * 注册常量
	 */
	public static final int REGISTER_NO = 2;
	public static final int REGISTER_YES = 1;
	public static final int REGISTER_ALL = -1;
	
	/**
	 * GROUP 常量
	 */
	public static final int GROUP_ALL = -1;
	public static final int GROUP_NO = -2;
	
	public static final int BYTE_BUFFER = 1024;

    public static Set<String>  METHOD_URL_SET = new HashSet<>();

    /**
     * 用户注册默认角色
     */
    public static final int DEFAULT_REGISTER_ROLE = 5;

    public static final int BUFFER_MULTIPLE = 10;

    //验证码过期时间
    public static final Long PASS_TIME =  50000 * 60 *1000L;

    //根菜单节点
    public static final String ROOT_MENU = "0";

    //菜单类型，1：菜单  2：按钮操作
    public static final int TYPE_MENU = 1;

    //菜单类型，1：菜单  2：按钮操作
    public static final int TYPE_BUTTON = 2;

    public static boolean isPass=false;

    //启用
    public static final int ENABLE = 1;
    //禁用
    public static final int DISABLE = 0;
    
    //未发送
    public static final int NOT_SENT = 0;
    //已发送
    public static final int SENT = 1;
    //发送中
    public static final int SENDING = 2;
    //已完成
    public static final int FINISH = 3;
    
    public static final int ROUTING = 3;
    public static final int SENDERROUTING = 1;
    public static final int CONTENTKEYWORDROUTING = 2;
    
    public static final String MESSAGE_STATUS_SUCCESS = "10000";
    
    public static final String CUSTOMER_CONFIG_CACHE_PREFIX = "config_";
    
    public static final String FILE_SENDING_CACHE_PREFIX = "customer_sending_";
    public static final String CDR_STATISTICS_CACHE_PREFIX = "cdr_statistics_";
    public static final String CDR_STATISTICS_CACHE_PREFIX_3_HOUE = "cdr_statistics_3hour";
    public static final String CDR_STATISTICS_CACHE_PREFIX_1_DAY = "cdr_statistics_1day";
    
    
    public static final String ROUTING_IS_CHANGING = "routing_is_changing";
    
    public static class CDRStatus{
    	//0=已发送，1=失败，2=状态报告成功,3=状态报告失败
    	public static final int SENT = 0;
    	public static final int FAILED = 1;
    	public static final int SUCCESS = 2;
    	public static final int DELVERYFAILED = 3;
    }

    public static class FilePostFix{
        public static final String ZIP_FILE =".zip";

        public static final String [] IMAGES ={"jpg", "jpeg", "JPG", "JPEG", "gif", "GIF", "bmp", "BMP", "png"};
        public static final String [] ZIP ={"ZIP","zip","rar","RAR"};
        public static final String [] VIDEO ={"mp4","MP4","mpg","mpe","mpa","m15","m1v", "mp2","rmvb"};
        public static final String [] APK ={"apk","exe"};
        public static final String [] OFFICE ={"xls","xlsx","docx","doc","ppt","pptx"};

    }
    public class FileType{
        public static final int FILE_IMG = 1;
        public static final int FILE_ZIP = 2;
        public static final int FILE_VEDIO= 3;
        public static final int FILE_APK = 4;
        public static final int FIVE_OFFICE = 5;
        public static final String FILE_IMG_DIR= "/img/";
        public static final String FILE_ZIP_DIR= "/zip/";
        public static final String FILE_VEDIO_DIR= "/video/";
        public static final String FILE_APK_DIR= "/apk/";
        public static final String FIVE_OFFICE_DIR= "/office/";
    }

    public class RoleType{
        //超级管理员
        public static final String SYS_ASMIN_ROLE= "sysadmin";
        //管理员
        public static final int ADMIN= 1;
        //普通用户
        public static final int USER= 0;
    }


}
