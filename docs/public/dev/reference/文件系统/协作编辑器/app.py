#!/usr/bin/env python3
"""极简协同文本编辑器：纯标准库后端，SSE 广播 + HTTP 操作接口。"""
import hashlib
import json
import os
import queue
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

ROOT = Path(__file__).resolve().parent
PORT = int(os.environ.get("PORT", "8000"))
COLORS = ["#ff2bd6", "#ff7300", "#008cff", "#00a884", "#7c4dff", "#e83e8c", "#c58b00"]


class Hub:
    def __init__(self):
        self.lock = threading.RLock()
        self.nodes = {}   # id -> [parent, char, deleted]
        self.clients = {} # uid -> {name, color, cursor, q}

    def color(self, uid):
        return COLORS[int(hashlib.md5(uid.encode()).hexdigest(), 16) % len(COLORS)]

    def user(self, uid, c):
        return {"uid": uid, "name": c["name"], "color": c["color"], "cursor": c["cursor"]}

    def broadcast(self, msg, skip=None):
        data = json.dumps(msg, ensure_ascii=True, separators=(",", ":"))
        for uid, c in list(self.clients.items()):
            if uid != skip:
                try:
                    c["q"].put_nowait(data)
                except queue.Full:
                    pass

    def join(self, uid, name, q):
        with self.lock:
            old = self.clients.get(uid)
            c = {
                "name": name,
                "color": old["color"] if old else self.color(uid),
                "cursor": old["cursor"] if old else {"s": "", "e": "", "h": ""},
                "q": q,
            }
            self.clients[uid] = c
            init = {
                "type": "init",
                "you": uid,
                "nodes": [[nid, p, ch, int(d)] for nid, (p, ch, d) in self.nodes.items()],
                "users": [self.user(u, x) for u, x in self.clients.items()],
            }
            q.put(json.dumps(init, ensure_ascii=True, separators=(",", ":")))
            self.broadcast({"type": "join", "user": self.user(uid, c)}, skip=uid)
            return c

    def leave(self, uid, c):
        with self.lock:
            if self.clients.get(uid) is c:
                del self.clients[uid]
                self.broadcast({"type": "leave", "uid": uid})

    def apply(self, op, uid):
        with self.lock:
            for nid in op.get("d") or []:
                if nid in self.nodes:
                    self.nodes[nid][2] = True
            ins = op.get("i")
            if isinstance(ins, dict):
                parent = ins.get("parent") or ""
                for item in ins.get("nodes") or []:
                    if not (isinstance(item, list) and len(item) == 2):
                        continue
                    nid, ch = item
                    if not isinstance(nid, str) or not isinstance(ch, str) or len(ch) > 2:
                        continue
                    if nid in self.nodes:
                        parent = nid
                        continue
                    if parent and parent not in self.nodes:
                        parent = ""
                    self.nodes[nid] = [parent, ch, False]
                    parent = nid
            self.broadcast({"type": "op", "op": op, "by": uid, "at": time.time()})

    def cursor(self, uid, s, e, h):
        with self.lock:
            c = self.clients.get(uid)
            if not c:
                return
            s = s if s in self.nodes else ""
            e = e if e in self.nodes else ""
            h = h if h in self.nodes else e
            c["cursor"] = {"s": s, "e": e, "h": h}
            self.broadcast({"type": "cursor", "uid": uid, "s": s, "e": e, "h": h}, skip=uid)


HUB = Hub()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.0"

    def log_message(self, *args):
        pass

    def _headers(self, code, ctype, length=None, extra=()):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Cache-Control", "no-store")
        if length is not None:
            self.send_header("Content-Length", str(length))
        for k, v in extra:
            self.send_header(k, v)
        self.end_headers()

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/":
            body = (ROOT / "index.html").read_bytes()
            self._headers(200, "text/html; charset=utf-8", len(body))
            self.wfile.write(body)
        elif url.path == "/events":
            self.events(parse_qs(url.query))
        elif url.path == "/favicon.ico":
            self._headers(204, "image/x-icon", 0)
        else:
            self._headers(404, "text/plain; charset=utf-8", 4)
            self.wfile.write(b"404\n")

    def events(self, query):
        uid = (query.get("uid") or [""])[0][:80]
        name = (query.get("name") or ["访客"])[0].strip()[:24] or "访客"
        if not uid:
            self._headers(400, "text/plain", 4)
            self.wfile.write(b"uid\n")
            return
        q = queue.Queue(maxsize=2000)
        c = HUB.join(uid, name, q)
        self._headers(200, "text/event-stream; charset=utf-8", None,
                      [("Connection", "close"), ("X-Accel-Buffering", "no")])
        try:
            while True:
                try:
                    msg = q.get(timeout=15)
                except queue.Empty:
                    self.wfile.write(b": ping\n\n")
                    self.wfile.flush()
                    continue
                self.wfile.write(("data: " + msg + "\n\n").encode("ascii"))
                self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError, OSError):
            pass
        finally:
            HUB.leave(uid, c)

    def do_POST(self):
        if urlparse(self.path).path != "/op":
            self._headers(404, "text/plain", 0)
            return
        try:
            n = int(self.headers.get("Content-Length", "0"))
            data = json.loads(self.rfile.read(n) or b"{}")
        except (ValueError, json.JSONDecodeError):
            return self.reply({"error": "bad json"}, 400)
        if not isinstance(data, dict):
            return self.reply({"error": "bad json"}, 400)
        uid = str(data.get("uid", ""))[:80]
        with HUB.lock:
            known = uid in HUB.clients
        if not known:
            return self.reply({"error": "not joined"}, 403)
        if data.get("type") == "op" and isinstance(data.get("op"), dict):
            HUB.apply(data["op"], uid)
        elif data.get("type") == "cursor":
            HUB.cursor(uid, str(data.get("s", "")), str(data.get("e", "")), str(data.get("h", "")))
        self.reply({"ok": True})

    def reply(self, obj, code=200):
        body = json.dumps(obj).encode()
        self._headers(code, "application/json", len(body))
        self.wfile.write(body)


if __name__ == "__main__":
    ThreadingHTTPServer.allow_reuse_address = True
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    server.daemon_threads = True
    print(f"同步文本框已启动：http://127.0.0.1:{PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
