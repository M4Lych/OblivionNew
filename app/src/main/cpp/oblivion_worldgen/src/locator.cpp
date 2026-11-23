#include "locator.h"

#include <algorithm>
#include <cstring>
#include <map>

namespace owg {

namespace {

const void *findBytes(const char *hay, size_t hayLen, const char *needle, size_t needleLen) {
    if (needleLen == 0 || hayLen < needleLen) return nullptr;
    const char *end = hay + (hayLen - needleLen) + 1;
    for (const char *p = hay; p < end;) {
        p = static_cast<const char *>(std::memchr(p, needle[0], static_cast<size_t>(end - p)));
        if (!p) return nullptr;
        if (std::memcmp(p, needle, needleLen) == 0) return p;
        p++;
    }
    return nullptr;
}

struct Range {
    uintptr_t lo;
    uintptr_t hi;
};

void collectRanges(const ModuleView &mod, const char *needle, std::vector<Range> &ranges) {
    size_t len = std::strlen(needle);
    for (const Segment &seg : mod.segments) {
        const char *p = reinterpret_cast<const char *>(seg.begin);
        const char *end = reinterpret_cast<const char *>(seg.end);
        while (p < end) {
            const void *hit = findBytes(p, static_cast<size_t>(end - p), needle, len);
            if (!hit) break;
            const char *h = static_cast<const char *>(hit);
            const char *start = h;
            const char *floor = reinterpret_cast<const char *>(seg.begin);
            int back = 0;
            while (start > floor && start[-1] != '\0' && back < 400) {
                start--;
                back++;
            }
            ranges.push_back({reinterpret_cast<uintptr_t>(start), reinterpret_cast<uintptr_t>(h)});
            p = h + len;
        }
    }
}

bool containsTarget(const std::vector<Range> &ranges, uintptr_t target) {
    for (const Range &r : ranges)
        if (target >= r.lo && target <= r.hi) return true;
    return false;
}

void collectXrefs(const ModuleView &mod, const std::vector<Range> &ranges, std::vector<uintptr_t> &out) {
    for (const Segment &seg : mod.segments) {
        if (!seg.exec) continue;
        uintptr_t begin = (seg.begin + 3) & ~static_cast<uintptr_t>(3);
        const uint32_t *w = reinterpret_cast<const uint32_t *>(begin);
        size_t n = (seg.end - begin) / 4;
        for (size_t i = 0; i + 2 < n; i++) {
            uint32_t a = w[i];
            if ((a & 0x9F000000u) != 0x90000000u) continue;
            uint32_t rd = a & 31u;
            int64_t imm = static_cast<int64_t>(((a >> 5) & 0x7FFFFu) << 2 | ((a >> 29) & 3u));
            if (imm & (1 << 20)) imm -= (1 << 21);
            uintptr_t pc = begin + i * 4;
            uintptr_t page = (pc & ~static_cast<uintptr_t>(0xFFF)) + static_cast<uintptr_t>(imm * 4096);
            bool near = false;
            for (const Range &r : ranges) {
                if (page + 0x1000 > r.lo && page <= r.hi) {
                    near = true;
                    break;
                }
            }
            if (!near) continue;
            for (int d = 1; d <= 2; d++) {
                uint32_t b = w[i + d];
                if ((b & 0xFFC00000u) != 0x91000000u) continue;
                if (((b >> 5) & 31u) != rd) continue;
                uintptr_t target = page + ((b >> 10) & 0xFFFu);
                if (containsTarget(ranges, target)) {
                    out.push_back(pc);
                    break;
                }
            }
        }
    }
}

bool functionOf(const ModuleView &mod, uintptr_t pc, uintptr_t &start, size_t &size) {
    if (!mod.ehHdr) return false;
    const uint8_t *hdr = reinterpret_cast<const uint8_t *>(mod.ehHdr);
    if (hdr[0] != 1) return false;
    uint32_t count;
    std::memcpy(&count, hdr + 8, 4);
    const int32_t *table = reinterpret_cast<const int32_t *>(hdr + 12);
    size_t lo = 0, hi = count;
    while (lo < hi) {
        size_t mid = (lo + hi) / 2;
        uintptr_t loc = mod.ehHdr + static_cast<uintptr_t>(static_cast<intptr_t>(table[mid * 2]));
        if (loc <= pc) lo = mid + 1;
        else hi = mid;
    }
    if (lo == 0) return false;
    size_t idx = lo - 1;
    uintptr_t loc = mod.ehHdr + static_cast<uintptr_t>(static_cast<intptr_t>(table[idx * 2]));
    const uint8_t *fde = reinterpret_cast<const uint8_t *>(mod.ehHdr + static_cast<uintptr_t>(static_cast<intptr_t>(table[idx * 2 + 1])));
    int32_t range;
    std::memcpy(&range, fde + 12, 4);
    if (range <= 0 || pc >= loc + static_cast<uintptr_t>(range)) return false;
    start = loc;
    size = static_cast<size_t>(range);
    return true;
}

}

bool locateFunction(const ModuleView &mod, const char *needle, Located &out) {
    out = Located();
    std::vector<Range> ranges;
    collectRanges(mod, needle, ranges);
    out.occurrences = static_cast<int>(ranges.size());
    if (ranges.empty()) return false;
    std::vector<uintptr_t> xrefs;
    collectXrefs(mod, ranges, xrefs);
    out.xrefs = static_cast<int>(xrefs.size());
    std::map<uintptr_t, std::pair<size_t, int>> funcs;
    for (uintptr_t pc : xrefs) {
        uintptr_t start;
        size_t size;
        if (!functionOf(mod, pc, start, size)) continue;
        auto &entry = funcs[start];
        entry.first = size;
        entry.second++;
    }
    out.functions = static_cast<int>(funcs.size());
    if (funcs.empty()) return false;
    uintptr_t best = 0;
    int bestCount = 0;
    int ties = 0;
    for (const auto &kv : funcs) {
        if (kv.second.second > bestCount) {
            best = kv.first;
            bestCount = kv.second.second;
            ties = 1;
        } else if (kv.second.second == bestCount) {
            ties++;
        }
    }
    if (ties != 1) return false;
    out.addr = best;
    out.size = funcs[best].first;
    return true;
}

}
