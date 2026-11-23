#pragma once

#include <cstdint>
#include <string>

namespace owg {

struct Config {
    uint64_t seed = 0;
    int minY = -64;
    int maxY = 320;
    int seaLevel = 63;
    int netherHeight = 128;
    double riverWidth = 1.0;
    double smoothness = 0.75;
    double mountainScale = 1.0;
    double caveScale = 1.0;
    double oceanDepth = 1.0;
    bool overworld = true;
    bool nether = true;
    bool end = true;
};

struct SurfaceSample {
    double height;
    double continental;
    double erosion;
    double mountain;
    double river;
    double valley;
};

bool parseConfig(const std::string &text, Config &out);
void clampConfig(Config &c);

class Overworld {
public:
    explicit Overworld(const Config &c);
    ~Overworld();
    Overworld(const Overworld &) = delete;
    Overworld &operator=(const Overworld &) = delete;
    SurfaceSample sample(double x, double z) const;
    double surfaceHeight(double x, double z) const;
    double caveCarve(double x, double y, double z, double surface) const;
    double density(double x, double y, double z) const;
    const Config &config() const;

private:
    struct Impl;
    Impl *impl_;
    Config cfg_;
};

class Nether {
public:
    explicit Nether(const Config &c);
    ~Nether();
    Nether(const Nether &) = delete;
    Nether &operator=(const Nether &) = delete;
    double density(double x, double y, double z) const;

private:
    struct Impl;
    Impl *impl_;
    Config cfg_;
};

class End {
public:
    explicit End(const Config &c);
    ~End();
    End(const End &) = delete;
    End &operator=(const End &) = delete;
    double density(double x, double y, double z) const;

private:
    struct Impl;
    Impl *impl_;
    Config cfg_;
};

const char *version();

}
