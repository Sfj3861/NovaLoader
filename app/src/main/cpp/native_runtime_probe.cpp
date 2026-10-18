#include <jni.h>
#include <cstddef>
#include <string>

namespace {

const char* compiled_abi() {
#if defined(__aarch64__)
    return "arm64-v8a";
#elif defined(__arm__)
    return "armeabi-v7a";
#elif defined(__x86_64__)
    return "x86_64";
#elif defined(__i386__)
    return "x86";
#else
    return "unknown";
#endif
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_safjran_novaloader_MainActivity_nativeRuntimeReport(JNIEnv* env, jobject /* thiz */) {
    const std::string report =
        std::string("NovaLoader native probe: OK\n") +
        "Probe API: 1\n" +
        "Compiled ABI: " + compiled_abi() + "\n" +
        "Native pointer size: " + std::to_string(sizeof(void*) * 8) + " bits\n\n" +
        "This confirms only that NovaLoader can load its own JNI library. " +
        "It does not attach to Geometry Dash, patch game memory, or load mods.";

    return env->NewStringUTF(report.c_str());
}
