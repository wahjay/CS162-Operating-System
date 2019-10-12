/****************************************************
syscall.c function
This function exxecute system calls for pintos.
*****************************************************/
#include "userprog/syscall.h"
#include <stdio.h>
#include <string.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "filesys/file.h"
#include "filesys/filesys.h"
#include "userprog/process.h"
#include "devices/shutdown.h"
#include "threads/malloc.h"
#include "threads/vaddr.h"
#include "devices/input.h"
#include "userprog/pagedir.h"
#include "list.h"
#include "threads/vaddr.h"
#include "devices/block.h"
#include "filesys/cache.h"

static void syscall_handler (struct intr_frame *);
struct file* fd_to_file(int fd);
struct fd_node* fd_to_fnode(int fd);
int add_file_to_list(struct file*);
struct list_elem* fd_to_elem(int fd);
void detect_p (const void *addr);
void * get_kernel_address (const void *addr);
void check_str (const void *str, unsigned size);


void
syscall_init (void)
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

/*using switch case to choose syscall
For each syscall, we need to check the pointer, then
excute the syscall by calling the function from
system calls*/
static void
syscall_handler (struct intr_frame *f)
{
  void *file_name;
  void *buf;
  uint32_t* args = ((uint32_t*) f->esp);

  get_kernel_address ((const void*) args);
  switch(args[0])
  {
    //or exit system call
    case SYS_EXIT:
        detect_p (&args[1]);
        f->eax =args[1];
        exit(args[1]);
        NOT_REACHED();
        break;
    //for pretice system call
    case SYS_PRACTICE:
        detect_p (&args[1]);
        f->eax = args[1] + 1;
        break;
    //for halt system call
    case SYS_HALT:
        shutdown_power_off();
        break;
    //for write system call
    case SYS_WRITE:
        detect_p (&args[1]);
        check_str ((void *) args[2], (unsigned) args[3]);
        buf = get_kernel_address ((void *) args[2]);
        detect_p (&args[3]);
        //for stdout case
        if (args[1] == 1)
        {
            putbuf((void *)args[2], args[3]);
            f->eax = args[3];
        }
        //for stdin case
        else if(args[1] == STDIN_FILENO)
            f->eax = 0;
        //for file case
        else
        {
             struct file* f_ = fd_to_file(args[1]);
             //file is NULL
             if(f_ == NULL)
                 f->eax = -1;
             else
                 f->eax = file_write(f_, buf, args[3]);
        }
        break;
    // system open case
    case SYS_OPEN:
     {
        file_name = get_kernel_address ((void *) args[1]);
        struct file *f_ = filesys_open(file_name);
        f->eax = f_ ? add_file_to_list(f_) : -1;
        break;
     }
    //system wait case
    case SYS_WAIT:
        detect_p (&args[1]);
        f->eax = process_wait (args[1]);
        break;
    //for system  execute case
    case SYS_EXEC:
    {
        file_name = get_kernel_address ((void *) args[1]);
        f->eax  = process_execute ((const char *)file_name);
        break;
    }
    //create file case
    case SYS_CREATE:
        detect_p(&args[1]);
        file_name = get_kernel_address ((void *) args[1]);
        detect_p (&args[2]);
        f->eax = filesys_create(file_name,args[2], false);
        break;
    //system seek case
    case SYS_SEEK:
        detect_p(&args[1]);
        detect_p (&args[2]);
        file_seek (fd_to_file(args[1]), args[2]);
        break;
    //system tell case
    case SYS_TELL:
        detect_p (&args[1]);
        f->eax = file_tell (fd_to_file(args[1]));
        break;
    //system remove case
    case SYS_REMOVE:
        detect_p(&args[1]);
        f->eax = filesys_remove ((char *) args[1]);
       break;
    //system filesize case
    case SYS_FILESIZE:
        detect_p(&args[1]);
        f->eax = file_length (fd_to_file(args[1]));
        break;
    //close file case
    case SYS_CLOSE:
        detect_p(&args[1]);
        struct fd_node* fn = fd_to_fnode(args[1]);
        if(fn)
          {
            file_close (fn->file);
            list_remove(&fn->elem);
            free(fn);
          }
          break;

    //chdir case
    case SYS_CHDIR:
        detect_p(&args[1]);
        file_name = get_kernel_address ((void *) args[1]);
        f->eax = filesys_chdir (file_name);
        break;
    //mkdir case
    case SYS_MKDIR:
        detect_p(&args[1]);
        file_name = get_kernel_address ((void *) args[1]);
        f->eax = filesys_create(file_name, 0, true);
        break;
    //readdir case
    case SYS_READDIR:
        detect_p(&args[1]);
        detect_p (&args[2]);
        file_name = get_kernel_address ((void *) args[2]);
        f->eax = dir_readdir(fd_to_file(args[1]), file_name);
        break;
    //isdir case
    case SYS_ISDIR:
        detect_p(&args[1]);
        f->eax = file_isdir(fd_to_file(args[1]));
        break;
    //inumber case
    case SYS_INUMBER:
        detect_p(&args[1]);
        f->eax = file_get_inumber(fd_to_file(args[1]));
        break;

    //read file case
    case SYS_READ:
       detect_p (&args[1]);
       check_str ((void *) args[2], (unsigned) args[3]);
       buf = get_kernel_address ((void *) args[2]);
       detect_p (&args[3]);
       int fd = args[1];
       //for stdin
       if(args[1] == 0)
       {
           uint8_t *buffer = (uint8_t *) args[2];
           size_t i = 0;
           while (i < args[3])
           {
              buffer[i] = input_getc ();
              if (buffer[i++] == '\n')
                 break;
           }
           f->eax = i;
        }
        //for stdout
        else if (fd == STDOUT_FILENO)
            f->eax = 0;
        //read file
        else
          {
             struct file* f_ = fd_to_file(args[1]);
             if(f_ == NULL)
                 f->eax = -1;
             else
                 f->eax =  file_read (f_, buf, args[3]);
          }
        break;
    case SYS_BUFFER_RESET:
      bf_cache_reset ();
      break;
    case SYS_BUFFER_STAT:
      switch (args[1])
      {
        case 0:
          f->eax = bf_cache_miss_cnt;
          break;
        case 1:
          f->eax = bf_cache_hit_cnt;
          break;
        case 2:
          f->eax = block_get_read_cnt (fs_device);
          break;
        case 3:
          f->eax = block_get_write_cnt (fs_device);
          break;
      }
      break;
  }
}

/*get the kernel address of the argument*/
void * get_kernel_address (const void *addr)
{
  detect_p(addr);

  int *pointer;
  if ((pointer = pagedir_get_page (thread_current ()->pagedir, addr)) == NULL)
    exit (-1);
  return pointer;
}

/*use file descritor to find the file struct and return it*/
struct file* fd_to_file(int fd)
{
    struct thread* cur = thread_current();
    struct fd_node *fnode;
    struct list_elem *e;
    for(e = list_begin(&cur->fd_list); e != list_end(&cur->fd_list);
        e = list_next(e))
     {
        fnode = list_entry(e, struct fd_node, elem);
        if (fnode->fd == fd)
            return fnode->file;
     }

     return NULL;
}

/* add file to fd_list and return its file descriptor */
int add_file_to_list(struct file* f)
{
    struct thread* cur = thread_current();
    struct fd_node *fn = malloc(sizeof (struct fd_node));
    fn->file = f;
    fn->fd = cur->cur_fd;
    cur->cur_fd++;
    list_push_back(&cur->fd_list, &fn->elem);
    return fn->fd;
}

/*print exit stastus and jump to thread exit*/
void exit (int status)
{
	thread_current()->exit_status = status;
	printf("%s: exit(%d)\n", thread_current()->name, status);
        thread_exit ();
}

///*check pointer of the address from argument*/
void detect_p (const void *addr)
{
  if (is_kernel_vaddr (addr) || !addr)
    exit (-1);
}

//check the file_name, check if it is a correct string
void check_str (const void *str, unsigned size)
{
  if (is_kernel_vaddr (str) || is_kernel_vaddr (str + size))
  {
    exit (-1);
  }
}

struct fd_node* fd_to_fnode(int fd)
{
    struct thread* cur = thread_current();
    struct fd_node *fnode;
    struct list_elem *e;
    for(e = list_begin(&cur->fd_list); e != list_end(&cur->fd_list);
        e = list_next(e))
     {
        fnode = list_entry(e, struct fd_node, elem);
        if (fnode->fd == fd)
            return fnode;
     }

     return NULL;
}
