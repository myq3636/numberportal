package com.liugh.rcs.model;

/**
 * 模板消息占位参数（name/value）。发送模板消息时用于个性化填充。
 */
public class RcsTemplateParameter {

    /** 参数名，对应模板中声明的占位符。 */
    private String name;
    /** 参数值。 */
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
