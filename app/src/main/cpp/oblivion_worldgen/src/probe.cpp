#include "probe.h"

#include <android/log.h>
#include <link.h>
#include <shadowhook.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>

#include "locator.h"

namespace owg {

namespace {

const char *kTag = "OblivionWG";
const char *kLibName = "libminecraftpe.so";

std::mutex g_lock;
std::atomic<bool> g_started{false};
std::atomic<uint64_t> g_cellCalls{0};
std::atomic<uint64_t> g_densityCalls{0};
std::string g_status = "idle";

ModuleView g_module;

int moduleCallback(struct dl_phdr_info *info, size_t, void *data) {
    ModuleView *mod = static_cast<ModuleView *>(data);
    if (!info->dlpi_name || !std::strstr(info->dlpi_name, kLibName)) return 0;
    mod->name = info->dlpi_name;
    mod->base = info->dlpi_addr;
    for (int i = 0; i < info->dlpi_phnum; i++) {
        const ElfW(Phdr) &ph = info->dlpi_phdr[i];
        if (ph.p_type == PT_LOAD && (ph.p_flags & PF_R)) {
            Segment seg;
            seg.begin = info->dlpi_addr + ph.p_vaddr;
            seg.end = info->dlpi_addr + ph.p_vaddr + ph.p_memsz;
            seg.exec = (ph.p_flags & PF_X) != 0;
            mod->segments.push_back(seg);
        } else if (ph.p_type == PT_GNU_EH_FRAME) {
            mod->ehHdr = info->dlpi_addr + ph.p_vaddr;
        }
    }
    return 1;
}

bool findModule(ModuleView &mod) {
    mod = ModuleView();
    dl_iterate_phdr(moduleCallback, &mod);
    return !mod.segments.empty() && mod.ehHdr != 0;
}

struct Target {
    const char *name;
    const char *needle;
};

const Target kTargets[] = {
    {"prepareHeightsForCells", "OverworldGeneratorMultinoise::_prepareHeightsForCells("},
    {"prepareHeightsForColumn", "OverworldGeneratorMultinoise::prepareHeightsForColumn("},
    {"prepareHeights2d", "OverworldGenerator2d::_prepareHeights("},
    {"attenuateDensity", "ChunkBlender::attenuateDensity("},
    {"netherPostProcess", "NetherGenerator::decorationPostProcessChunk("},
    {"endPostProcess", "TheEndGenerator::decorationPostProcessChunk("},
    {"endSpawn", "TheEndGenerator::findSpawnPosition("},
    {"biomeArea3d", "BiomeSource3d::getBiomeArea("},
};

void cellsProxy(void *self, void *volume, const void *chunkPos, const void *cache, void *aquifer, void *fn,
                bool flag, uint64_t x7, uint64_t s0, uint64_t s1, uint64_t s2, uint64_t s3, uint64_t s4,
                uint64_t s5, uint64_t s6, uint64_t s7) {
    SHADOWHOOK_STACK_SCOPE();
    uint64_t n = g_cellCalls.fetch_add(1, std::memory_order_relaxed) + 1;
    if (n == 1 || n % 4096 == 0) {
        int32_t cx = 0, cz = 0;
        if (chunkPos) {
            std::memcpy(&cx, chunkPos, sizeof(cx));
            std::memcpy(&cz, static_cast<const char *>(chunkPos) + 4, sizeof(cz));
        }
        __android_log_print(ANDROID_LOG_INFO, kTag, "prepareHeightsForCells calls=%llu chunk=(%d,%d)",
                            static_cast<unsigned long long>(n), cx, cz);
    }
    SHADOWHOOK_CALL_PREV(cellsProxy, self, volume, chunkPos, cache, aquifer, fn, flag, x7, s0, s1, s2, s3, s4,
                         s5, s6, s7);
}

float densityProxy(void *self, const void *pos, int cell, float density) {
    SHADOWHOOK_STACK_SCOPE();
    uint64_t n = g_densityCalls.fetch_add(1, std::memory_order_relaxed) + 1;
    if (n == 1 || n % 65536 == 0) {
        __android_log_print(ANDROID_LOG_INFO, kTag, "attenuateDensity calls=%llu", static_cast<unsigned long long>(n));
    }
    return SHADOWHOOK_CALL_PREV(densityProxy, self, pos, cell, density);
}

void setStatus(const std::string &text) {
    std::lock_guard<std::mutex> guard(g_lock);
    g_status = text;
    __android_log_print(ANDROID_LOG_INFO, kTag, "%s", text.c_str());
}

void worker() {
    bool ready = false;
    for (int i = 0; i < 1200; i++) {
        if (findModule(g_module)) {
            ready = true;
            break;
        }
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    if (!ready) {
        setStatus("game library was not loaded");
        g_started.store(false);
        return;
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));
    double execMb = 0;
    for (const Segment &seg : g_module.segments)
        if (seg.exec) execMb += static_cast<double>(seg.end - seg.begin) / 1048576.0;
    char head[256];
    std::snprintf(head, sizeof(head), "module=%s base=0x%llx segments=%zu exec=%.1fMB ", g_module.name.c_str(),
                  static_cast<unsigned long long>(g_module.base), g_module.segments.size(), execMb);
    std::string report = head;
    int located = 0;
    uintptr_t cellsAddr = 0, densityAddr = 0;
    for (const Target &t : kTargets) {
        Located loc;
        bool ok = locateFunction(g_module, t.needle, loc);
        char line[200];
        if (ok) {
            located++;
            std::snprintf(line, sizeof(line), "%s=+0x%llx(%zu) ", t.name,
                          static_cast<unsigned long long>(loc.addr - g_module.base), loc.size);
            if (std::strcmp(t.name, "prepareHeightsForCells") == 0) cellsAddr = loc.addr;
            if (std::strcmp(t.name, "attenuateDensity") == 0) densityAddr = loc.addr;
        } else {
            std::snprintf(line, sizeof(line), "%s=miss(str=%d,xref=%d,fn=%d) ", t.name, loc.occurrences, loc.xrefs,
                          loc.functions);
        }
        report += line;
        __android_log_print(ANDROID_LOG_INFO, kTag, "%s", line);
    }
    if (located == 0) {
        setStatus(report + "| nothing located, this game build is not supported");
        g_started.store(false);
        return;
    }
    if (shadowhook_init(SHADOWHOOK_MODE_SHARED, false) != 0) {
        setStatus(report + "| shadowhook init failed");
        g_started.store(false);
        return;
    }
    int hooked = 0;
    void *stub = nullptr;
    void *origCells = nullptr;
    void *origDensity = nullptr;
    if (cellsAddr) {
        stub = shadowhook_hook_func_addr(reinterpret_cast<void *>(cellsAddr), reinterpret_cast<void *>(cellsProxy), &origCells);
        if (stub) hooked++;
    }
    if (densityAddr) {
        stub = shadowhook_hook_func_addr(reinterpret_cast<void *>(densityAddr), reinterpret_cast<void *>(densityProxy), &origDensity);
        if (stub) hooked++;
    }
    setStatus(report + "| observers installed=" + std::to_string(hooked));
}

}

bool probeStart() {
    bool expected = false;
    if (!g_started.compare_exchange_strong(expected, true)) return false;
    setStatus("waiting for the game library");
    std::thread(worker).detach();
    return true;
}

std::string probeStatus() {
    std::lock_guard<std::mutex> guard(g_lock);
    return g_status + " | cells=" + std::to_string(g_cellCalls.load()) + " density=" + std::to_string(g_densityCalls.load());
}

}
