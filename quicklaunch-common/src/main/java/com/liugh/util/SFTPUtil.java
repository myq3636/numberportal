package com.liugh.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SFTPUtil.class);

    //private static ChannelSftp sftp = null;
    /**
     * connect server via sftp
     * 
     * @throws SFTPException
     */
    public static ChannelSftp connect(SFTPInfo sftpInfo) throws SFTPException {
        ChannelSftp sftp = null;
        try {
            String sftpServer = sftpInfo.getSftpServerIP();
            String sftpUser = sftpInfo.getSftpServerUser();
            String sftpPassword = sftpInfo.getSftpServerPassword();
            int sftpPort = sftpInfo.getPort();
            int timeout = sftpInfo.getConnectTimeout();
            String privateKey = sftpInfo.getPrivateKey();
            String passphrase = sftpInfo.getPassphrase();
            JSch jsch = new JSch();
            if (StringUtils.isNotBlank(privateKey)) {
                if (StringUtils.isNotBlank(passphrase)) {
                    jsch.addIdentity(privateKey, passphrase);
                } else {
                    jsch.addIdentity(privateKey);
                }
                LOG.info("SFTP use key authentication");
            } else {
                LOG.info("SFTP use password authentication");
            }
            Session sshSession = jsch.getSession(sftpUser, sftpServer, sftpPort);
            if (StringUtils.isNotBlank(sftpPassword)) {
                sshSession.setPassword(sftpPassword);
            }
            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshConfig.put("PreferredAuthentications","publickey,keyboard-interactive,password");
            sshSession.setConfig(sshConfig);
            sshSession.setTimeout(timeout);
            sshSession.connect();
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            sftp = (ChannelSftp) channel;
            LOG.info("Connected to " + sftpServer + ".");
        } catch (JSchException e) {
            LOG.error(e.toString());
            throw new SFTPException(e.toString());
        }
        return sftp;
    }

    /**
     * Disconnect with server
     * 
     * @throws SFTPException
     */
    public static void disconnect(ChannelSftp sftp) throws SFTPException {
        try {
            if (sftp != null) {
                if (sftp.isConnected()) {
                    sftp.disconnect();
                    sftp.getSession().disconnect();
                }
            }
        } catch (JSchException e) {
            LOG.error("Close sftp error", e);
            throw new SFTPException(e.toString());
        }
    }

    /**
     * upload the files to the server
     * 
     * @throws SFTPException
     */
    public static void upload(String sourceFile, String descFile, ChannelSftp sftp) throws SFTPException {
        try {
            /*String localFile = sourceFile;
            File file = new File(localFile);
            if (file.isFile()) {
                String remoteFile = descFile;
                File rfile = new File(remoteFile);
                String rpath = rfile.getParent();
                try {
                    createDir(rpath, sftp);
                } catch (Exception e) {
                    LOG.error("Create remote path failed {}", rpath);
                    throw new SFTPException(e.toString());
                }
                FileInputStream is = new FileInputStream(file);
                sftp.mkdir("/usr/local/a2p/conf/routing/routing");
                sftp.put(is, rfile.getName());
                is.close();
                LOG.info("sftp sever:{}:{}, path:{}, upload successfully.", sftp.getSession().getHost(), sftp.getSession().getPort(), descFile);
            }*/
            sftp.put(sourceFile, descFile);
            LOG.info("sftp sever:{}:{}, path:{}, upload successfully.", sftp.getSession().getHost(), sftp.getSession().getPort(), descFile);
        } catch (SftpException e) {
            LOG.error("Upload SftpException", e.toString());
            throw new SFTPException(e.toString());
        } catch (JSchException e) {
            LOG.error("Upload JSchException", e.toString());
            throw new SFTPException(e.toString());
        }

    }

    /**
     * create Directory
     * 
     * @param filepath
     */
    private static void createDir(String filepath, ChannelSftp sftp) {
        if (filepath.startsWith("\\")) {
            filepath = filepath.replace('\\', '/');
        }
        boolean bcreated = false;
        boolean bparent = false;
        File file = new File(filepath);
        String ppath = file.getParent();
        if (ppath.startsWith("\\")) {
            ppath = ppath.replace('\\', '/');
        }
        try {
            sftp.cd(ppath);
            bparent = true;
        } catch (SftpException e1) {
            bparent = false;
        }
        try {
            if (bparent) {
                try {
                    sftp.cd(filepath);
                    bcreated = true;
                } catch (Exception e) {
                    bcreated = false;
                }
                if (!bcreated) {
                    sftp.mkdir(filepath);
                    bcreated = true;
                }
                //return;
            } else {
                createDir(ppath, sftp);
                sftp.cd(ppath);
                sftp.mkdir(filepath);
            }
        } catch (SftpException e) {
            LOG.error("mkdir failed :" + filepath);
        }
        try {
            sftp.cd(filepath);
        } catch (SftpException e) {
            LOG.error("Can't cd into :" + filepath);
        }

    }

    /*public static boolean uploadFile(SFTPInfo sftpInfo, String sourceFile, String descFile) throws SFTPException{
    	boolean isSuccess = false;
    	ChannelSftp sftp = null;
    	try {
    		sftp = SFTPUtil.connect(sftpInfo);
    		SFTPUtil.upload(sourceFile, descFile, sftp);
    		isSuccess = true;
    	} finally {
    		SFTPUtil.disconnect(sftp);
    	}
    	return isSuccess;
    }*/

    

}
