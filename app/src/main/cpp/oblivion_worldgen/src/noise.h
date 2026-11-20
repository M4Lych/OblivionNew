#pragma once

#include <cmath>
#include <cstdint>

namespace owg {

inline double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
inline double clampd(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
inline double lerp(double a, double b, double t) { return a + (b - a) * t; }
inline double smoothstep(double e0, double e1, double x) {
    double t = clamp01((x - e0) / (e1 - e0));
    return t * t * (3.0 - 2.0 * t);
}

inline uint64_t splitmix(uint64_t &s) {
    s += 0x9E3779B97F4A7C15ULL;
    uint64_t z = s;
    z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9ULL;
    z = (z ^ (z >> 27)) * 0x94D049BB133111EBULL;
    return z ^ (z >> 31);
}

class Perlin {
public:
    explicit Perlin(uint64_t seed) {
        uint64_t s = seed;
        for (int i = 0; i < 256; i++) p_[i] = static_cast<uint8_t>(i);
        for (int i = 255; i > 0; i--) {
            int j = static_cast<int>(splitmix(s) % static_cast<uint64_t>(i + 1));
            uint8_t t = p_[i];
            p_[i] = p_[j];
            p_[j] = t;
        }
        for (int i = 0; i < 256; i++) p_[256 + i] = p_[i];
        ox_ = static_cast<double>(splitmix(s) % 100000) + 0.31;
        oy_ = static_cast<double>(splitmix(s) % 100000) + 0.57;
        oz_ = static_cast<double>(splitmix(s) % 100000) + 0.83;
    }

    double noise2(double x, double z) const { return noise3(x, 0.0, z) * 1.15; }

    double noise3(double x, double y, double z) const {
        x += ox_;
        y += oy_;
        z += oz_;
        double fx = std::floor(x), fy = std::floor(y), fz = std::floor(z);
        int X = static_cast<int>(static_cast<int64_t>(fx) & 255);
        int Y = static_cast<int>(static_cast<int64_t>(fy) & 255);
        int Z = static_cast<int>(static_cast<int64_t>(fz) & 255);
        x -= fx;
        y -= fy;
        z -= fz;
        double u = fade(x), v = fade(y), w = fade(z);
        int A = p_[X] + Y, AA = p_[A] + Z, AB = p_[A + 1] + Z;
        int B = p_[X + 1] + Y, BA = p_[B] + Z, BB = p_[B + 1] + Z;
        return lerp(
            lerp(lerp(grad(p_[AA], x, y, z), grad(p_[BA], x - 1, y, z), u),
                 lerp(grad(p_[AB], x, y - 1, z), grad(p_[BB], x - 1, y - 1, z), u), v),
            lerp(lerp(grad(p_[AA + 1], x, y, z - 1), grad(p_[BA + 1], x - 1, y, z - 1), u),
                 lerp(grad(p_[AB + 1], x, y - 1, z - 1), grad(p_[BB + 1], x - 1, y - 1, z - 1), u), v),
            w);
    }

private:
    static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    static double grad(int h, double x, double y, double z) {
        int g = h & 15;
        double u = g < 8 ? x : y;
        double v = g < 4 ? y : (g == 12 || g == 14 ? x : z);
        return ((g & 1) ? -u : u) + ((g & 2) ? -v : v);
    }

    uint8_t p_[512];
    double ox_, oy_, oz_;
};

class Fbm {
public:
    Fbm(uint64_t seed, int octaves, double lacunarity = 2.0, double gain = 0.5)
        : octaves_(octaves > kMax ? kMax : octaves), lac_(lacunarity), gain_(gain) {
        uint64_t s = seed;
        for (int i = 0; i < octaves_; i++) layers_[i] = new Perlin(splitmix(s));
        double a = 1.0, sum = 0.0;
        for (int i = 0; i < octaves_; i++) {
            sum += a;
            a *= gain_;
        }
        norm_ = 1.0 / sum;
    }
    ~Fbm() {
        for (int i = 0; i < octaves_; i++) delete layers_[i];
    }
    Fbm(const Fbm &) = delete;
    Fbm &operator=(const Fbm &) = delete;

    double at2(double x, double z, int maxOctaves = 99) const {
        double a = 1.0, f = 1.0, sum = 0.0, norm = 0.0;
        for (int i = 0; i < octaves_ && i < maxOctaves; i++) {
            sum += layers_[i]->noise2(x * f, z * f) * a;
            norm += a;
            a *= gain_;
            f *= lac_;
        }
        return clampd(sum / norm * 1.35, -1.0, 1.0);
    }

    double at3(double x, double y, double z) const {
        double a = 1.0, f = 1.0, sum = 0.0;
        for (int i = 0; i < octaves_; i++) {
            sum += layers_[i]->noise3(x * f, y * f, z * f) * a;
            a *= gain_;
            f *= lac_;
        }
        return clampd(sum * norm_ * 1.35, -1.0, 1.0);
    }

private:
    static constexpr int kMax = 8;
    int octaves_;
    double lac_, gain_, norm_;
    Perlin *layers_[kMax] = {};
};

}
