import json
import sys


def main():
    src, out, version = sys.argv[1], sys.argv[2], sys.argv[3]
    items = [t for t in json.load(open(src)) if t["matches"] == 1]
    lines = ["#pragma once", "", "#include <cstdint>", "", "namespace owg {", "namespace targets {", ""]
    lines += ["struct Signature {", "    const char *name;", "    uint32_t size;", "    int count;", "    const uint32_t *words;", "    const uint32_t *mask;", "};", ""]
    for i, t in enumerate(items):
        lines.append("static const uint32_t kWords%d[] = {%s};" % (i, ", ".join("0x%08X" % w for w in t["words"])))
        lines.append("static const uint32_t kMask%d[] = {%s};" % (i, ", ".join("0x%08X" % w for w in t["mask"])))
    lines.append("")
    lines.append('static const char *const kGameVersion = "%s";' % version)
    lines.append("")
    lines.append("static const Signature kAll[] = {")
    for i, t in enumerate(items):
        lines.append('    {"%s", %d, %d, kWords%d, kMask%d},' % (t["name"], t["size"], len(t["words"]), i, i))
    lines.append("};")
    lines.append("")
    lines.append("static const int kAllCount = %d;" % len(items))
    lines += ["", "}", "}", ""]
    open(out, "w").write("\n".join(lines))


if __name__ == "__main__":
    main()
