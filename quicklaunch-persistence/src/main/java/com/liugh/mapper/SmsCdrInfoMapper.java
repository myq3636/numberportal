package com.liugh.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.liugh.entity.SmsCdrInfo;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
public interface SmsCdrInfoMapper extends BaseMapper<SmsCdrInfo> {
	public Integer queryMaxLine(@Param("filename") String filename);
	
	public List<Map<String, Object>> groupByCdrStatus(@Param("filename") String filename);
	
	List<SmsCdrInfo> selectPageByConditionCdr(Page<SmsCdrInfo> page,
            @Param("number") String number,              
            @Param("effectiveStartTime")String effectiveStartTime,
            @Param("effectiveEndTime")String effectiveEndTime,
            @Param("ssid") int ssid);
	
    int existTable(String tableName);
    
    int dropTable(@Param("tableName")String tableName);
    
    int createNewTable(@Param("tableName")String tableName);
    
    int insertCdr(@Param("tableName")String tableName,@Param("cdr") SmsCdrInfo cdr);
		
}
