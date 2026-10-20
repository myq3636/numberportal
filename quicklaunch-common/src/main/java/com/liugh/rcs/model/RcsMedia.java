package com.liugh.rcs.model;

/**
 * 媒体引用：url 可为公网 URL 或 files/{uid}（发送前解析成公网 URL）。
 */
public class RcsMedia {

    private String url;
    private String thumbnailUrl;
    private String mimeType;
    /** SHORT / MEDIUM / TALL，通道不支持则忽略并记 degrade */
    private String height;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }
}
