#include <jni.h>
#include <string>
#include <fstream>
#include <cstdio>
#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cstdlib>
#include <cstring>

#define TAG "RDL_Native"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static bool fileExists(const char* path) {
    struct stat st{};
    return stat(path, &st) == 0;
}

static const char* ROOT_PATHS[] = {
        "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/system/xbin/busybox", "/system/sbin/su", "/vendor/bin/su",
        "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
        "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
        "/system/xbin/daemonsu", "/system/etc/init.d/99SuperSUDaemon",
        "/system/bin/.ext/.su", "/system_ext/bin/su", "/system_ext/xbin/su",
        "/system_ext/sbin/su", "/apex/com.android.runtime/bin/su",
        "/apex/com.android.art/bin/su", nullptr
};

static const char* MAGISK_PATHS[] = {
        "/data/adb/magisk", "/data/adb/magisk.db", "/data/adb/magisk.img",
        "/data/adb/modules", "/data/adb/post-fs-data.d", "/data/adb/service.d",
        "/sbin/.magisk", "/sbin/.core/mirror", "/sbin/.core/img", nullptr
};

static const char* ROOT_PACKAGES[] = {
        "com.topjohnwu.magisk", "com.topjohnwu.magisk.stub",
        "com.topjohnwu.magisk.alpha", "eu.chainfire.supersu",
        "com.noshufou.android.su", "com.koushikdutta.superuser",
        "com.yellowes.su", "com.kingroot.kinguser", "com.kingo.root",
        "me.phh.superuser", nullptr
};

static bool checkPackagesXml() {
    std::ifstream f("/data/system/packages.xml");
    if (!f.is_open()) return false;
    std::string line;
    while (std::getline(f, line)) {
        for (int i = 0; ROOT_PACKAGES[i]; i++) {
            if (line.find(ROOT_PACKAGES[i]) != std::string::npos) {
                LOGW("[NATIVE][ROOT][PKG] %s", ROOT_PACKAGES[i]);
                return true;
            }
        }
    }
    return false;
}

static bool checkWhichSu() {
    FILE* fp = popen("which su", "r");
    if (!fp) return false;
    char buf[256] = {};
    bool found = (fgets(buf, sizeof(buf), fp) != nullptr && buf[0] != '\0');
    pclose(fp);
    if (found) LOGW("[NATIVE][ROOT][EXEC] which su = %s", buf);
    return found;
}

bool nativeCheckRooted() {
    for (int i = 0; ROOT_PATHS[i]; i++) {
        if (fileExists(ROOT_PATHS[i])) {
            LOGW("[NATIVE][ROOT][PATH] %s", ROOT_PATHS[i]);
            return true;
        }
    }
    for (int i = 0; MAGISK_PATHS[i]; i++) {
        if (fileExists(MAGISK_PATHS[i])) {
            LOGW("[NATIVE][ROOT][MAGISK] %s", MAGISK_PATHS[i]);
            return true;
        }
    }
    if (checkPackagesXml()) return true;
    if (checkWhichSu())     return true;
    return false;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_raconf_lab04_NativeRootDetector_isRooted(JNIEnv*, jclass) {
    return nativeCheckRooted() ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_raconf_lab04_NativeRootDetector_crashAbort(JNIEnv*, jclass) {
    LOGE("[NATIVE] CRASH #2 abort()");
    abort();
}