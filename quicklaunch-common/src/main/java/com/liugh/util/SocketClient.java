package com.liugh.util;

import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

	public static void reloadRouting(String ip, int port) throws Exception{
		Socket socket=null;
        PrintWriter out=null;
        try{
            socket=new Socket(ip,port);
            out=new PrintWriter(socket.getOutputStream(),true);
            out.println("routingInfo -a");
        }
        catch(Exception e){
            e.printStackTrace();
        }finally{
        	if (out!=null) {
				out.close();
			}
        	if (socket !=null) {
				socket.close();
			}
        }
	}
}
