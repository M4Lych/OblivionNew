#include <jni.h>

#include <memory>
#include <mutex>
#include <string>

#include "oblivion/worldgen.h"

#ifdef __ANDROID__
#include "probe.h"
#endif

#define OWG_API __attribute__((visibility("default")))

namespace {

std::mutex g_lock;
std::unique_ptr<owg::Overworld> g_overworld;
std::unique_ptr<owg::Nether> g_nether;
std::unique_ptr<owg::End> g_end;
owg::Config g_config;

}

extern "C" {

OWG_API int owg_configure(const char *text) {
    owg::Config c;
    if (!owg::parseConfig(text ? text : "", c)) return 0;
    std::lock_guard<std::mutex> guard(g_lock);
    g_config = c;
    g_overworld.reset(c.overworld ? new owg::Overworld(c) : nullptr);
    g_nether.reset(c.nether ? new owg::Nether(c) : nullptr);
    g_end.reset(c.end ? new owg::End(c) : nullptr);
    return 1;
}

OWG_API double owg_surface_height(double x, double z) {
    std::lock_guard<std::mutex> guard(g_lock);
    return g_overworld ? g_overworld->surfaceHeight(x, z) : -1.0e9;
}

OWG_API double owg_overworld_density(double x, double y, double z) {
    std::lock_guard<std::mutex> guard(g_lock);
    return g_overworld ? g_overworld->density(x, y, z) : -1.0;
}

OWG_API double owg_nether_density(double x, double y, double z) {
    std::lock_guard<std::mutex> guard(g_lock);
    return g_nether ? g_nether->density(x, y, z) : -1.0;
}

OWG_API double owg_end_density(double x, double y, double z) {
    std::lock_guard<std::mutex> guard(g_lock);
    return g_end ? g_end->density(x, y, z) : -1.0;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_worldgen_OblivionWorldGen_nativeConfigure(JNIEnv *env, jclass, jstring text) {
    if (!text) return JNI_FALSE;
    const char *chars = env->GetStringUTFChars(text, nullptr);
    if (!chars) return JNI_FALSE;
    int ok = owg_configure(chars);
    env->ReleaseStringUTFChars(text, chars);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_org_levimc_launcher_core_worldgen_OblivionWorldGen_nativeVersion(JNIEnv *env, jclass) {
    return env->NewStringUTF(owg::version());
}

JNIEXPORT jdouble JNICALL
Java_org_levimc_launcher_core_worldgen_OblivionWorldGen_nativeSurfaceHeight(JNIEnv *, jclass, jdouble x, jdouble z) {
    return owg_surface_height(x, z);
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_worldgen_OblivionWorldGen_nativeProbeStart(JNIEnv *, jclass) {
#ifdef __ANDROID__
    return owg::probeStart() ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jstring JNICALL
Java_org_levimc_launcher_core_worldgen_OblivionWorldGen_nativeProbeStatus(JNIEnv *env, jclass) {
#ifdef __ANDROID__
    return env->NewStringUTF(owg::probeStatus().c_str());
#else
    return env->NewStringUTF("unsupported platform");
#endif
}

}
