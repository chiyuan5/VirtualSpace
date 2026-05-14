#ifndef HOOK_UTILS_H
#define HOOK_UTILS_H

#include <jni.h>
#include <unistd.h>
#include <sys/mman.h>
#include <link.h>
#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <errno.h>

#define HOOK_SUCCESS 0
#define HOOK_ERROR -1

typedef void* (*HookFunc)(void*, ...);

typedef struct {
    const char* libName;
    const char* symbolName;
    void* oldFunc;
    void* newFunc;
    void** oldFuncPtr;
} HookInfo;

typedef struct {
    uint32_t st_name;
    uint32_t st_info;
    uint32_t st_other;
    uint32_t st_shndx;
} Elf32_Sym;

typedef struct {
    uint32_t r_offset;
    uint32_t r_info;
} Elf32_Rel;

typedef struct {
    uint32_t n_namesz;
    uint32_t n_descsz;
    uint32_t n_type;
    char n_name[0];
} Elf32_Nhdr;

#ifndef Elf32_Rel
typedef struct {
    unsigned int offset;
    unsigned int info;
} Elf32_Rel;
#endif

void* get_module_base(const char* module_name);
void* get_elf_symbol(void* module_base, const char* symbol_name);
int hook_plt_got(void* module_base, const char* symbol_name, void* new_func, void** old_func);
int unhook_plt_got(void* module_base, const char* symbol_name, void* old_func);
int inline_hook(void* target, void* new_func, void** old_func);

#endif
