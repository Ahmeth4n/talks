setImmediate(function () {
    var GHIDRA_BASE = 0x00100000;
    var GHIDRA_FRIDA = 0x00166a00;
    var GHIDRA_ROOT  = 0x001661b0;
    var OFFSET_FRIDA = GHIDRA_FRIDA - GHIDRA_BASE;
    var OFFSET_ROOT  = GHIDRA_ROOT  - GHIDRA_BASE;

    Java.perform(function () {
        var JR = Java.use("com.raconf.lab02.JavaRootDetector");
        var JF = Java.use("com.raconf.lab02.JavaFridaDetector");
        var NR = Java.use("com.raconf.lab02.NativeRootDetector");
        var NF = Java.use("com.raconf.lab02.NativeFridaDetector");

        JR.isRooted.implementation = function () {
            console.log("[+] Java Root bypass");
            return false;
        };
        NR.isRooted.implementation = function () {
            console.log("[+] Native Root wrapper bypass");
            return false;
        };
        JF.isFridaPresent.implementation = function () {
            console.log("[+] Java Frida bypass");
            return false;
        };
        NF.isFridaPresent.implementation = function () {
            console.log("[+] Native Frida wrapper bypass");
            return false;
        };
        JR.crashKillProcess.implementation = function () {
            console.log("[+] killProcess blocked");
        };
        NR.crashAbort.implementation = function () {
            console.log("[+] crashAbort blocked");
        };
    });

    // ───────── NATIVE HOOK ─────────
    function hookNative() {
        var mod = null;
        try {
            mod = Process.getModuleByName("libdetector.so");
        } catch (e) {
            setTimeout(hookNative, 100);
            return;
        }
        var base = mod.base;
        console.log("[+] libdetector.so @ " + base);

        var fridaCheck = base.add(OFFSET_FRIDA);
        var rootCheck  = base.add(OFFSET_ROOT);

        Interceptor.attach(fridaCheck, {
            onEnter: function () {
                console.log("[+] Native Frida check called");
            },
            onLeave: function (retval) {
                console.log("[+] Native Frida bypassed");
                retval.replace(ptr(0));
            }
        });

        Interceptor.attach(rootCheck, {
            onEnter: function () {
                console.log("[+] Native Root check called");
            },
            onLeave: function (retval) {
                console.log("[+] Native Root bypassed");
                retval.replace(ptr(0));
            }
        });
    }
    hookNative();

    // ───────── CRASH BYPASS ─────────
    var libc = Process.getModuleByName("libc.so");
    var abortPtr = libc.findExportByName("abort");
    if (abortPtr) {
        Interceptor.replace(abortPtr, new NativeCallback(function () {
            console.log("[+] abort() blocked");
        }, 'void', []));
        console.log("[+] abort() hooked @ " + abortPtr);
    }
});