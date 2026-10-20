package com.liugh.mapper;


import org.apache.ibatis.annotations.Param;

import com.liugh.entity.RCSCdrInfo;
import com.baomidou.mybatisplus.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
public interface RCSCdrInfoMapper extends BaseMapper<RCSCdrInfo> {

    int existTable(String tableName);

    int dropTable(@Param("tableName") String tableName);

    int createNewTable(@Param("tableName") String tableName);

    int insertCdr(@Param("tableName") String tableName, @Param("cdr") RCSCdrInfo cdr);

	/*
	 * @Select("SELECT * FROM rcs_cdr_info WHERE channel_code='AVANT' " +
	 * "AND rssid=#{rssid} AND msisdn=#{recipient} AND " +
	 * "((#{messageKey} IS NOT NULL AND msg_id=#{messageKey}) OR " +
	 * "(#{channelMsgId} IS NOT NULL AND channel_msg_id=#{channelMsgId}))")
	 * List<RCSCdrInfo> selectAvantCdrs(@Param("rssid") int rssid,
	 * 
	 * @Param("messageKey") String messageKey,
	 * 
	 * @Param("channelMsgId") String channelMsgId,
	 * 
	 * @Param("recipient") String recipient);
	 * 
	 * @Update("UPDATE rcs_cdr_info SET " +
	 * "channel_msg_id=COALESCE(channel_msg_id,#{channelMsgId}), " + "status=CASE "
	 * + "WHEN #{status}=9 AND status IN (-1,0,3,8) THEN 9 " +
	 * "WHEN #{status}=3 AND status IN (-1,0,8) THEN 3 " +
	 * "WHEN #{status}=4 AND status IN (-1,0,8) THEN 4 " +
	 * "WHEN #{status}=5 AND status IN (-1,0,8) THEN 5 " + "ELSE status END, " +
	 * "delivered_time=CASE WHEN #{status}=3 AND delivered_time IS NULL " +
	 * "THEN #{occurredAt} ELSE delivered_time END, " +
	 * "read_time=CASE WHEN #{status}=9 AND read_time IS NULL " +
	 * "THEN #{occurredAt} ELSE read_time END, " +
	 * "error_code=CASE WHEN #{status} IN (4,5) AND status IN (-1,0,8) " +
	 * "THEN #{errorCode} ELSE error_code END, " +
	 * "error_reason=CASE WHEN #{status} IN (4,5) AND status IN (-1,0,8) " +
	 * "THEN #{errorReason} ELSE error_reason END " +
	 * "WHERE id=#{cdrId} AND channel_code='AVANT' AND rssid=#{rssid} " +
	 * "AND (channel_msg_id IS NULL OR channel_msg_id=#{channelMsgId})") int
	 * advanceAvantStatus(@Param("cdrId") int cdrId,
	 * 
	 * @Param("rssid") int rssid,
	 * 
	 * @Param("channelMsgId") String channelMsgId,
	 * 
	 * @Param("status") int status,
	 * 
	 * @Param("occurredAt") long occurredAt,
	 * 
	 * @Param("errorCode") String errorCode,
	 * 
	 * @Param("errorReason") String errorReason);
	 * 
	 * @Update("UPDATE rcs_cdr_info SET channel_msg_id=#{channelMsgId}, " +
	 * "status=CASE WHEN status IN (-1,0,8) THEN 0 ELSE status END, " +
	 * "error_code=CASE WHEN status IN (-1,0,8) THEN NULL ELSE error_code END, " +
	 * "error_reason=CASE WHEN status IN (-1,0,8) THEN NULL ELSE error_reason END "
	 * + "WHERE id=#{cdrId}") int markSubmittedPreservingTerminal(@Param("cdrId")
	 * Integer cdrId,
	 * 
	 * @Param("channelMsgId") String channelMsgId);
	 */

}
