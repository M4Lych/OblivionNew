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

#include "targets.h"

namespace owg {

namespace {

const char *kTag = "OblivionWG";
const char *kLibName = "libminecraftpe.so";

std::mutex g_lock;
std::atomic<bool> g_started{false};
std::atomic<uint64_t> g_cellCalls{0};
std::atomic<uint64_t> g_densityCalls{0};
std::string g_status = "idle";

struct Region {
    uintptr_t begin;
    uintptr_t end;
};

struct ModuleInfo {
    bool found = false;
    uintptr_t base = 0;
    Region regions[8];
    int regionCount = 0;
};

int moduleCallback(struct dl_phdr_info *info, size_t, void *data) {
    ModuleInfo *mod = static_cast<ModuleInfo *>(data);
    if (!info->dlpi_name || !std::strstr(info->dlpi_name, kLibName)) return 0;
    mod->found = true;
    mod->base = info->dlpi_addr;
    for (int i = 0; i < info->dlpi_phnum && mod->regionCount < 8; i++) {
        const ElfW(Phdr) &ph = info->dlpi_phdr[i];
        if (ph.p_type == PT_LOAD && (ph.p_flags & PF_X) && (ph.p_flags & PF_R)) {
            mod->regions[mod->regionCount].begin = info->dlpi_addr + ph.p_vaddr;
            mod->regions[mod->regionCount].end = info->dlpi_addr + ph.p_vaddr + ph.p_memsz;
            mod->regionCount++;
        }
    }
    return 1;
}

ModuleInfo findModule() {
    ModuleInfo mod;
    dl_iterate_phdr(moduleCallback, &mod);
    return mod;
}

uintptr_t scan(const ModuleInfo &mod, const targets::Signature &sig, int &matches) {
    matches = 0;
    uintptr_t found = 0;
    for (int r = 0; r < mod.regionCount; r++) {
        uintptr_t begin = (mod.regions[r].begin + 3) & ~static_cast<uintptr_t>(3);
        uintptr_t end = mod.regions[r].end;
        if (end < begin + static_cast<uintptr_t>(sig.count) * 4) continue;
        const uint32_t first = sig.words[0] & sig.mask[0];
        const uint32_t firstMask = sig.mask[0];
        for (uintptr_t p = begin; p + static_cast<uintptr_t>(sig.count) * 4 <= end; p += 4) {
            const uint32_t *w = reinterpret_cast<const uint32_t *>(p);
            if ((w[0] & firstMask) != first) continue;
            bool ok = true;
            for (int k = 1; k < sig.count; k++) {
                if ((w[k] & sig.mask[k]) != (sig.words[k] & sig.mask[k])) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                matches++;
                if (!found) found = p;
            }
        }
    }
    return found;
}

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
    ModuleInfo mod;
    for (int i = 0; i < 1200; i++) {
        mod = findModule();
        if (mod.found && mod.regionCount > 0) break;
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    if (!mod.found || mod.regionCount == 0) {
        setStatus("game library was not loaded");
        g_started.store(false);
        return;
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));
    std::string report = std::string("target game ") + targets::kGameVersion + ": ";
    int located = 0;
    uintptr_t cellsAddr = 0, densityAddr = 0;
    for (int i = 0; i < targets::kAllCount; i++) {
        int matches = 0;
        uintptr_t addr = scan(mod, targets::kAll[i], matches);
        if (matches == 1) {
            located++;
            if (std::strcmp(targets::kAll[i].name, "OverworldGeneratorMultinoise_prepareHeightsForCells") == 0) cellsAddr = addr;
            if (std::strcmp(targets::kAll[i].name, "ChunkBlender_attenuateDensity") == 0) densityAddr = addr;
        }
        char line[160];
        std::snprintf(line, sizeof(line), "%s=%s(%d) ", targets::kAll[i].name, matches == 1 ? "ok" : "miss", matches);
        report += line;
    }
    if (located == 0) {
        setStatus(report + "no signature matched, this game version is not supported yet");
        g_started.store(false);
        return;
    }
    if (shadowhook_init(SHADOWHOOK_MODE_SHARED, false) != 0) {
        setStatus(report + "shadowhook init failed");
        g_started.store(false);
        return;
    }
    int hooked = 0;
    void *orig = nullptr;
    if (cellsAddr && shadowhook_hook_func_addr(reinterpret_cast<void *>(cellsAddr), reinterpret_cast<void *>(cellsProxy), &orig)) hooked++;
    orig = nullptr;
    if (densityAddr && shadowhook_hook_func_addr(reinterpret_cast<void *>(densityAddr), reinterpret_cast<void *>(densityProxy), &orig)) hooked++;
    setStatus(report + "observers installed=" + std::to_string(hooked));
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
