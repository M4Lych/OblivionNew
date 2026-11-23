#include "oblivion/worldgen.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <vector>

namespace {

struct Img {
    int w, h;
    std::vector<unsigned char> px;
    Img(int w_, int h_) : w(w_), h(h_), px(static_cast<size_t>(w_) * h_ * 3, 0) {}
    void set(int x, int y, int r, int g, int b) {
        size_t i = (static_cast<size_t>(y) * w + x) * 3;
        px[i] = static_cast<unsigned char>(std::max(0, std::min(255, r)));
        px[i + 1] = static_cast<unsigned char>(std::max(0, std::min(255, g)));
        px[i + 2] = static_cast<unsigned char>(std::max(0, std::min(255, b)));
    }
    void save(const std::string &path) const {
        FILE *f = std::fopen(path.c_str(), "wb");
        if (!f) return;
        std::fprintf(f, "P6\n%d %d\n255\n", w, h);
        std::fwrite(px.data(), 1, px.size(), f);
        std::fclose(f);
    }
};

void mapView(const owg::Overworld &ow, const std::string &path, double span, int size, double cx, double cz) {
    const owg::Config &cfg = ow.config();
    std::vector<double> hgt(static_cast<size_t>(size) * size);
    std::vector<double> riv(static_cast<size_t>(size) * size);
    double step = span / size;
    for (int j = 0; j < size; j++)
        for (int i = 0; i < size; i++) {
            owg::SurfaceSample s = ow.sample(cx - span / 2 + i * step, cz - span / 2 + j * step);
            hgt[static_cast<size_t>(j) * size + i] = s.height;
            riv[static_cast<size_t>(j) * size + i] = s.river;
        }
    Img img(size, size);
    for (int j = 0; j < size; j++)
        for (int i = 0; i < size; i++) {
            size_t k = static_cast<size_t>(j) * size + i;
            double h = hgt[k];
            double hl = hgt[static_cast<size_t>(j) * size + std::max(0, i - 1)];
            double hu = hgt[static_cast<size_t>(std::max(0, j - 1)) * size + i];
            double shade = 1.0 + std::max(-0.45, std::min(0.45, ((hl - h) + (hu - h)) * 0.35 / step));
            int r, g, b;
            if (h < cfg.seaLevel) {
                double d = std::min(1.0, (cfg.seaLevel - h) / 70.0);
                r = static_cast<int>(40 - 30 * d);
                g = static_cast<int>(110 - 60 * d);
                b = static_cast<int>(190 - 70 * d);
            } else {
                double e = h - cfg.seaLevel;
                if (e < 2.5) {
                    r = 214; g = 200; b = 140;
                } else if (e < 140.0) {
                    double u = e / 140.0;
                    r = static_cast<int>(70 + 60 * u); g = static_cast<int>(150 - 20 * u); b = static_cast<int>(60 + 20 * u);
                } else if (e < 380.0) {
                    double u = (e - 140.0) / 240.0;
                    r = static_cast<int>(130 + 20 * u); g = static_cast<int>(130 + 5 * u); b = static_cast<int>(80 + 60 * u);
                } else {
                    double u = std::min(1.0, (e - 380.0) / 300.0);
                    r = static_cast<int>(150 + 100 * u); g = static_cast<int>(140 + 110 * u); b = static_cast<int>(140 + 110 * u);
                }
                r = static_cast<int>(r * shade); g = static_cast<int>(g * shade); b = static_cast<int>(b * shade);
                if (riv[k] > 0.15) {
                    double a = std::min(1.0, riv[k]);
                    r = static_cast<int>(r * (1 - a) + 40 * a); g = static_cast<int>(g * (1 - a) + 110 * a); b = static_cast<int>(b * (1 - a) + 190 * a);
                }
            }
            img.set(i, j, r, g, b);
        }
    img.save(path);
}

void crossSection(const owg::Overworld &ow, const std::string &path, double x0, double z, int width, int y0, int y1) {
    int height = y1 - y0;
    Img img(width, height);
    for (int i = 0; i < width; i++) {
        double x = x0 + i;
        owg::SurfaceSample s = ow.sample(x, z);
        for (int j = 0; j < height; j++) {
            double y = y1 - 1 - j;
            double d = ow.density(x, y, z);
            if (d > 0) {
                double depth = std::min(1.0, (s.height - y) / 60.0);
                img.set(i, j, static_cast<int>(120 - 60 * depth), static_cast<int>(105 - 55 * depth), static_cast<int>(90 - 45 * depth));
            } else if (y <= s.height) {
                img.set(i, j, 14, 12, 18);
            } else if (y <= ow.config().seaLevel) {
                img.set(i, j, 40, 110, 190);
            } else {
                img.set(i, j, 150, 190, 235);
            }
        }
    }
    img.save(path);
}

void netherSlice(const owg::Nether &n, const std::string &path, int height) {
    int w = 512, h = height;
    Img img(w, h);
    for (int i = 0; i < w; i++)
        for (int j = 0; j < h; j++) {
            double y = h - 1 - j;
            double d = n.density(i * 2.0, y, 300.0);
            if (d > 0) img.set(i, j, 110, 45, 45);
            else if (y <= 31) img.set(i, j, 230, 110, 20);
            else img.set(i, j, 30, 12, 12);
        }
    img.save(path);
}

void endTop(const owg::End &e, const std::string &path) {
    int size = 768;
    Img img(size, size);
    double span = 4200.0;
    for (int j = 0; j < size; j++)
        for (int i = 0; i < size; i++) {
            double x = -span / 2 + i * span / size, z = -span / 2 + j * span / size;
            bool solid = false;
            for (int y = 100; y >= 20; y -= 2)
                if (e.density(x, y, z) > 0) { solid = true; break; }
            if (solid) img.set(i, j, 225, 225, 160);
            else img.set(i, j, 12, 6, 24);
        }
    img.save(path);
}

void stats(const owg::Overworld &ow, double span, int n) {
    double step = span / n;
    std::vector<double> h(static_cast<size_t>(n) * n);
    for (int j = 0; j < n; j++)
        for (int i = 0; i < n; i++)
            h[static_cast<size_t>(j) * n + i] = ow.surfaceHeight(i * step, j * step);
    double maxH = -1e9, sumSlope = 0, sumCurv = 0;
    int land = 0, ocean = 0, steep = 0, count = 0;
    std::vector<double> slopes;
    for (int j = 1; j < n - 1; j++)
        for (int i = 1; i < n - 1; i++) {
            double c = h[static_cast<size_t>(j) * n + i];
            maxH = std::max(maxH, c);
            double dx = (h[static_cast<size_t>(j) * n + i + 1] - h[static_cast<size_t>(j) * n + i - 1]) / (2 * step);
            double dz = (h[static_cast<size_t>(j + 1) * n + i] - h[static_cast<size_t>(j - 1) * n + i]) / (2 * step);
            double slope = std::sqrt(dx * dx + dz * dz);
            double lap = (h[static_cast<size_t>(j) * n + i + 1] + h[static_cast<size_t>(j) * n + i - 1] + h[static_cast<size_t>(j + 1) * n + i] + h[static_cast<size_t>(j - 1) * n + i] - 4 * c) / (step * step);
            sumSlope += slope;
            sumCurv += std::fabs(lap);
            slopes.push_back(slope);
            if (slope > 1.5) steep++;
            if (c < ow.config().seaLevel) ocean++; else land++;
            count++;
        }
    std::sort(slopes.begin(), slopes.end());
    std::printf("region %.0f blocks, sample step %.1f\n", span, step);
    std::printf("max height %.1f  ocean %.1f%%  land %.1f%%\n", maxH, 100.0 * ocean / count, 100.0 * land / count);
    std::printf("mean slope %.3f  p50 %.3f  p95 %.3f  p99 %.3f  steep(>1.5) %.2f%%\n", sumSlope / count, slopes[slopes.size() / 2], slopes[static_cast<size_t>(slopes.size() * 0.95)], slopes[static_cast<size_t>(slopes.size() * 0.99)], 100.0 * steep / count);
    std::printf("mean |curvature| %.5f\n", sumCurv / count);

    int rows = 0, bandSum = 0, bands = 0;
    for (int j = 0; j < 40; j++) {
        double z = j * 97.0 + 5000.0;
        bool in = false;
        int w = 0;
        for (int i = 0; i < 20000; i++) {
            owg::SurfaceSample s = ow.sample(i * 1.0 - 8000.0, z);
            bool r = s.river > 0.5 && s.continental > 0.05;
            if (r) { in = true; w++; }
            else if (in) { bandSum += w; bands++; in = false; w = 0; }
        }
        rows++;
    }
    std::printf("river channels crossed %d, mean channel width %.1f blocks\n", bands, bands ? static_cast<double>(bandSum) / bands : 0.0);
}

}

int main(int argc, char **argv) {
    owg::Config c;
    c.seed = 424242;
    c.maxY = 1000;
    c.minY = -64;
    std::string out = argc > 1 ? argv[1] : ".";
    if (argc > 2) owg::parseConfig(argv[2], c);
    owg::Overworld ow(c);
    stats(ow, 8192.0, 1024);
    mapView(ow, out + "/map_wide.ppm", 12000.0, 900, 0, 0);
    double bx = 0, bz = 0, bh = -1e9, rx = 0, rz = 0;
    bool haveRiver = false;
    for (int j = -40; j <= 40; j++)
        for (int i = -40; i <= 40; i++) {
            double x = i * 150.0, z = j * 150.0;
            owg::SurfaceSample s = ow.sample(x, z);
            if (s.height > bh) { bh = s.height; bx = x; bz = z; }
            if (!haveRiver && s.river > 0.9 && s.continental > 0.25 && s.mountain < 0.1) { haveRiver = true; rx = x; rz = z; }
        }
    std::printf("highest sampled %.0f at (%.0f,%.0f); river sample at (%.0f,%.0f) found=%d\n", bh, bx, bz, rx, rz, haveRiver ? 1 : 0);
    mapView(ow, out + "/map_close.ppm", 3200.0, 900, rx, rz);
    mapView(ow, out + "/map_peak.ppm", 4000.0, 900, bx, bz);
    crossSection(ow, out + "/section_caves.ppm", rx - 500.0, rz, 1000, -64, 200);
    owg::Nether ne(c);
    netherSlice(ne, out + "/nether.ppm", c.netherHeight);
    owg::End en(c);
    endTop(en, out + "/end.ppm");
    return 0;
}
