#include "oblivion/worldgen.h"
#include "noise.h"

#include <algorithm>
#include <cmath>

namespace owg {

struct Overworld::Impl {
    Fbm warpX, warpZ, continental, erosion, peaks, mountain, hills, detail, river, riverWarp;
    Fbm cheese, tubeA, tubeB, tubeC, entrance, shapeBlend;

    explicit Impl(uint64_t seed)
        : warpX(seed ^ 0x1111, 2), warpZ(seed ^ 0x2222, 2),
          continental(seed ^ 0x3333, 5), erosion(seed ^ 0x4444, 4), peaks(seed ^ 0x5555, 4),
          mountain(seed ^ 0x6666, 3), hills(seed ^ 0x7777, 4), detail(seed ^ 0x8888, 3),
          river(seed ^ 0x9999, 3), riverWarp(seed ^ 0xAAAA, 2),
          cheese(seed ^ 0xBBBB, 3), tubeA(seed ^ 0xCCCC, 2), tubeB(seed ^ 0xDDDD, 2),
          tubeC(seed ^ 0xEEEE, 2), entrance(seed ^ 0xFFFF, 2), shapeBlend(seed ^ 0x1234, 2) {}
};

namespace {

double oceanFloor(double c, const Config &cfg) {
    double shelf = smoothstep(-0.10, -0.32, c);
    double deep = smoothstep(-0.32, -0.80, c);
    double depth = (6.0 + 22.0 * shelf + 34.0 * deep) * cfg.oceanDepth;
    return cfg.seaLevel - depth;
}

double landBase(double c, const Config &cfg) {
    double coast = smoothstep(-0.10, 0.04, c);
    double inland = smoothstep(0.03, 0.42, c);
    double beach = cfg.seaLevel - 5.0 + 7.0 * coast;
    return beach + 20.0 * inland;
}

}

Overworld::Overworld(const Config &c) : impl_(new Impl(c.seed)), cfg_(c) { clampConfig(cfg_); }

Overworld::~Overworld() { delete impl_; }

const Config &Overworld::config() const { return cfg_; }

SurfaceSample Overworld::sample(double x, double z) const {
    const Config &cfg = cfg_;
    double wx = x + 110.0 * impl_->warpX.at2(x / 900.0, z / 900.0);
    double wz = z + 110.0 * impl_->warpZ.at2(x / 900.0 + 31.7, z / 900.0 - 17.3);

    double cont = clampd(impl_->continental.at2(wx / 3400.0, wz / 3400.0) * 1.35 + 0.16, -1.0, 1.0);
    double eros01 = 0.5 + 0.5 * impl_->erosion.at2(wx / 1700.0, wz / 1700.0);
    double ridge = 1.0 - std::fabs(impl_->peaks.at2(wx / 1250.0, wz / 1250.0));
    double mount01 = 0.5 + 0.5 * impl_->mountain.at2(wx / 2600.0, wz / 2600.0);

    double landMask = smoothstep(-0.06, 0.10, cont);
    double base = lerp(oceanFloor(cont, cfg), landBase(cont, cfg), landMask);

    int hillOctaves = cfg.smoothness > 0.85 ? 2 : (cfg.smoothness > 0.55 ? 3 : 4);
    double hillAmp = (5.0 + 30.0 * (1.0 - eros01)) * smoothstep(-0.02, 0.30, cont);
    double hillNoise = impl_->hills.at2(wx / 520.0, wz / 520.0, hillOctaves);
    double hills = hillAmp * (0.5 + 0.5 * hillNoise);

    double detailAmp = 2.2 * (1.0 - cfg.smoothness) * smoothstep(0.0, 0.2, cont);
    double detail = detailAmp * impl_->detail.at2(wx / 70.0, wz / 70.0, 2);

    double range = std::max(40.0, static_cast<double>(cfg.maxY - cfg.seaLevel) - 30.0);
    double inlandGate = smoothstep(0.12, 0.62, cont);
    double massif = std::pow(smoothstep(0.38, 0.92, mount01), 1.6);
    double drive = massif * inlandGate * (0.30 + 0.70 * (1.0 - eros01));
    double peakShape = 0.55 + 0.45 * ridge;
    double peaks = range * cfg.mountainScale * drive * peakShape;

    double height = base + hills * (1.0 - 0.6 * drive) + detail + peaks;

    double rw = wx + 140.0 * impl_->riverWarp.at2(x / 1100.0 + 7.1, z / 1100.0 - 3.3);
    double rz = wz + 140.0 * impl_->riverWarp.at2(x / 1100.0 - 9.2, z / 1100.0 + 5.4);
    double r = std::fabs(impl_->river.at2(rw / 1500.0 + 12.0, rz / 1500.0 - 8.0, 2));
    double w = 0.0095 * cfg.riverWidth;
    double channel = 1.0 - smoothstep(w * 0.35, w, r);
    double valley = 1.0 - smoothstep(w * 0.8, w * 4.2, r);

    double lowland = 1.0 - smoothstep(cfg.seaLevel + 22.0, cfg.seaLevel + 85.0, height);
    double riverGate = smoothstep(0.00, 0.14, cont) * (1.0 - smoothstep(0.25, 0.75, drive)) * lowland;
    channel *= riverGate;
    valley *= riverGate;

    double bed = cfg.seaLevel - 4.0 - 3.0 * channel;
    double banks = lerp(height, std::min(height, cfg.seaLevel + 3.0), valley * 0.55);
    double carved = lerp(banks, std::min(banks, bed), channel);
    height = carved;

    height = clampd(height, cfg.minY + 8.0, cfg.maxY - 4.0);

    SurfaceSample s;
    s.height = height;
    s.continental = cont;
    s.erosion = eros01;
    s.mountain = drive;
    s.river = channel;
    s.valley = valley;
    return s;
}

double Overworld::surfaceHeight(double x, double z) const { return sample(x, z).height; }

double Overworld::caveCarve(double x, double y, double z, double surface) const {
    const Config &cfg = cfg_;
    if (cfg.caveScale <= 0.0) return 0.0;
    double depth = surface - y;
    if (depth < 4.0) return 0.0;

    double fadeTop = smoothstep(4.0, 22.0, depth);
    double entrance = 0.5 + 0.5 * impl_->entrance.at2(x / 140.0, z / 140.0);
    double mouth = smoothstep(0.78, 0.92, entrance) * (1.0 - smoothstep(4.0, 14.0, depth));
    double top = std::max(fadeTop, mouth * 0.9);

    double fadeBottom = smoothstep(cfg.minY + 2.0, cfg.minY + 14.0, y);

    double yScale = 60.0;
    double big = impl_->cheese.at3(x / 120.0, y / yScale, z / 120.0);
    double cavern = smoothstep(0.26, 0.44, big) * smoothstep(8.0, 40.0, depth);

    double a = impl_->tubeA.at3(x / 62.0, y / 44.0, z / 62.0);
    double b = impl_->tubeB.at3(x / 62.0, y / 44.0, z / 62.0);
    double radiusMod = 0.5 + 0.5 * impl_->tubeC.at3(x / 210.0, y / 120.0, z / 210.0);
    double radius = (0.10 + 0.10 * radiusMod) * cfg.caveScale;
    double tubeDist = std::sqrt(a * a + b * b);
    double tube = 1.0 - smoothstep(radius * 0.55, radius, tubeDist);

    double carve = std::max(cavern, tube);
    return clamp01(carve * top * fadeBottom);
}

double Overworld::density(double x, double y, double z) const {
    SurfaceSample s = sample(x, z);
    double d = clampd((s.height - y) / 10.0, -1.0, 1.0);
    if (d > -0.9) {
        double carve = caveCarve(x, y, z, s.height);
        d -= carve * 2.4;
    }
    return d;
}

}
