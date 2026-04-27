#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdint>
#include <csignal>
#include <unistd.h>

#define TAG "RDL_CRC"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)

#define GUARD_BYTES 32

static void fallbackCrash() {
    LOGE("[CRC] TAMPER DETECTED — fallback crash via SIGKILL");
    raise(SIGKILL);
    _exit(1);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_raconf_lab04_NativeRootDetector_isRooted(JNIEnv*, jclass);

static void* getIsRootedAddr() {
    return reinterpret_cast<void*>(Java_com_raconf_lab04_NativeRootDetector_isRooted);
}

static void logBytes(const char* prefix, const uint8_t* b, size_t n) {
    if (n < 32) return;
    LOGI("%s %02X %02X %02X %02X %02X %02X %02X %02X "
         "%02X %02X %02X %02X %02X %02X %02X %02X "
         "%02X %02X %02X %02X %02X %02X %02X %02X "
         "%02X %02X %02X %02X %02X %02X %02X %02X",
         prefix,
         b[0],  b[1],  b[2],  b[3],  b[4],  b[5],  b[6],  b[7],
         b[8],  b[9],  b[10], b[11], b[12], b[13], b[14], b[15],
         b[16], b[17], b[18], b[19], b[20], b[21], b[22], b[23],
         b[24], b[25], b[26], b[27], b[28], b[29], b[30], b[31]);
}

// ── Mimari bazlı EXPECTED_BYTES ───────────────────────────
#if defined(__aarch64__)
static const uint8_t EXPECTED_BYTES[GUARD_BYTES] = {
        0xFD, 0x7B, 0xBF, 0xA9, 0xFD, 0x03, 0x00, 0x91,
        0x39, 0xE2, 0x01, 0x94, 0x00, 0x00, 0x00, 0x12,
        0xFD, 0x7B, 0xC1, 0xA8, 0xC0, 0x03, 0x5F, 0xD6,
        0xFD, 0x7B, 0xBF, 0xA9, 0xFD, 0x03, 0x00, 0x91
};
#elif defined(__x86_64__)
static const uint8_t EXPECTED_BYTES[GUARD_BYTES] = {
        0x50, 0xE8, 0x6A, 0x21, 0x07, 0x00, 0x59, 0xC3,
        0xCC, 0xCC, 0xCC, 0xCC, 0xCC, 0xCC, 0xCC, 0xCC,
        0x50, 0x48, 0x8D, 0x35, 0x68, 0x8E, 0xFD, 0xFF,
        0x48, 0x8D, 0x15, 0x6C, 0x8E, 0xFD, 0xFF, 0xBF
};
#else
static const uint8_t EXPECTED_BYTES[GUARD_BYTES] = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};
#endif

static bool isExpectedBytesConfigured() {
    for (size_t i = 0; i < GUARD_BYTES; i++) {
        if (EXPECTED_BYTES[i] != 0x00) return true;
    }
    return false;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_raconf_lab04_CrcGuard_init(JNIEnv*, jclass) {
    void* sym = getIsRootedAddr();
    if (!sym) { LOGE("[CRC] init: isRooted address is null"); return; }
    auto* b = reinterpret_cast<uint8_t*>(sym);
#if defined(__aarch64__) || defined(__arm__)
    __builtin___clear_cache(reinterpret_cast<char*>(sym),
                            reinterpret_cast<char*>(sym) + GUARD_BYTES);
#endif
    LOGI("[CRC] init: isRooted address = %p", sym);
    logBytes("[CRC] init: current bytes:", b, GUARD_BYTES);
    if (!isExpectedBytesConfigured()) {
        LOGW("[CRC] init: EXPECTED_BYTES is not configured");
        LOGW("[CRC] init: configure exact %d bytes from your release build", GUARD_BYTES);
        return;
    }
    logBytes("[CRC] init: expected bytes:", EXPECTED_BYTES, GUARD_BYTES);
    if (memcmp(EXPECTED_BYTES, sym, GUARD_BYTES) == 0)
        LOGI("[CRC] init: EXPECTED matches current bytes");
    else
        LOGE("[CRC] init: EXPECTED does NOT match current bytes");
}

static bool verifyIsRootedBytes(const char* caller) {
    void* sym = getIsRootedAddr();
    if (!sym) { LOGE("[CRC] %s: isRooted address is null", caller); return false; }
#if defined(__aarch64__) || defined(__arm__)
    __builtin___clear_cache(reinterpret_cast<char*>(sym),
                            reinterpret_cast<char*>(sym) + GUARD_BYTES);
#endif
    auto* b = reinterpret_cast<uint8_t*>(sym);
    if (!isExpectedBytesConfigured()) {
        LOGW("[CRC] %s: EXPECTED_BYTES not configured — skip", caller);
        return true;
    }
    if (memcmp(EXPECTED_BYTES, sym, GUARD_BYTES) != 0) {
        LOGE("[CRC] %s: BYTE MISMATCH — isRooted was modified", caller);
        logBytes("[CRC] expected:", EXPECTED_BYTES, GUARD_BYTES);
        logBytes("[CRC] current :", b, GUARD_BYTES);
        return false;
    }
    LOGI("[CRC] %s: OK", caller);
    return true;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_raconf_lab04_CrcGuard_verifyIsRooted(JNIEnv*, jclass) {
    if (!verifyIsRootedBytes("verifyIsRooted")) {
        fallbackCrash();
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C"
bool nativeVerifyIsRooted() {
    if (!verifyIsRootedBytes("nativeVerifyIsRooted")) {
        fallbackCrash();
        return false;
    }
    return true;
}