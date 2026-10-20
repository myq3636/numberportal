package com.liugh.base;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */

public class RoutingInfoResponse {

	//0=db和系统同步，1=db和系统不同步
	private int sync;
	private List<RoutingInfoDTO> infos;
	public int getSync() {
		return sync;
	}
	public void setSync(int sync) {
		this.sync = sync;
	}
	public List<RoutingInfoDTO> getInfos() {
		return infos;
	}
	public void setInfos(List<RoutingInfoDTO> infos) {
		this.infos = infos;
	}
	
	
    
}
