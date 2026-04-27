#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <android/log.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <cstring>
#include <cstdio>

#define TAG "RDL_Frida"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define MAX_LINE 512

// ── 1. PATH-BASED (/data/local/tmp) ───────────────────────
static bool checkFridaFiles() {
    std::vector<std::string> fridaFiles = {
            "frida-server",
            "frida-server-arm64",
            "frida-server-x86_64",
            "frida-agent",
            "frida-inject",
            "frida-gadget",
            "frida",
            "re.frida.server",
    };
    for (const auto& fileName : fridaFiles) {
        std::string filePath = "/data/local/tmp/" + fileName;
        FILE* fd = fopen(filePath.c_str(), "r");
        if (fd != nullptr) {
            LOGW("[NATIVE][FRIDA][PATH] found: %s", filePath.c_str());
            fclose(fd);
            return true;
        }
    }
    DIR* dir = opendir("/data/local/tmp");
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != nullptr) {
            if (strncmp(entry->d_name, "frida", 5) == 0) {
                LOGW("[NATIVE][FRIDA][PATH] /data/local/tmp/%s", entry->d_name);
                closedir(dir);
                return true;
            }
        }
        closedir(dir);
    }
    return false;
}

// ── 2. PORT-BASED ─────────────────────────────────────────
// 27042 decimal = 0x69A2 hex
static bool checkFridaPort() {
    const char* files[] = {
            "/proc/net/tcp",
            "/proc/net/tcp6",
            nullptr
    };

    // Önce direkt okuma dene
    for (int f = 0; files[f]; f++) {
        FILE* file = fopen(files[f], "r");
        if (!file) continue;
        char line[MAX_LINE];
        while (fgets(line, sizeof(line), file)) {
            if (strstr(line, ":69A2") || strstr(line, ":69a2")) {
                LOGW("[NATIVE][FRIDA][PORT] port 27042 found in %s", files[f]);
                fclose(file);
                return true;
            }
        }
        fclose(file);
    }
    const char* suCmds[] = {
            "su -c 'cat /proc/net/tcp /proc/net/tcp6 2>/dev/null'",
            "su 0 cat /proc/net/tcp",
            nullptr
    };
    for (int i = 0; suCmds[i]; i++) {
        FILE* fp = popen(suCmds[i], "r");
        if (!fp) continue;
        char line[MAX_LINE];
        bool found = false;
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, ":69A2") || strstr(line, ":69a2")) {
                LOGW("[NATIVE][FRIDA][PORT] port 27042 found via su");
                found = true;
                break;
            }
        }
        pclose(fp);
        if (found) return true;
    }
    return false;
}

// ── 3. MAPS-BASED (/proc/self/maps) ───────────────────────
static bool checkFridaMaps() {
    FILE* mapsFile = fopen("/proc/self/maps", "r");
    if (!mapsFile) {
        LOGE("[NATIVE][FRIDA][MAPS] cannot open /proc/self/maps");
        return false;
    }
    char buffer[MAX_LINE];
    while (fgets(buffer, sizeof(buffer), mapsFile)) {
        std::string line(buffer);
        if (line.find("frida")        != std::string::npos ||
            line.find("gum-js-loop")  != std::string::npos ||
            line.find("frida-agent")  != std::string::npos ||
            line.find("frida-gadget") != std::string::npos) {
            LOGW("[NATIVE][FRIDA][MAPS] found: %s", buffer);
            fclose(mapsFile);
            return true;
        }
    }
    fclose(mapsFile);
    return false;
}

// ── JNI: isFridaPresent ───────────────────────────────────
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_raconf_lab02_NativeFridaDetector_isFridaPresent(JNIEnv*, jclass) {
    if (checkFridaFiles()) return JNI_TRUE;
    if (checkFridaPort())  return JNI_TRUE;
    if (checkFridaMaps())  return JNI_TRUE;
    return JNI_FALSE;
}