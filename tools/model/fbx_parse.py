import struct
import zlib


class Node:
    def __init__(self, name):
        self.name = name
        self.props = []
        self.children = []

    def find(self, name):
        for c in self.children:
            if c.name == name:
                return c
        return None

    def find_all(self, name):
        return [c for c in self.children if c.name == name]


def _read_property(buf, pos):
    t = chr(buf[pos])
    pos += 1
    if t == "Y":
        return struct.unpack_from("<h", buf, pos)[0], pos + 2
    if t == "C":
        return buf[pos] != 0, pos + 1
    if t == "I":
        return struct.unpack_from("<i", buf, pos)[0], pos + 4
    if t == "F":
        return struct.unpack_from("<f", buf, pos)[0], pos + 4
    if t == "D":
        return struct.unpack_from("<d", buf, pos)[0], pos + 8
    if t == "L":
        return struct.unpack_from("<q", buf, pos)[0], pos + 8
    if t in "fdlib":
        length, enc, clen = struct.unpack_from("<III", buf, pos)
        pos += 12
        raw = bytes(buf[pos:pos + clen])
        pos += clen
        if enc == 1:
            raw = zlib.decompress(raw)
        fmt = {"f": "f", "d": "d", "l": "q", "i": "i", "b": "?"}[t]
        return list(struct.unpack("<%d%s" % (length, fmt), raw)), pos
    if t == "S":
        length = struct.unpack_from("<I", buf, pos)[0]
        pos += 4
        return bytes(buf[pos:pos + length]).decode("utf-8", "replace"), pos + length
    if t == "R":
        length = struct.unpack_from("<I", buf, pos)[0]
        pos += 4
        return bytes(buf[pos:pos + length]), pos + length
    raise ValueError("bad property type %r" % t)


def _read_node(buf, pos, wide):
    if wide:
        end, nprops, plen, nlen = struct.unpack_from("<QQQB", buf, pos)
        pos += 25
    else:
        end, nprops, plen, nlen = struct.unpack_from("<IIIB", buf, pos)
        pos += 13
    if end == 0:
        return None, pos
    node = Node(bytes(buf[pos:pos + nlen]).decode("utf-8", "replace"))
    pos += nlen
    for _ in range(nprops):
        value, pos = _read_property(buf, pos)
        node.props.append(value)
    while pos < end:
        child, pos = _read_node(buf, pos, wide)
        if child is None:
            break
        node.children.append(child)
    return node, end


def parse(path):
    buf = open(path, "rb").read()
    if not buf.startswith(b"Kaydara FBX Binary"):
        raise ValueError("not a binary FBX file")
    version = struct.unpack_from("<I", buf, 23)[0]
    wide = version >= 7500
    pos = 27
    root = Node("root")
    while pos < len(buf) - 32:
        node, pos = _read_node(buf, pos, wide)
        if node is None:
            break
        root.children.append(node)
    return version, root
