package com.liugh.util;

public class SFTPInfo implements java.io.Serializable {

    private static final long serialVersionUID = -2125997146531898166L;
    private String sftpServerIP = null;
    private String sftpServerUser = null;
    private String sftpServerPassword = null;
    private static final int DEFAULT_PORT = 22;
    private int port = DEFAULT_PORT;
    private int connectTimeout = 30000;
    private String privateKey = null;
    private String passphrase = null;
    
    public SFTPInfo(String ftpServerIP, String ftpServerUser,
            String ftpServerPassword) {
        this.sftpServerIP = ftpServerIP;
        this.sftpServerUser = ftpServerUser;
        this.sftpServerPassword = ftpServerPassword;
        this.port = DEFAULT_PORT;
    }

    public SFTPInfo(String ftpServerIP, String ftpServerUser,
            String ftpServerPassword, int port, int connectTimeout, String privateKey, String passphrase) {
        this.sftpServerIP = ftpServerIP;
        this.sftpServerUser = ftpServerUser;
        this.sftpServerPassword = ftpServerPassword;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    

    public boolean equals(Object obj) {
        if (!(obj instanceof SFTPInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SFTPInfo tmp_obj = (SFTPInfo) obj;
        boolean passwordFlag = false;
        if (this.sftpServerPassword == null || tmp_obj.sftpServerPassword == null) {
            if (this.sftpServerPassword == null && tmp_obj.sftpServerPassword == null)
                passwordFlag = true;
        } else {
            passwordFlag = this.sftpServerPassword
                    .equals(tmp_obj.sftpServerPassword);
        }
        if (this.sftpServerIP.equals(tmp_obj.sftpServerIP)
                && this.sftpServerUser.equals(tmp_obj.sftpServerUser)
                && this.port == tmp_obj.port && passwordFlag) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = 17;
        if (this.sftpServerIP != null) {
            result = 37 * result + this.sftpServerIP.hashCode();
        }
        if (this.sftpServerPassword != null) {
            result = 37 * result + this.sftpServerPassword.hashCode();
        }
        if (this.sftpServerUser != null) {
            result = 37 * result + this.sftpServerUser.hashCode();
        }
        result = 37 * result + this.port;
        return result;
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder();
        sb.append("ip=").append(sftpServerIP);
        sb.append(",port=").append(port);
        sb.append(",user=").append(sftpServerUser);
        sb.append(",password=").append(sftpServerPassword);
        sb.append(",privateKey=").append(privateKey);
        sb.append(",passphrase=").append(passphrase);
        return sb.toString();
    }

	public String getSftpServerIP() {
		return sftpServerIP;
	}

	public void setSftpServerIP(String sftpServerIP) {
		this.sftpServerIP = sftpServerIP;
	}

	public String getSftpServerUser() {
		return sftpServerUser;
	}

	public void setSftpServerUser(String sftpServerUser) {
		this.sftpServerUser = sftpServerUser;
	}

	public String getSftpServerPassword() {
		return sftpServerPassword;
	}

	public void setSftpServerPassword(String sftpServerPassword) {
		this.sftpServerPassword = sftpServerPassword;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
}
