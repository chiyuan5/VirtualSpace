#include <jni.h>
#include <string>
#include <pthread.h>
#include "hook_utils.h"

static JavaVM* g_jvm = nullptr;
static jobject g_hook_callback = nullptr;
static jmethodID g_method_hook = nullptr;
static bool g_hook_initialized = false;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_virtual_hook_nativehook_NativeHook_initNative(JNIEnv* env, jobject thiz) {
    if (g_hook_initialized) return;
    
    g_hook_callback = env->NewGlobalRef(thiz);
    
    jclass clazz = env->GetObjectClass(thiz);
    g_method_hook = env->GetMethodID(clazz, "onNativeHook", 
        "(Ljava/lang/String;Ljava/lang/String;JJ)J");
    
    g_hook_initialized = true;
}

JNIEXPORT jlong JNICALL Java_com_virtual_hook_nativehook_NativeHook_hookMethod(JNIEnv* env, jobject thiz,
    jstring library, jstring symbol, jlong newMethod) {
    
    if (!g_hook_initialized || !library || !symbol) return 0;
    
    const char* lib_name = env->GetStringUTFChars(library, nullptr);
    const char* sym_name = env->GetStringUTFChars(symbol, nullptr);
    
    void* module_base = get_module_base(lib_name);
    if (!module_base) {
        module_base = get_module_base("/system/lib/libandroid_runtime.so");
    }
    
    void* old_method = nullptr;
    int result = hook_plt_got(module_base, sym_name, (void*)newMethod, &old_method);
    
    if (result == HOOK_SUCCESS && g_hook_callback && g_method_hook) {
        JNIEnv* jni_env = nullptr;
        if (g_jvm->GetEnv((void**)&jni_env, JNI_VERSION_1_6) == JNI_OK && jni_env) {
            jstring jlib = jni_env->NewStringUTF(lib_name);
            jstring jsym = jni_env->NewStringUTF(sym_name);
            jni_env->CallLongMethod(g_hook_callback, g_method_hook, 
                jlib, jsym, (jlong)(uintptr_t)old_method, (jlong)(uintptr_t)newMethod);
            jni_env->DeleteLocalRef(jlib);
            jni_env->DeleteLocalRef(jsym);
        }
    }
    
    env->ReleaseStringUTFChars(library, lib_name);
    env->ReleaseStringUTFChars(symbol, sym_name);
    
    return (jlong)(uintptr_t)old_method;
}

JNIEXPORT jlong JNICALL Java_com_virtual_hook_nativehook_NativeHook_hookNativeMethod(JNIEnv* env, jobject thiz,
    jlong targetAddr, jlong newAddr) {
    
    void* old_addr = nullptr;
    int result = inline_hook((void*)(uintptr_t)targetAddr, (void*)(uintptr_t)newAddr, &old_addr);
    
    return result == HOOK_SUCCESS ? (jlong)(uintptr_t)old_addr : 0;
}

JNIEXPORT jlong JNICALL Java_com_virtual_hook_nativehook_NativeHook_getModuleBase(JNIEnv* env, jobject thiz,
    jstring moduleName) {
    
    if (!moduleName) return 0;
    
    const char* name = env->GetStringUTFChars(moduleName, nullptr);
    void* base = get_module_base(name);
    env->ReleaseStringUTFChars(moduleName, name);
    
    return (jlong)(uintptr_t)base;
}

JNIEXPORT jlong JNICALL Java_com_virtual_hook_nativehook_NativeHook_getSymbol(JNIEnv* env, jobject thiz,
    jstring moduleName, jstring symbolName) {
    
    if (!moduleName || !symbolName) return 0;
    
    const char* lib_name = env->GetStringUTFChars(moduleName, nullptr);
    const char* sym_name = env->GetStringUTFChars(symbolName, nullptr);
    
    void* module_base = get_module_base(lib_name);
    void* symbol = get_elf_symbol(module_base, sym_name);
    
    env->ReleaseStringUTFChars(moduleName, lib_name);
    env->ReleaseStringUTFChars(symbolName, sym_name);
    
    return (jlong)(uintptr_t)symbol;
}

JNIEXPORT jboolean JNICALL Java_com_virtual_hook_nativehook_NativeHook_unhook(JNIEnv* env, jobject thiz,
    jstring library, jstring symbol, jlong oldAddr) {
    
    if (!library || !symbol) return JNI_FALSE;
    
    const char* lib_name = env->GetStringUTFChars(library, nullptr);
    const char* sym_name = env->GetStringUTFChars(symbol, nullptr);
    
    int result = unhook_plt_got(lib_name, sym_name, (void*)(uintptr_t)oldAddr);
    
    env->ReleaseStringUTFChars(library, lib_name);
    env->ReleaseStringUTFChars(symbol, sym_name);

    return result == HOOK_SUCCESS ? JNI_TRUE : JNI_FALSE;
}

}