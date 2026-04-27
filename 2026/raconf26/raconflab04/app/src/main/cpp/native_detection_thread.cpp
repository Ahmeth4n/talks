#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <string>
#include <cstring>
#include <cstdlib>
#include <unistd.h>
#include <csignal>

extern bool nativeCheckRooted();
extern bool nativeCheckFridaPresent();
extern "C" bool nativeVerifyIsRooted();

#define TAG "RDL_NativeThread"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct DetectionArgs {
    JavaVM*   jvm;
    jobject   classLoaderRef;
    jmethodID loadClassMethod;
};

static jclass findClassViaLoader(JNIEnv* env, jobject classLoader,
                                 jmethodID loadClassMethod, const char* className) {
    std::string dotName(className);
    for (char& c : dotName) if (c == '/') c = '.';
    jstring jName = env->NewStringUTF(dotName.c_str());
    auto clazz = (jclass) env->CallObjectMethod(classLoader, loadClassMethod, jName);
    env->DeleteLocalRef(jName);
    return clazz;
}

static void* detectionThreadFunc(void* arg) {
    auto* args = reinterpret_cast<DetectionArgs*>(arg);
    JavaVM*   jvm             = args->jvm;
    jobject   classLoaderRef  = args->classLoaderRef;
    jmethodID loadClassMethod = args->loadClassMethod;
    delete args;

    LOGI("[NativeThread] started — tid=%d", gettid());

    // ── 1. CRC Kontrolü ──────────────────────────────────
    bool crcOk = nativeVerifyIsRooted();
    if (!crcOk) {
        LOGE("[NativeThread] CRC failed — killing");
        raise(SIGKILL);
        return nullptr;
    }
    LOGI("[NativeThread] CRC OK");

    // ── 2. Root Detection ────────────────────────────────
    bool rootDetected = nativeCheckRooted();
    LOGI("[NativeThread] Root  : %s", rootDetected ? "TRUE" : "FALSE");

    // ── 3. Frida Detection ───────────────────────────────
    bool fridaDetected = nativeCheckFridaPresent();
    LOGI("[NativeThread] Frida : %s", fridaDetected ? "TRUE" : "FALSE");

    LOGI("[NativeThread] detection complete — calling Java callback");

    // ── JNI attach ───────────────────────────────────────
    JNIEnv* env    = nullptr;
    bool attached  = false;
    int status = jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        if (jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) attached = true;
        else { LOGE("[NativeThread] AttachCurrentThread failed"); return nullptr; }
    } else if (status != JNI_OK) {
        LOGE("[NativeThread] GetEnv failed: %d", status);
        return nullptr;
    }

    jclass clazz = findClassViaLoader(env, classLoaderRef, loadClassMethod,
                                      "com/raconf/lab04/NativeDetectionManager");
    if (!clazz || env->ExceptionCheck()) {
        env->ExceptionClear();
        LOGE("[NativeThread] NativeDetectionManager class not found");
        env->DeleteGlobalRef(classLoaderRef);
        if (attached) jvm->DetachCurrentThread();
        return nullptr;
    }

    jmethodID mid = env->GetStaticMethodID(clazz, "nativeDetectionCallback", "(ZZ)V");
    if (!mid) {
        LOGE("[NativeThread] nativeDetectionCallback method not found");
        env->DeleteLocalRef(clazz);
        env->DeleteGlobalRef(classLoaderRef);
        if (attached) jvm->DetachCurrentThread();
        return nullptr;
    }

    env->CallStaticVoidMethod(clazz, mid,
                              static_cast<jboolean>(rootDetected),
                              static_cast<jboolean>(fridaDetected));

    env->DeleteLocalRef(clazz);
    env->DeleteGlobalRef(classLoaderRef);
    if (attached) jvm->DetachCurrentThread();

    LOGI("[NativeThread] finished");
    return nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_raconf_lab04_NativeDetectionManager_nativeStartDetectionThread(
        JNIEnv* env, jclass, jobject classLoader) {

    JavaVM* jvm = nullptr;
    env->GetJavaVM(&jvm);
    if (!jvm) { LOGE("[NativeThread] GetJavaVM failed"); return; }

    jobject globalCL = env->NewGlobalRef(classLoader);
    if (!globalCL) { LOGE("[NativeThread] NewGlobalRef failed"); return; }

    jclass clClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = env->GetMethodID(clClass, "loadClass",
                                           "(Ljava/lang/String;)Ljava/lang/Class;");
    env->DeleteLocalRef(clClass);
    if (!loadClass) {
        LOGE("[NativeThread] ClassLoader.loadClass not found");
        env->DeleteGlobalRef(globalCL);
        return;
    }

    auto* dargs = new DetectionArgs{ jvm, globalCL, loadClass };

    pthread_t      tid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    int ret = pthread_create(&tid, &attr, detectionThreadFunc, dargs);
    pthread_attr_destroy(&attr);

    if (ret != 0) {
        LOGE("[NativeThread] pthread_create failed: %d", ret);
        env->DeleteGlobalRef(globalCL);
        delete dargs;
    } else {
        LOGI("[NativeThread] pthread_create OK");
    }
}