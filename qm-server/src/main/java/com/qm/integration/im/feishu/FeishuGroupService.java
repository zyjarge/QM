package com.qm.integration.im.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuGroupService {

    private final FeishuClient client;

    public String createGroup(String name, String description, String ownerOpenId) {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("description", description);
        body.put("chat_mode", "group");
        body.put("chat_type", "private");
        if (ownerOpenId != null) {
            body.put("owner_id", ownerOpenId);
        }

        JSONObject resp = client.post("/im/v1/chats", body);
        if (resp.getIntValue("code") != 0) {
            throw new RuntimeException("建群失败: " + resp.getString("msg"));
        }
        return resp.getJSONObject("data").getString("chat_id");
    }

    public void addMembers(String chatId, List<String> openIds) {
        JSONObject body = new JSONObject();
        body.put("id_list", JSONArray.from(openIds));
        JSONObject resp = client.post("/im/v1/chats/" + chatId + "/members", body);
        if (resp.getIntValue("code") != 0) {
            log.warn("拉人入群失败: {}", resp.getString("msg"));
        }
    }

    public void updateGroupName(String chatId, String name) {
        JSONObject body = new JSONObject();
        body.put("name", name);
        JSONObject resp = client.put("/im/v1/chats/" + chatId, body);
        if (resp.getIntValue("code") != 0) {
            log.warn("改群名失败: {}", resp.getString("msg"));
        }
    }

    public void dissolveGroup(String chatId) {
        JSONObject resp = client.delete("/im/v1/chats/" + chatId);
        if (resp.getIntValue("code") != 0) {
            log.warn("解散群失败: {}", resp.getString("msg"));
        }
    }

    public String sendCard(String chatId, String cardJson) {
        JSONObject body = new JSONObject();
        body.put("receive_id", chatId);
        body.put("msg_type", "interactive");
        body.put("content", cardJson);

        JSONObject resp = client.post("/im/v1/messages?receive_id_type=chat_id", body);
        if (resp.getIntValue("code") != 0) {
            throw new RuntimeException("发卡片失败: " + resp.getString("msg"));
        }
        return resp.getJSONObject("data").getString("message_id");
    }

    public String sendText(String chatId, String text) {
        JSONObject content = new JSONObject();
        content.put("text", text);
        JSONObject body = new JSONObject();
        body.put("receive_id", chatId);
        body.put("msg_type", "text");
        body.put("content", content.toJSONString());

        JSONObject resp = client.post("/im/v1/messages?receive_id_type=chat_id", body);
        if (resp.getIntValue("code") != 0) {
            throw new RuntimeException("发消息失败: " + resp.getString("msg"));
        }
        return resp.getJSONObject("data").getString("message_id");
    }

    public List<JSONObject> getChatMembers(String chatId) {
        JSONObject resp = client.get("/im/v1/chats/" + chatId + "/members",
            java.util.Map.of("page_size", 50));
        if (resp.getIntValue("code") != 0) {
            log.warn("查群成员失败: {}", resp.getString("msg"));
            return List.of();
        }
        return resp.getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }
}
