setImmediate(function () {
    Java.perform(function () {
        var J = Java.use("com.raconf.lab01.JavaRootDetector");
        var N = Java.use("com.raconf.lab01.NativeRootDetector");
        J.isRooted.implementation = function (ctx) {
            console.log("[+] Java bypass");
            return false;
        };
        
        J.crashKillProcess.implementation = function () {
            console.log("[+] killProcess blocked");
        };
        N.isRooted.implementation = function () {
            console.log("[+] Native (Java wrapper) bypass");
            return false;
        };
        N.crashAbort.implementation = function () {
            console.log("[+] crashAbort blocked");
        };
    });

    function hookNative() {
        var mod = null;
        try {
            mod = Process.getModuleByName("librootdetector.so");
        } catch (e) {
            setTimeout(hookNative, 100);
            return;
        }
        var base = mod.base;
        console.log("[+] librootdetector.so @ " + base);
        var isRooted = base.add(0x65c90);
        Interceptor.attach(isRooted, {
            onEnter: function () {
                console.log("[+] isRooted called");
            },
            onLeave: function (retval) {
                console.log("[+] Native return patched");
                retval.replace(ptr(0));
            }
        });
    }
    hookNative();

    var libc = Process.getModuleByName("libc.so");
    var abortPtr = libc.findExportByName("abort");
    if (abortPtr) {
        Interceptor.replace(abortPtr, new NativeCallback(function () {
            console.log("[+] abort() blocked");
        }, 'void', []));
        console.log("[+] abort() hooked @ " + abortPtr);
    }
});