package com.liugh.rcs.service;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.RcsFile;
import com.liugh.mapper.RcsFileMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * RCS 文件服务：把二进制/URL 变成我方可控的公网 URL，供富媒体消息引用。
 * Infobip RCS 媒体用公网 URL 引用（无需预上传到 Infobip）。
 */
@Service
@Slf4j
public class RcsFileService {

    @Autowired
    private RcsFileMapper rcsFileMapper;

    /** 平台对外基础 URL，形如 http://host/api/v1/ */
    @Value("${project.url}")
    private String projectUrl;

    /** 本地文件存储根目录 */
    @Value("${project.image.path}")
    private String storagePath;

    public RcsFileService() {
    }

    RcsFileService(RcsFileMapper rcsFileMapper, String projectUrl,
            String storagePath) {
        this.rcsFileMapper = rcsFileMapper;
        this.projectUrl = projectUrl;
        this.storagePath = storagePath;
    }

    /** 上传二进制，落地到磁盘并生成可公网访问的 URL */
    public RcsFile upload(Integer ssid, byte[] data, String fileName, String mimeType) throws Exception {
        String uid = UUID.randomUUID().toString().replace("-", "");
        File dir = new File(storagePath, "rcs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, uid);
        Files.write(target.toPath(), data);

        RcsFile rf = new RcsFile();
        rf.setSsid(ssid);
        rf.setUid(uid);
        rf.setFileUri(buildPublicUrl(uid));
        rf.setMimeType(mimeType);
        rf.setFileName(fileName);
        rf.setFileSize((long) data.length);
        rf.setCreateTime(System.currentTimeMillis());
        rcsFileMapper.insert(rf);
        return rf;
    }

    /** 登记一个客户已提供的公网 URL（透传，不落地二进制），便于审计与计费溯源 */
    public RcsFile register(Integer ssid, String fileUrl, String thumbnailUrl, String mimeType) {
        String uid = UUID.randomUUID().toString().replace("-", "");
        RcsFile rf = new RcsFile();
        rf.setSsid(ssid);
        rf.setUid(uid);
        rf.setFileUri(fileUrl);
        rf.setThumbnailUri(thumbnailUrl);
        rf.setMimeType(mimeType);
        rf.setCreateTime(System.currentTimeMillis());
        rcsFileMapper.insert(rf);
        return rf;
    }

    /**
     * 解析媒体引用：files/{uid} → 公网 URL；已是 http(s) URL 则原样返回。
     */
    public String resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith("files/")) {
            String uid = ref.substring("files/".length());
            RcsFile rf = getByUid(uid);
            if (rf != null) {
                return rf.getFileUri();
            }
            log.warn("rcs file ref not found: {}", ref);
            return null;
        }
        return ref;
    }

    /** 读取磁盘上的原始文件（供 GET /rcs/files/{uid}/raw 流式返回） */
    public byte[] loadRaw(String uid) throws Exception {
        if (uid == null || !uid.matches("[A-Za-z0-9_-]{16,64}")) {
            return null;
        }
        File target = new File(new File(storagePath, "rcs"), uid);
        if (!target.exists()) {
            return null;
        }
        return Files.readAllBytes(target.toPath());
    }

    public RcsFile getByUid(String uid) {
        java.util.List<RcsFile> list = rcsFileMapper.selectList(new EntityWrapper<RcsFile>().eq("uid", uid));
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    public RcsFile getOwned(Integer ssid, String uid) {
        RcsFile file = getByUid(uid);
        if (file == null) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }
        if (ssid == null || !ssid.equals(file.getSsid())) {
            throw new RcsCustomerAccessException("FORBIDDEN_RESOURCE");
        }
        return file;
    }

    public void deleteOwned(Integer ssid, String uid) throws Exception {
        RcsFile file = getOwned(ssid, uid);
        File target = new File(new File(storagePath, "rcs"), file.getUid());
        Files.deleteIfExists(target.toPath());
        rcsFileMapper.deleteById(file.getId());
    }

    private String buildPublicUrl(String uid) {
        String base = projectUrl;
        if (base != null && !base.endsWith("/")) {
            base = base + "/";
        }
        return base + "rcs/files/" + uid + "/raw";
    }
}
