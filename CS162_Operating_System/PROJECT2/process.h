#ifndef USERPROG_PROCESS_H
#define USERPROG_PROCESS_H

#include "threads/thread.h"
#include "lib/user/syscall.h"
#include "filesys/file.h"

/* Added: This struct is used for conversion
    between file descriptor and struct file */
struct fd_node
{
    int fd;                      /* file descriptor */
    struct file *file;           /* file struct for file operations */
    struct list_elem elem;       /* fd list element */
};


tid_t process_execute (const char *file_name);
int process_wait (tid_t);
void process_exit (void);
void process_activate (void);

#endif /* userprog/process.h */
