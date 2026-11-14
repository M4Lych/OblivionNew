#define _GNU_SOURCE
#include <android/log.h>
#include <android/native_activity.h>
#include <dlfcn.h>
#include <jni.h>
#include <limits.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define TAG "OblivionNativeHost"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

typedef void (*native_activity_on_create_fn)(ANativeActivity *, void *, size_t);

static pthread_mutex_t g_path_lock = PTHREAD_MUTEX_INITIALIZER;
static char g_real_library_path[PATH_MAX];
static void *g_real_library_handle;

__attribute__((visibility("default"))) JNIEXPORT void JNICALL
Java_com_oblivion_client_hosted_OblivionNativeBridge_nativeSetRealLibraryPath(
        JNIEnv *env, jclass clazz, jstring path) {
    (void) clazz;
    if (path == NULL) return;

    const char *utf = (*env)->GetStringUTFChars(env, path, NULL);
    if (utf == NULL) return;

    pthread_mutex_lock(&g_path_lock);
    size_t length = strnlen(utf, sizeof(g_real_library_path) - 1);
    memcpy(g_real_library_path, utf, length);
    g_real_library_path[length] = '\0';
    pthread_mutex_unlock(&g_path_lock);

    (*env)->ReleaseStringUTFChars(env, path, utf);
    LOGI("Configured installed Minecraft native library path");
}

static int copy_real_path(char *out, size_t out_size) {
    pthread_mutex_lock(&g_path_lock);
    size_t length = strnlen(g_real_library_path, sizeof(g_real_library_path));
    if (length == 0 || length >= out_size) {
        pthread_mutex_unlock(&g_path_lock);
        return 0;
    }
    memcpy(out, g_real_library_path, length + 1);
    pthread_mutex_unlock(&g_path_lock);
    return 1;
}

__attribute__((visibility("default"))) void ANativeActivity_onCreate(
        ANativeActivity *activity,
        void *saved_state,
        size_t saved_state_size) {
    char path[PATH_MAX];
    if (!copy_real_path(path, sizeof(path))) {
        LOGE("Real Minecraft library path was not configured before native startup");
        ANativeActivity_finish(activity);
        return;
    }

    void *handle = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    if (handle == NULL) {
        const char *error = dlerror();
        LOGE("dlopen of installed Minecraft library failed: %s", error ? error : "unknown error");
        ANativeActivity_finish(activity);
        return;
    }

    dlerror();
    native_activity_on_create_fn entry =
            (native_activity_on_create_fn) dlsym(handle, "ANativeActivity_onCreate");
    const char *symbol_error = dlerror();
    if (entry == NULL || symbol_error != NULL || entry == &ANativeActivity_onCreate) {
        LOGE("Installed Minecraft ANativeActivity_onCreate export is unavailable: %s",
             symbol_error ? symbol_error : "invalid symbol address");
        dlclose(handle);
        ANativeActivity_finish(activity);
        return;
    }

    pthread_mutex_lock(&g_path_lock);
    g_real_library_handle = handle;
    pthread_mutex_unlock(&g_path_lock);

    LOGI("Forwarding native Activity startup to the installed Minecraft library");
    entry(activity, saved_state, saved_state_size);
}
