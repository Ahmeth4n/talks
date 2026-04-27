// ── 1. Java Thread Engelle ────────────────────────────────
Java.perform(function () {
    var Thread = Java.use("java.lang.Thread");
    Thread.start.implementation = function () {
        var name = this.getName();
        if (name === "JavaDetectionThread") {
            console.log("[*] JavaDetectionThread.start() engellendi");
            return;
        }
        this.start();
    };
    console.log("[*] JavaDetectionThread hook aktif");
});

// ── 2. Native Thread Engelle ──────────────────────────────
var pthreadCreatePtr = null;

Process.enumerateModules().forEach(function (m) {
    if (pthreadCreatePtr) return;
    var exp = m.findExportByName("pthread_create");
    if (exp) {
        pthreadCreatePtr = exp;
        console.log("[*] pthread_create bulundu: " + exp + " in " + m.name);
    }
});

if (pthreadCreatePtr) {
    var realPthreadCreate = new NativeFunction(
        pthreadCreatePtr, 'int', ['pointer', 'pointer', 'pointer', 'pointer']
    );

    Interceptor.replace(pthreadCreatePtr, new NativeCallback(function (tidPtr, attr, func, arg) {
        var detector = Process.findModuleByName("libdetector.so");
        if (detector) {
            var start = detector.base;
            var end   = start.add(detector.size);
            if (func.compare(start) >= 0 && func.compare(end) < 0) {
                console.log("[*] libdetector.so pthread_create engellendi");
                return 0;
            }
        }
        return realPthreadCreate(tidPtr, attr, func, arg);
    }, 'int', ['pointer', 'pointer', 'pointer', 'pointer']));

    console.log("[*] Native pthread_create hook aktif");
} else {
    console.log("[-] pthread_create bulunamadi");
}