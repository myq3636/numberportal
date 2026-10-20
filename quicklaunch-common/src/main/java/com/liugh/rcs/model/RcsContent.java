package com.liugh.rcs.model;

import java.util.List;

/**
 * 规范化内部消息内容。北向多种入参先归一化为本类型，再由各通道 PayloadMapper 翻译出去。
 */
public class RcsContent {

    private ContentType type = ContentType.TEXT;

    /** TEXT，<=3072 */
    private String text;

    /** FILE */
    private RcsMedia file;

    /** CARD（standalone） */
    private RcsCard card;

    /** CAROUSEL，2-10 张 */
    private List<RcsCard> cards;

    /** 消息级建议（TEXT/FILE 可带），<=11 */
    private List<RcsSuggestion> suggestions;

    /** TEMPLATE：已注册审核通过的模板 ID。 */
    private String templateId;

    /** TEMPLATE：个性化占位参数（可选）。 */
    private List<RcsTemplateParameter> parameters;
    
    /** CAROUSEL，cardWidth 2-10 张//MEDIUM/SMALL */
    private String cardWidth="MEDIUM";

    public ContentType getType() {
        return type;
    }

    public void setType(ContentType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public RcsMedia getFile() {
        return file;
    }

    public void setFile(RcsMedia file) {
        this.file = file;
    }

    public RcsCard getCard() {
        return card;
    }

    public void setCard(RcsCard card) {
        this.card = card;
    }

    public List<RcsCard> getCards() {
        return cards;
    }

    public void setCards(List<RcsCard> cards) {
        this.cards = cards;
    }

    public List<RcsSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<RcsSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<RcsTemplateParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<RcsTemplateParameter> parameters) {
        this.parameters = parameters;
    }

	public String getCardWidth() {
		return cardWidth;
	}

	public void setCardWidth(String cardWidth) {
		this.cardWidth = cardWidth;
	}
    
    
}
