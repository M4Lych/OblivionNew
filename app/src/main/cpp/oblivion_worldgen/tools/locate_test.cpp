#include "locator.h"

#include <elf.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#include <chrono>
#include <cstdio>
#include <cstring>

int main(int argc, char **argv) {
    if (argc < 2) return 1;
    int fd = open(argv[1], O_RDONLY);
    struct stat st;
    fstat(fd, &st);
    unsigned char *file = static_cast<unsigned char *>(mmap(nullptr, static_cast<size_t>(st.st_size), PROT_READ, MAP_PRIVATE, fd, 0));
    Elf64_Ehdr *eh = reinterpret_cast<Elf64_Ehdr *>(file);
    Elf64_Phdr *ph = reinterpret_cast<Elf64_Phdr *>(file + eh->e_phoff);
    uint64_t top = 0;
    for (int i = 0; i < eh->e_phnum; i++)
        if (ph[i].p_type == PT_LOAD && ph[i].p_vaddr + ph[i].p_memsz > top) top = ph[i].p_vaddr + ph[i].p_memsz;
    unsigned char *mem = static_cast<unsigned char *>(mmap(nullptr, top, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE, -1, 0));
    owg::ModuleView mod;
    mod.base = reinterpret_cast<uintptr_t>(mem);
    for (int i = 0; i < eh->e_phnum; i++) {
        if (ph[i].p_type == PT_LOAD) {
            std::memcpy(mem + ph[i].p_vaddr, file + ph[i].p_offset, ph[i].p_filesz);
            mod.segments.push_back({mod.base + ph[i].p_vaddr, mod.base + ph[i].p_vaddr + ph[i].p_memsz, (ph[i].p_flags & PF_X) != 0});
        } else if (ph[i].p_type == PT_GNU_EH_FRAME) {
            mod.ehHdr = mod.base + ph[i].p_vaddr;
        }
    }
    std::printf("segments %zu exec_total_mb=%.1f\n", mod.segments.size(), [&] {
        double t = 0;
        for (auto &s : mod.segments) if (s.exec) t += static_cast<double>(s.end - s.begin);
        return t / 1048576.0;
    }());
    const char *needles[] = {
        "OverworldGeneratorMultinoise::_prepareHeightsForCells(",
        "OverworldGeneratorMultinoise::prepareHeightsForColumn(",
        "OverworldGenerator2d::_prepareHeights(",
        "ChunkBlender::attenuateDensity(",
        "NetherGenerator::decorationPostProcessChunk(",
        "TheEndGenerator::decorationPostProcessChunk(",
        "TheEndGenerator::findSpawnPosition(",
        "BiomeSource3d::getBiomeArea(",
    };
    for (const char *n : needles) {
        auto t0 = std::chrono::steady_clock::now();
        owg::Located loc;
        bool ok = owg::locateFunction(mod, n, loc);
        double ms = std::chrono::duration<double, std::milli>(std::chrono::steady_clock::now() - t0).count();
        std::printf("%-58s ok=%d rva=0x%lx size=%zu occ=%d xrefs=%d funcs=%d %.0fms\n", n, ok ? 1 : 0, static_cast<unsigned long>(loc.addr - mod.base), loc.size, loc.occurrences, loc.xrefs, loc.functions, ms);
    }
    return 0;
}
