Java.perform(function () {

    // ── 1. Java hooks ────────────────────────────────────
    var JavaRootDetector  = Java.use("com.raconf.lab03.JavaRootDetector");
    var JavaFridaDetector = Java.use("com.raconf.lab03.JavaFridaDetector");

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

// ── Yardımcı: Frida 17 uyumlu export bulucu ──────────────
function findExport(moduleName, exportName) {
    try {
        return Process.getModuleByName(moduleName).findExportByName(exportName);
    } catch (e) {
        return null;
    }
}

// ── 2. Native hook'lar (CRC bypass YOK) ──────────────────
var dlopen = findExport("libc.so", "android_dlopen_ext")
          || findExport("libdl.so", "android_dlopen_ext");

if (dlopen) {
    Interceptor.attach(dlopen, {
        onLeave: function () {

            var isRootedPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeRootDetector_isRooted");
            if (!isRootedPtr) return;

            console.log("[*] isRooted bulundu: " + isRootedPtr);

            Interceptor.replace(isRootedPtr, new NativeCallback(function () {
                console.log("[*] Native isRooted hooked → false");
                return 0;
            }, 'uint8', []));

            var crashAbortPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeRootDetector_crashAbort");
            if (crashAbortPtr) {
                Interceptor.replace(crashAbortPtr, new NativeCallback(function () {
                    console.log("[*] crashAbort engellendi");
                }, 'void', []));
            }

            var nativeFridaPtr = findExport("libdetector.so",
                "Java_com_raconf_lab03_NativeFridaDetector_isFridaPresent");
            if (nativeFridaPtr) {
                Interceptor.replace(nativeFridaPtr, new NativeCallback(function () {
                    console.log("[*] Native isFridaPresent hooked → false");
                    return 0;
                }, 'uint8', []));
            }

            console.log("[*] Native hook'lar aktif (CRC bypass YOK)");
        }
    });
} else {
    console.log("[-] android_dlopen_ext bulunamadi");
}

console.log("[*] lab03 bypass (CRC bypass YOK) aktif");