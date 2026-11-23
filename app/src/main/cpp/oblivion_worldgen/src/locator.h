#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace owg {

struct Segment {
    uintptr_t begin;
    uintptr_t end;
    bool exec;
};

struct ModuleView {
    std::vector<Segment> segments;
    uintptr_t ehHdr = 0;
    uintptr_t base = 0;
    std::string name;
};

struct Located {
    uintptr_t addr = 0;
    size_t size = 0;
    int occurrences = 0;
    int xrefs = 0;
    int functions = 0;
};

bool locateFunction(const ModuleView &mod, const char *needle, Located &out);

}
