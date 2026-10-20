package com.liugh.rcs.model;

import java.util.List;

/**
 * 富卡片（standalone 或 carousel 中的一张）。
 */
public class RcsCard {

    /** <=200 */
    private String title;
    /** <=2000 */
    private String description;
    private RcsMedia media;
    /** 卡内建议，<=4 */
    private List<RcsSuggestion> suggestions;
    /** HORIZONTAL / VERTICAL */
    private String orientation;
    /** "LEFT" "RIGHT" */
    private String alignment="LEFT";
    /**  "SMALL" "MEDIUM" */
    private String cardWidth="MEDIUM";
    
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RcsMedia getMedia() {
        return media;
    }

    public void setMedia(RcsMedia media) {
        this.media = media;
    }

    public List<RcsSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RcsSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

	public String getAlignment() {
		return alignment;
	}

	public void setAlignment(String alignment) {
		this.alignment = alignment;
	}

	public String getCardWidth() {
		return cardWidth;
	}

	public void setCardWidth(String cardWidth) {
		this.cardWidth = cardWidth;
	}
    
    
}
