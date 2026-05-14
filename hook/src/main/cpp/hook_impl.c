#include "hook_utils.h"
#include <sys/uio.h>
#include <asm/ptrace.h>

#define PAGE_SIZE 4096
#define ARM_INSTRUCTION_SIZE 4
#define THUMB_INSTRUCTION_SIZE 2

static inline void sync_cache(void* start, size_t len) {
    __builtin___clear_cache(start, (char*)start + len);
}

static int is_thumb_code(uint16_t first_instruction) {
    return (first_instruction & 0x8000) == 0;
}

static void* get_thumb_branch_target(uint32_t instruction, void* pc) {
    int32_t offset = (instruction & 0x7FF) << 1;
    offset |= ((int32_t)(instruction >> 12) & 0x7) << 12;
    if (instruction & 0x400000) {
        offset = -((0x00FFFFFF - offset) & 0x00FFFFFF);
    }
    return (void*)((uintptr_t)pc + offset + 4);
}

void* get_module_base(const char* module_name) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return NULL;

    char line[512];
    void* base = NULL;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, module_name) && strstr(line, "r-xp")) {
            char* start = strtok(line, "-");
            if (start) {
                base = (void*)strtoul(start, NULL, 16);
                break;
            }
        }
    }

    fclose(fp);
    return base;
}

void* get_elf_symbol(void* module_base, const char* symbol_name) {
    int fd = open("/proc/self/maps", O_RDONLY);
    if (fd < 0) return NULL;

    FILE* fp = fdopen(fd, "r");
    char line[512];
    char full_path[256] = {0};
    void* base = NULL;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "r-xp") && strstr(line, ".so")) {
            char* path_start = strstr(line, "/");
            if (path_start) {
                char* path_end = strstr(path_start, "\n");
                if (path_end) *path_end = '\0';
                if (strstr(path_start, module_name)) {
                    strncpy(full_path, path_start, sizeof(full_path) - 1);
                    char* start = strtok(line, "-");
                    if (start) {
                        base = (void*)strtoul(start, NULL, 16);
                    }
                    break;
                }
            }
        }
    }

    fclose(fp);
    if (!base || !full_path[0]) return NULL;

    int sym_fd = open(full_path, O_RDONLY);
    if (sym_fd < 0) return NULL;

    void* symbol_addr = NULL;
    unsigned char* elf = (unsigned char*)mmap(NULL, 1 * 1024 * 1024, PROT_READ, MAP_PRIVATE, sym_fd, 0);
    if (elf && elf != MAP_FAILED) {
        uint32_t rel_count = 0;
        uint32_t rel_offset = 0;
        uint32_t sym_count = 0;
        uint32_t sym_offset = 0;
        uint32_t str_offset = 0;

        uint32_t* dyn = (uint32_t*)(elf + 0x40);
        for (int i = 0; dyn[i] != 0 && i < 100; i += 2) {
            uint32_t tag = dyn[i];
            uint32_t val = dyn[i + 1];
            if (tag == 5) rel_offset = val;
            if (tag == 6) rel_count = val;
            if (tag == 7) sym_offset = val;
            if (tag == 10) str_offset = val;
        }

        if (rel_offset && sym_offset && str_offset) {
            unsigned char* rel = elf + rel_offset;
            unsigned char* sym = elf + sym_offset;
            char* str = (char*)(elf + str_offset);

            for (uint32_t i = 0; i < rel_count; i++) {
                uint32_t r_info = ((uint32_t*)rel)[i * 2 + 1];
                uint32_t sym_idx = r_info >> 8;

                Elf32_Sym* s = (Elf32_Sym*)(sym + sym_idx * 16);
                if (s->st_name && (s->st_info >> 4) == 1) {
                    const char* name = str + s->st_name;
                    if (strcmp(name, symbol_name) == 0) {
                        uint32_t r_offset = ((uint32_t*)rel)[i * 2];
                        symbol_addr = (void*)(base + r_offset);
                        break;
                    }
                }
            }
        }

        munmap(elf, 1 * 1024 * 1024);
    }

    close(sym_fd);
    return symbol_addr;
}

int hook_plt_got(void* module_base, const char* symbol_name, void* new_func, void** old_func) {
    if (!module_base || !symbol_name || !new_func) {
        return HOOK_ERROR;
    }

    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return HOOK_ERROR;

    char line[512];
    char full_path[256] = {0};
    unsigned long rel_offset = 0, sym_offset = 0, str_offset = 0;
    unsigned long rel_size = 0;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, "r-xp") && strstr(line, ".so")) {
            char* path_start = strstr(line, "/");
            if (path_start) {
                char* path_end = strstr(path_start, "\n");
                if (path_end) *path_end = '\0';
                if (strstr(path_start, module_base)) {
                    strncpy(full_path, path_start, sizeof(full_path) - 1);
                    break;
                }
            }
        }
    }
    fclose(fp);

    if (!full_path[0]) return HOOK_ERROR;

    int fd = open(full_path, O_RDONLY);
    if (fd < 0) return HOOK_ERROR;

    unsigned char* elf = (unsigned char*)mmap(NULL, 2 * 1024 * 1024, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);

    if (elf == MAP_FAILED) return HOOK_ERROR;

    unsigned long base = 0;
    unsigned char* dyn = elf + 0x40;

    for (int i = 0; dyn[i] && i < 200; i += 2) {
        unsigned long tag = *(unsigned long*)&dyn[i];
        unsigned long val = *(unsigned long*)&dyn[i + 1];
        if (tag == 1) base = val;
        else if (tag == 5) rel_offset = val;
        else if (tag == 6) rel_size = val;
        else if (tag == 7) sym_offset = val;
        else if (tag == 10) str_offset = val;
    }

    if (!rel_offset || !sym_offset || !str_offset) {
        munmap((void*)elf, 2 * 1024 * 1024);
        return HOOK_ERROR;
    }

    unsigned char* rel = elf + rel_offset;
    unsigned char* sym = elf + sym_offset;
    char* strtab = (char*)(elf + str_offset);

    unsigned int rel_count = rel_size / 8;

    for (unsigned int i = 0; i < rel_count; i++) {
        unsigned long r_info = *(unsigned long*)(rel + i * 8 + 4);
        unsigned int sym_idx = r_info >> 8;
        unsigned int type = r_info & 0xFF;

        if (type != 7 && type != 5) continue;

        Elf32_Sym* s = (Elf32_Sym*)(sym + sym_idx * 16);
        if (!s->st_name || !s->st_value) continue;

        const char* name = strtab + s->st_name;
        if (strcmp(name, symbol_name) == 0) {
            unsigned long r_offset = *(unsigned long*)(rel + i * 8);
            void* got_addr = (void*)(base + r_offset);
            void* original = *((void**)got_addr);

            if (old_func) *old_func = original;

            mprotect((void*)((unsigned long)got_addr & ~(PAGE_SIZE - 1)), PAGE_SIZE, PROT_READ | PROT_WRITE);
            *((void**)got_addr) = new_func;
            mprotect((void*)((unsigned long)got_addr & ~(PAGE_SIZE - 1)), PAGE_SIZE, PROT_READ);

            munmap((void*)elf, 2 * 1024 * 1024);
            return HOOK_SUCCESS;
        }
    }

    munmap((void*)elf, 2 * 1024 * 1024);
    return HOOK_ERROR;
}

int inline_hook(void* target, void* new_func, void** old_func) {
    if (!target || !new_func) return HOOK_ERROR;

    unsigned long target_addr = (unsigned long)target;
    unsigned long page_start = target_addr & ~(PAGE_SIZE - 1);

    if (mprotect((void*)page_start, PAGE_SIZE, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
        return HOOK_ERROR;
    }

    unsigned char* code = (unsigned char*)target;
    unsigned char backup[ARM_INSTRUCTION_SIZE];
    memcpy(backup, code, ARM_INSTRUCTION_SIZE);

    if (old_func) {
        *old_func = malloc(ARM_INSTRUCTION_SIZE + 8);
        if (*old_func) {
            memcpy(*old_func, backup, ARM_INSTRUCTION_SIZE);
            unsigned char* backup_code = (unsigned char*)(*old_func);
            backup_code[ARM_INSTRUCTION_SIZE] = 0xFE;
            backup_code[ARM_INSTRUCTION_SIZE + 1] = 0x0F;
            backup_code[ARM_INSTRUCTION_SIZE + 2] = 0x1F;
            backup_code[ARM_INSTRUCTION_SIZE + 3] = 0xF8;
            unsigned int offset = (unsigned long)target + ARM_INSTRUCTION_SIZE - ((unsigned long)(*old_func) + ARM_INSTRUCTION_SIZE + 4);
            memcpy(backup_code + ARM_INSTRUCTION_SIZE + 4, &offset, 4);
            sync_cache(*old_func, ARM_INSTRUCTION_SIZE + 8);
        }
    }

    unsigned int branch_insn = 0xEA000000;
    int offset = ((unsigned long)new_func - (unsigned long)target - 8) >> 2;
    if (offset > 0x00FFFFFF || offset < -(int)0x00FFFFFF) {
        code[0] = 0x01;
        code[1] = 0x00;
        code[2] = 0x9F;
        code[3] = 0xF5;
        code[4] = 0x00;
        code[5] = 0x00;
        code[6] = 0xA0;
        code[7] = 0xF2;
        unsigned int imm16 = ((unsigned long)new_func >> 16) & 0xFFFF;
        unsigned int imm12 = (unsigned long)new_func & 0xFFF;
        code[8] = (imm12 >> 1) & 0xFF;
        code[9] = ((imm12 & 1) << 7) | ((imm16 & 0x7) << 4) | 0x00;
        code[10] = (imm16 >> 3) & 0xFF;
        code[11] = 0x80 | ((imm16 >> 11) & 0x3);
    } else {
        branch_insn |= (offset & 0x00FFFFFF);
        memcpy(code, &branch_insn, 4);
    }

    syncCache(target, ARM_INSTRUCTION_SIZE);

    mprotect((void*)page_start, PAGE_SIZE, PROT_READ | PROT_EXEC);

    return HOOK_SUCCESS;
}

int unhook_plt_got(void* module_base, const char* symbol_name, void* old_func) {
    return hook_plt_got(module_base, symbol_name, old_func, NULL);
}
