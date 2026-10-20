package com.liugh.rcs.channel.infobip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsContentValidator;

public class InfobipPayloadMapperTest {

    @Test
    public void mapsTemplateContentToProviderTemplatePayload() {
        JSONObject request = new JSONObject();
        JSONObject content = new JSONObject();
        content.put("type", "TEMPLATE");
        content.put("templateId", "tpl-123");
        JSONArray parameters = new JSONArray();
        JSONObject p = new JSONObject();
        p.put("name", "orderId");
        p.put("value", "A-100");
        parameters.add(p);
        content.put("parameters", parameters);
        request.put("content", content);

        RcsContent parsed = RcsContentParser.parse(request);
        assertNull(RcsContentValidator.validate(parsed));
        assertEquals("tpl-123", parsed.getTemplateId());
        assertEquals("A-100", parsed.getParameters().get(0).getValue());

        Map<String, Object> payload = InfobipPayloadMapper.toSendPayload(
                parsed, "snd-1", "+8613800138000", "msg-1",
                "https://cb.example", null, null, null);

        JSONObject out = JSONObject.parseObject(JSONObject.toJSONString(payload));
        JSONObject outContent = out.getJSONArray("messages").getJSONObject(0).getJSONObject("content");
        assertEquals("PROVIDER_TEMPLATE", outContent.getString("type"));
        assertEquals("tpl-123", outContent.getString("templateId"));
        JSONArray outParams = outContent.getJSONArray("parameters");
        assertEquals(1, outParams.size());
        assertEquals("A-100", outParams.getJSONObject(0).getString("value"));
    }

    @Test
    public void rejectsTemplateWithoutTemplateId() {
        RcsContent c = new RcsContent();
        c.setType(ContentType.TEMPLATE);
        assertNotNull(RcsContentValidator.validate(c));
    }
}
