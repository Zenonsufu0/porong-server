"""servers.dat 용 미니 NBT 읽기/쓰기 (자체·무의존).

Minecraft `servers.dat` 는 **비압축(gzip 아님) NBT**. 서버 목록 자동등록을 위해
필요한 태그만 지원: Compound(10) / List(9) / String(8) / Byte(1) / Int(3).
임의 servers.dat 를 읽어 서버를 append 하고 다시 쓴다(기존 항목 보존).

표현:
- Compound -> list[(name, type, value)]  (순서 보존)
- List     -> (elem_type, [value, ...])
- String   -> str
- Byte/Int -> int
"""
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

_SIMPLE = {
    TAG_BYTE: (">b", 1),
    TAG_SHORT: (">h", 2),
    TAG_INT: (">i", 4),
    TAG_LONG: (">q", 8),
    TAG_FLOAT: (">f", 4),
    TAG_DOUBLE: (">d", 8),
}


class _Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def take(self, n):
        b = self.d[self.i:self.i + n]
        if len(b) != n:
            raise ValueError("NBT: unexpected EOF")
        self.i += n
        return b

    def string(self):
        (n,) = struct.unpack(">H", self.take(2))
        return self.take(n).decode("utf-8")

    def value(self, ttype):
        if ttype in _SIMPLE:
            fmt, n = _SIMPLE[ttype]
            return struct.unpack(fmt, self.take(n))[0]
        if ttype == TAG_STRING:
            return self.string()
        if ttype == TAG_BYTE_ARRAY:
            (n,) = struct.unpack(">i", self.take(4))
            return list(self.take(n))
        if ttype == TAG_LIST:
            (etype,) = struct.unpack(">b", self.take(1))
            (n,) = struct.unpack(">i", self.take(4))
            return (etype, [self.value(etype) for _ in range(n)])
        if ttype == TAG_COMPOUND:
            out = []
            while True:
                (t,) = struct.unpack(">b", self.take(1))
                if t == TAG_END:
                    break
                name = self.string()
                out.append((name, t, self.value(t)))
            return out
        raise ValueError(f"NBT: unsupported tag {ttype}")


class _Writer:
    def __init__(self):
        self.parts = []

    def raw(self, b):
        self.parts.append(b)

    def string(self, s):
        enc = s.encode("utf-8")
        self.parts.append(struct.pack(">H", len(enc)))
        self.parts.append(enc)

    def value(self, ttype, val):
        if ttype in _SIMPLE:
            self.parts.append(struct.pack(_SIMPLE[ttype][0], val))
        elif ttype == TAG_STRING:
            self.string(val)
        elif ttype == TAG_BYTE_ARRAY:
            self.parts.append(struct.pack(">i", len(val)))
            self.parts.append(bytes(val))
        elif ttype == TAG_LIST:
            etype, items = val
            self.parts.append(struct.pack(">b", etype))
            self.parts.append(struct.pack(">i", len(items)))
            for it in items:
                self.value(etype, it)
        elif ttype == TAG_COMPOUND:
            for name, t, v in val:
                self.parts.append(struct.pack(">b", t))
                self.string(name)
                self.value(t, v)
            self.parts.append(struct.pack(">b", TAG_END))
        else:
            raise ValueError(f"NBT: unsupported tag {ttype}")

    def out(self):
        return b"".join(self.parts)


def read_file(path):
    """servers.dat -> (root_name, root_compound_value)."""
    with open(path, "rb") as f:
        data = f.read()
    r = _Reader(data)
    (t,) = struct.unpack(">b", r.take(1))
    if t != TAG_COMPOUND:
        raise ValueError("NBT: root is not compound")
    name = r.string()
    return name, r.value(TAG_COMPOUND)


def write_file(path, root_name, root_value):
    w = _Writer()
    w.raw(struct.pack(">b", TAG_COMPOUND))
    w.string(root_name)
    w.value(TAG_COMPOUND, root_value)
    with open(path, "wb") as f:
        f.write(w.out())


def _find(compound, key):
    for idx, (name, t, v) in enumerate(compound):
        if name == key:
            return idx, t, v
    return None


def register_server(path, name, ip):
    """servers.dat 에 서버 1개 추가(중복 ip 면 갱신). 파일 없으면 새로 생성.

    구조: root{ servers: LIST<COMPOUND>[ {name, ip} ... ] }
    """
    import os
    if os.path.isfile(path):
        root_name, root = read_file(path)
    else:
        root_name, root = "", []

    found = _find(root, "servers")
    if found is None:
        servers = (TAG_COMPOUND, [])
        root.append(("servers", TAG_LIST, servers))
    else:
        idx, _, servers = found
        if servers[0] != TAG_COMPOUND:
            servers = (TAG_COMPOUND, [])
            root[idx] = ("servers", TAG_LIST, servers)

    items = servers[1]
    entry = [("name", TAG_STRING, name), ("ip", TAG_STRING, ip)]
    # 동일 ip 있으면 교체, 없으면 추가
    for i, comp in enumerate(items):
        f = _find(comp, "ip")
        if f and f[2] == ip:
            items[i] = entry
            break
    else:
        items.append(entry)

    write_file(path, root_name, root)
