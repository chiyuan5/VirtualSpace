// 简化版 - 移除复杂的hook实现
// 占位符文件，避免编译错误

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "VirtualHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// 空实现
int hook_plt_got(void* module_base, const char* symbol_name, void* new_func, void** old_func) {
    LOGD("Hook not implemented");
    return -1;
}

int unhook_plt_got(void* module_base, const char* symbol_name, void* old_func) {
    LOGD("Unhook not implemented");
    return -1;
}
