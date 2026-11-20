#include "oblivion/worldgen.h"
#include "noise.h"

#include <cmath>

namespace owg {

struct Nether::Impl {
    Fbm body, warp, pillars, floorShape;
    explicit Impl(uint64_t seed)
        : body(seed ^ 0xA1A1, 4), warp(seed ^ 0xB2B2, 2), pillars(seed ^ 0xC3C3, 2),
          floorShape(seed ^ 0xD4D4, 3) {}
};

Nether::Nether(const Config &c) : impl_(new Impl(c.seed)), cfg_(c) { clampConfig(cfg_); }
Nether::~Nether() { delete impl_; }

double Nether::density(double x, double y, double z) const {
    double h = static_cast<double>(cfg_.netherHeight);
    double wx = x + 40.0 * impl_->warp.at3(x / 180.0, y / 120.0, z / 180.0);
    double wz = z + 40.0 * impl_->warp.at3(x / 180.0 + 50.0, y / 120.0, z / 180.0 - 50.0);
    double n = impl_->body.at3(wx / 110.0, y / (h * 0.55), wz / 110.0);

    double t = y / h;
    double floorBias = 1.25 * (1.0 - smoothstep(0.03, 0.34, t));
    double ceilBias = 1.25 * smoothstep(0.66, 0.97, t);

    double floorWave = impl_->floorShape.at2(x / 160.0, z / 160.0);
    double floorLift = 0.18 * floorWave * smoothstep(0.0, 0.3, t) * (1.0 - smoothstep(0.3, 0.55, t));

    double pillar = impl_->pillars.at2(x / 55.0, z / 55.0);
    double pillarBias = 0.9 * smoothstep(0.55, 0.8, pillar) * smoothstep(0.1, 0.4, t) * (1.0 - smoothstep(0.7, 0.9, t));

    return n * 1.15 + floorBias + ceilBias + floorLift + pillarBias - 0.36;
}

struct End::Impl {
    Fbm islands, shape, rim;
    explicit Impl(uint64_t seed)
        : islands(seed ^ 0xE5E5, 3), shape(seed ^ 0xF6F6, 3), rim(seed ^ 0x0707, 2) {}
};

End::End(const Config &c) : impl_(new Impl(c.seed)), cfg_(c) { clampConfig(cfg_); }
End::~End() { delete impl_; }

double End::density(double x, double y, double z) const {
    double r = std::sqrt(x * x + z * z);
    double mainFall = 1.0 - smoothstep(55.0, 105.0, r);
    double top = 64.0 + 10.0 * mainFall;
    double mainBody = mainFall;

    double outer = 0.0;
    if (r > 700.0) {
        double ring = smoothstep(700.0, 1000.0, r);
        double field = impl_->islands.at2(x / 220.0, z / 220.0);
        double cell = smoothstep(0.22, 0.52, field);
        outer = ring * cell;
        double outerTop = 62.0 + 26.0 * impl_->shape.at2(x / 140.0, z / 140.0);
        top = mainBody > outer ? top : outerTop;
    }

    double mass = std::max(mainBody, outer);
    if (mass <= 0.001) return -1.0;

    double thickness = 14.0 + 40.0 * mass;
    double belly = 0.5 + 0.5 * impl_->rim.at2(x / 90.0, z / 90.0);
    double bottom = top - thickness * (0.65 + 0.35 * belly);

    double above = (top - y) / 4.0;
    double below = (y - bottom) / 8.0;
    return clampd(std::min(above, below) * mass * 1.6, -1.0, 1.0);
}

}
