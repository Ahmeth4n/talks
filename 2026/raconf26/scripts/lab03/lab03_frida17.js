Java.perform(function () {

    // ── 1. Java hooks ────────────────────────────────────
    var NativeRootDetector  = Java.use("com.raconf.lab03.NativeRootDetector");
    var NativeFridaDetector = Java.use("com.raconf.lab03.NativeFridaDetector");
    var JavaRootDetector    = Java.use("com.raconf.lab03.JavaRootDetector");
    var JavaFridaDetector   = Java.use("com.raconf.lab03.JavaFridaDetector");

    JavaRootDetector.isRooted.implementation = function (ctx) {
        console.log("[*] JavaRootDetector.isRooted hooked → false");
        return false;
    };
    JavaFridaDetector.isFridaPresent.implementation = function () {
        console.log("[*] JavaFridaDetector.isFridaPresent hooked → false");
        return false;
    };
    console.log("[*] Java detection hooks aktif");
});

// ── 2. dlopen hook — libdetector yüklenince native hook'ları kur ──
var isRootedPtr = null;

function findExport(moduleName, exportName) {
    try {
        var mod = Process.getModuleByName(moduleName);
        return mod.findExportByName(exportName);
    } catch (e) {
        return null;
    }
}

// android_dlopen_ext libc içinde
var dlopen = findExport("libc.so", "android_dlopen_ext");

if (!dlopen) {
    // bazı cihazlarda libdl içinde olabilir
    dlopen = findExport("libdl.so", "android_dlopen_ext");
}

if (dlopen) {
    Interceptor.attach(dlopen, {
        onLeave: function () {
            if (isRootedPtr) return;

            // ── 3. isRooted ──────────────────────────────
            isRootedPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeRootDetector_isRooted");
            if (!isRootedPtr) return;

            console.log("[*] isRooted bulundu: " + isRootedPtr);

            // ── 4. verifyIsRooted → CRC bypass ───────────
            var verifyPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_CrcGuard_verifyIsRooted");
            if (verifyPtr) {
                Interceptor.replace(verifyPtr, new NativeCallback(function () {
                    console.log("[*] verifyIsRooted hooked → true (CRC bypass)");
                    return 1;
                }, 'uint8', []));
            }

            // ── 5. isRooted → false ───────────────────────
            Interceptor.replace(isRootedPtr, new NativeCallback(function () {
                console.log("[*] Native isRooted hooked → false");
                return 0;
            }, 'uint8', []));

            // ── 6. crashAbort → engelle ───────────────────
            var crashAbortPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeRootDetector_crashAbort");
            if (crashAbortPtr) {
                Interceptor.replace(crashAbortPtr, new NativeCallback(function () {
                    console.log("[*] crashAbort engellendi");
                }, 'void', []));
            }

            // ── 7. Native isFridaPresent → false ─────────
            var nativeFridaPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeFridaDetector_isFridaPresent");
            if (nativeFridaPtr) {
                Interceptor.replace(nativeFridaPtr, new NativeCallback(function () {
                    console.log("[*] Native isFridaPresent hooked → false");
                    return 0;
                }, 'uint8', []));
            }

            console.log("[*] Tüm native hook'lar aktif");
        }
    });
} else {
    console.log("[-] android_dlopen_ext bulunamadi, libdetector zaten yuklu olabilir — direkt deneniyor");

    // dlopen bulunamazsa libdetector zaten yüklü olabilir, direkt hook'la
    var tryDirect = findExport("libdetector.so",
        "Java_com_raconf_lab03_NativeRootDetector_isRooted");
    if (tryDirect) {
        console.log("[*] Direkt hook baslatiliyor");
        isRootedPtr = tryDirect;
        // aynı hook'ları buraya da uygula
    }
}

console.log("[*] lab03 bypass aktif");