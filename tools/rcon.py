#!/usr/bin/env python3
"""Minimal Minecraft RCON client used by the CI smoke test.

Protocol: https://wiki.vg/RCON — little-endian length-prefixed packets with a
request id, type (3 = login, 2 = command), and null-terminated payload.
"""
import socket
import struct
import sys
import time

SERVER_INFO = 0
LOGIN = 3
COMMAND = 2
RESPONSE = 0


class Rcon:
    def __init__(self, host, port, password, timeout=10.0):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.req_id = 1
        self._send(LOGIN, password.encode("utf-8"))
        kind, payload = self._recv()
        if kind == SERVER_INFO and payload == b"":
            raise RuntimeError("RCON login rejected")

    def _send(self, ptype, payload):
        self.req_id += 1
        data = struct.pack("<ii", self.req_id, ptype) + payload + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        return self.req_id

    def _recv(self):
        (length,) = struct.unpack("<i", self.sock.recv(4))
        data = b""
        while len(data) < length:
            chunk = self.sock.recv(length - len(data))
            if not chunk:
                break
            data += chunk
        req_id, ptype = struct.unpack("<ii", data[:8])
        payload = data[8:-2]
        return ptype, payload

    def command(self, cmd):
        self._send(COMMAND, cmd.encode("utf-8"))
        time.sleep(0.2)
        _, payload = self._recv()
        return payload.decode("utf-8", errors="replace")

    def close(self):
        try:
            self.sock.close()
        except OSError:
            pass


def main():
    host = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 25575
    password = sys.argv[3] if len(sys.argv) > 3 else "smoke"

    rcon = Rcon(host, port, password)
    failures = []

    def check(name, cmd, expect):
        out = rcon.command(cmd)
        print(f"[{name}] {cmd!r} -> {out!r}")
        if expect not in out:
            failures.append(f"{name}: expected {expect!r} in {out!r}")

    # Unknown commands return an error; ours must exist and respond.
    check("reload", "kinetics reload", "reloaded")
    check("toggle-off", "kinetics toggle dash", "dash is now disabled")
    check("toggle-on", "kinetics toggle dash", "dash is now enabled")
    check("toggle-all-off", "kinetics toggle all", "all is now disabled")
    check("toggle-all-on", "kinetics toggle all", "all is now enabled")
    check("info-no-player", "kinetics info", "kinetics")
    check("bad-ability", "kinetics toggle warp_drive", "Unknown ability")

    # Exercise custom entity registration and server-side tick logic.
    check("summon-nullhook", "summon myfirstmod:nullhook 0 -60 0", "Summoned")
    rcon.command("kill @e[type=myfirstmod:nullhook]")

    # Command feedback for a non-player must not crash the server.
    rcon.command("kinetics energy 50")
    rcon.command("stop")

    rcon.close()
    if failures:
        for f in failures:
            print("FAIL:", f)
        sys.exit(1)
    print("All kinetics command checks passed")


if __name__ == "__main__":
    main()
