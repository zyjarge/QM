#!/usr/bin/env python3
"""
QM 飞书技术红线验证脚本 (V1-V6)
设计文档 docs/07-roadmap.md §0 对应项逐项实测

用法:
  export FEISHU_APP_ID=xxx
  export FEISHU_APP_SECRET=xxx
  python3 feishu_verify.py            # 跑全部
  python3 feishu_verify.py V1 V2      # 只跑指定项
"""
import json, os, sys, time, urllib.request, urllib.error

BASE = "https://open.feishu.cn/open-apis"
APP_ID = os.environ.get("FEISHU_APP_ID", "")
APP_SECRET = os.environ.get("FEISHU_APP_SECRET", "")
STATE_FILE = os.path.join(os.path.dirname(__file__), ".verify_state.json")

results = {}
state = json.load(open(STATE_FILE)) if os.path.exists(STATE_FILE) else {}

def save_state():
    json.dump(state, open(STATE_FILE, "w"), indent=2)

def http(method, path, body=None, token=None, raw=False):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            payload = r.read().decode()
            return (r.status, payload if raw else json.loads(payload or "{}"))
    except urllib.error.HTTPError as e:
        payload = e.read().decode()
        try:
            return (e.code, json.loads(payload or "{}"))
        except Exception:
            return (e.code, {"raw": payload})

def report(vid, ok, detail):
    results[vid] = (ok, detail)
    mark = "✅ PASS" if ok else "❌ FAIL"
    print(f"\n[{vid}] {mark}")
    print(f"      {detail}")

# ---------- 公共: 取 tenant_access_token ----------
def get_token():
    st, body = http("POST", "/auth/v3/tenant_access_token/internal",
                    {"app_id": APP_ID, "app_secret": APP_SECRET})
    if st == 200 and body.get("code") == 0:
        return body["tenant_access_token"]
    return None

def v0_token():
    t = get_token()
    if t:
        state["token"] = t
        save_state()
        report("V0", True, f"tenant_access_token 获取成功 (len={len(t)}) — 应用凭证有效")
    else:
        report("V0", False, "token 获取失败: 检查 App ID/Secret 或应用是否已发布版本")
    return t

# ---------- V1 建群/改名/公告/解散 ----------
def v1_group(t):
    # 建群(机器人为群主)
    st, b = http("POST", "/im/v1/chats", {
        "name": "QM红线验证群(自动删除)",
        "description": "V1验证: 建群/改名/公告/解散",
        "chat_mode": "group", "chat_type": "private",
    }, t)
    if b.get("code") != 0:
        report("V1", False, f"建群失败: code={b.get('code')} msg={b.get('msg')} — 检查 im:chat 权限与版本发布")
        return
    chat_id = b["data"]["chat_id"]
    state["chat_id"] = chat_id
    save_state()
    steps = [f"建群 OK chat_id={chat_id}"]

    # 改名
    st, b = http("PUT", f"/im/v1/chats/{chat_id}", {"name": "QM红线验证群-改名OK"}, t)
    steps.append(f"改名 {'OK' if b.get('code')==0 else 'FAIL:'+b.get('msg','')}")

    # 群公告
    # 公告API需要块编辑格式+先获取revision,跳过(不影响群引擎核心能力验证)
    steps.append(f"公告 SKIP(块编辑格式,联调期直接手动验证)")

    report("V1", all("OK" in s for s in steps), " | ".join(steps) + " | 群保留供 V3 使用, 最后统一解散")

def v1b_dissolve(t):
    chat_id = state.get("chat_id")
    if not chat_id:
        report("V1b", False, "无 chat_id, 先跑 V1"); return
    st, b = http("DELETE", f"/im/v1/chats/{chat_id}", token=t)
    ok = b.get("code") == 0
    if ok:
        state.pop("chat_id", None); save_state()
    report("V1b", ok, f"解散群 {chat_id}: {'OK' if ok else 'FAIL: '+b.get('msg','')}")

# ---------- V2 群内卡片发送(按钮回调需在服务端验证,这里先验证卡片可发) ----------
def v2_card(t):
    chat_id = state.get("chat_id")
    if not chat_id:
        report("V2", False, "无 chat_id, 先跑 V1"); return
    card = {
        "config": {"wide_screen_mode": True},
        "header": {"title": {"tag": "plain_text", "content": "【红线验证】V2 卡片按钮"},
                   "template": "blue"},
        "elements": [
            {"tag": "div", "text": {"tag": "plain_text", "content": "点击按钮测试回调(需服务端在线时验证)"}},
            {"tag": "action", "actions": [
                {"tag": "button", "text": {"tag": "plain_text", "content": "测试按钮"},
                 "type": "primary", "value": {"act": "verify", "v": "2"}}
            ]}
        ]
    }
    st, b = http("POST", "/im/v1/messages?receive_id_type=chat_id",
                 {"receive_id": chat_id, "msg_type": "interactive",
                  "content": json.dumps(card)}, t)
    if b.get("code") == 0:
        mid = b["data"]["message_id"]
        state["card_msg_id"] = mid; save_state()
        report("V2", True, f"交互卡片已发入群 message_id={mid} | 按钮回调验证需等 webhook 服务起来后在浏览器点按钮确认")
    else:
        report("V2", False, f"卡片发送失败: code={b.get('code')} msg={b.get('msg')}")

# ---------- V3 读群消息历史(V4同源,先拉权限内可读范围) ----------
def v3_read_history(t):
    chat_id = state.get("chat_id")
    if not chat_id:
        report("V3", False, "无 chat_id, 先跑 V1"); return
    st, b = http("GET", f"/im/v1/messages?container_id_type=chat&container_id={chat_id}&page_size=20", token=t)
    if b.get("code") == 0:
        items = b["data"].get("items", [])
        report("V3", True, f"读取群消息历史 OK, 当前 {len(items)} 条 (V4 解散前导出同此接口, 分页 page_token 可用: {'page_token' in b['data']})")
    else:
        report("V3", False, f"读历史失败: code={b.get('code')} msg={b.get('msg')} — 可能需要 im:message 相关读权限或机器人必须在群内")

# ---------- V4 机器人拉人(需要一个测试 user open_id, 通过通讯录拿自己) ----------
def v4_invite(t):
    chat_id = state.get("chat_id")
    # 从通讯录拿一个用户(拿应用的可见范围第一人)
    # 先查应用可见范围(users列表在可见范围小时可能返回0条,用scopes更可靠)
    st, b = http("GET", "/contact/v3/scopes", token=t)
    if b.get("code") != 0:
        report("V4", False, f"scopes查询失败: code={b.get('code')} msg={b.get('msg')}")
        return
    user_ids = b.get("data", {}).get("user_ids", [])
    if not user_ids:
        report("V4", False, "可见范围为空: 去版本管理检查可用范围设置+重新发布")
        return
    open_id = user_ids[0]
    # 用open_id查用户详情
    st, b = http("GET", f"/contact/v3/users/{open_id}?user_id_type=open_id", token=t)
    if b.get("code") != 0:
        report("V4", False, f"用户查询失败: code={b.get('code')} msg={b.get('msg')}")
        return
    name = b.get("data", {}).get("user", {}).get("name", "?")
    if not chat_id:
        report("V4", False, f"通讯录 OK(拿到 {name} 的 open_id={open_id[:20]}...) 但无 chat_id, 先跑 V1")
        return
    st, b = http("POST", f"/im/v1/chats/{chat_id}/members",
                 {"id_list": [open_id]}, t)
    ok = b.get("code") == 0
    report("V4", ok, f"通讯录查询: {name}({open_id[:20]}...) | 拉人入群: {'OK' if ok else 'code='+str(b.get('code'))+' '+b.get('msg','')}")

# ---------- V5 免登链路预检(app_access_token + 授权URL生成; 真实code换token需浏览器) ----------
def v5_oauth_precheck(t):
    st, b = http("POST", "/auth/v3/app_access_token/internal",
                 {"app_id": APP_ID, "app_secret": APP_SECRET})
    if b.get("code") != 0:
        report("V5", False, f"app_access_token 获取失败: {b.get('msg')}")
        return
    auth_url = (f"https://open.feishu.cn/open-apis/authen/v1/authorize"
                f"?app_id={APP_ID}&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback")
    report("V5", True, "app_access_token OK | 免登授权URL可构造(真实 code 换 user_access_token 需浏览器登录后回调, 联调期验证)")

def main():
    if not APP_ID or not APP_SECRET:
        print("ERROR: 请先 export FEISHU_APP_ID / FEISHU_APP_SECRET"); sys.exit(1)
    which = [a.upper() for a in sys.argv[1:]] or ["ALL"]
    print("=" * 56)
    print("QM 飞书红线验证  (app_id=%s...%s)" % (APP_ID[:8], APP_ID[-4:]))
    print("=" * 56)

    t = v0_token()
    if not t:
        summary(); sys.exit(1)

    run_all = "ALL" in which
    if run_all or "V1" in which: v1_group(t)
    if run_all or "V2" in which: v2_card(t)
    if run_all or "V3" in which: v3_read_history(t)
    if run_all or "V4" in which: v4_invite(t)
    if run_all or "V5" in which: v5_oauth_precheck(t)
    if run_all or "V1B" in which or "CLEAN" in which: v1b_dissolve(t)
    summary()

def summary():
    print("\n" + "=" * 56)
    print("验证汇总")
    print("=" * 56)
    for vid, (ok, detail) in sorted(results.items()):
        print(f"  {vid:4} {'✅' if ok else '❌'}  {detail[:80]}")
    passed = sum(1 for ok, _ in results.values() if ok)
    print(f"\n  {passed}/{len(results)} 项通过")

if __name__ == "__main__":
    main()
