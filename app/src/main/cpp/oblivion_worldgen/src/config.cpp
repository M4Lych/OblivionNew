#include "oblivion/worldgen.h"
#include "noise.h"

#include <cstdlib>
#include <string>

namespace owg {

namespace {

bool asBool(const std::string &v) { return v == "1" || v == "true" || v == "on"; }

}

void clampConfig(Config &c) {
    c.maxY = static_cast<int>(clampd(c.maxY, 128, 4000));
    c.minY = static_cast<int>(clampd(c.minY, -512, 0));
    c.seaLevel = static_cast<int>(clampd(c.seaLevel, c.minY + 16, c.maxY - 32));
    c.netherHeight = static_cast<int>(clampd(c.netherHeight, 64, 1024));
    c.riverWidth = clampd(c.riverWidth, 0.2, 6.0);
    c.smoothness = clampd(c.smoothness, 0.0, 1.0);
    c.mountainScale = clampd(c.mountainScale, 0.0, 1.5);
    c.caveScale = clampd(c.caveScale, 0.0, 3.0);
    c.oceanDepth = clampd(c.oceanDepth, 0.3, 3.0);
}

bool parseConfig(const std::string &text, Config &out) {
    size_t i = 0;
    bool any = false;
    while (i < text.size()) {
        size_t end = text.find_first_of(";\n", i);
        if (end == std::string::npos) end = text.size();
        std::string pair = text.substr(i, end - i);
        i = end + 1;
        size_t eq = pair.find('=');
        if (eq == std::string::npos) continue;
        std::string k = pair.substr(0, eq), v = pair.substr(eq + 1);
        while (!k.empty() && (k.back() == ' ' || k.back() == '\r')) k.pop_back();
        while (!k.empty() && k.front() == ' ') k.erase(k.begin());
        while (!v.empty() && (v.back() == ' ' || v.back() == '\r')) v.pop_back();
        while (!v.empty() && v.front() == ' ') v.erase(v.begin());
        if (k == "seed") out.seed = static_cast<uint64_t>(std::strtoull(v.c_str(), nullptr, 10));
        else if (k == "minY") out.minY = std::atoi(v.c_str());
        else if (k == "maxY") out.maxY = std::atoi(v.c_str());
        else if (k == "seaLevel") out.seaLevel = std::atoi(v.c_str());
        else if (k == "netherHeight") out.netherHeight = std::atoi(v.c_str());
        else if (k == "riverWidth") out.riverWidth = std::atof(v.c_str());
        else if (k == "smoothness") out.smoothness = std::atof(v.c_str());
        else if (k == "mountainScale") out.mountainScale = std::atof(v.c_str());
        else if (k == "caveScale") out.caveScale = std::atof(v.c_str());
        else if (k == "oceanDepth") out.oceanDepth = std::atof(v.c_str());
        else if (k == "overworld") out.overworld = asBool(v);
        else if (k == "nether") out.nether = asBool(v);
        else if (k == "end") out.end = asBool(v);
        else continue;
        any = true;
    }
    clampConfig(out);
    return any;
}

const char *version() { return "oblivion-worldgen 0.1.0"; }

}
