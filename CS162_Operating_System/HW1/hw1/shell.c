#include <ctype.h>
#include <errno.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>
#include <dirent.h>
#include <fcntl.h>

#include "tokenizer.h"

/* Convenience macro to silence compiler warnings about unused function parameters. */
#define unused __attribute__((unused))

/* Whether the shell is connected to an actual terminal or not. */
bool shell_is_interactive;

/* File descriptor for the shell input */
int shell_terminal;

/* Terminal mode settings for the shell */
struct termios shell_tmodes;

/* Process group id for the shell */
pid_t shell_pgid;

/* Built-in commands */
int cmd_exit(struct tokens *tokens);
int cmd_help(struct tokens *tokens);
int cmd_cd(struct tokens *tokens);
int cmd_pwd(struct tokens *tokens);
int cmd_wait(struct tokens *tokens);

/* Built-in command functions take token array (see parse.h) and return int */
typedef int cmd_fun_t(struct tokens *tokens);

/* Built-in command struct and lookup table */
typedef struct fun_desc {
  cmd_fun_t *fun;
  char *cmd;
  char *doc;
} fun_desc_t;

fun_desc_t cmd_table[] = {
  {cmd_help, "?", "show this help menu"},
  {cmd_exit, "exit", "exit the command shell"},
  {cmd_cd, "cd", "change the current working directory to a given directory"},
  {cmd_pwd, "pwd", "print the current working directory"},
  {cmd_wait, "wait", "wait until all background jobs have terminated"}
};

static size_t bg_count = 0; // counting background processes

/* Wait for all background jobs to terminate */
int cmd_wait(unused struct tokens *tokens){
    //here
    while (bg_count > 0) {}
    return 1;
}

/* Change the current directory to a given directory */
int cmd_cd(struct tokens *tokens){
  chdir(tokens_get_token(tokens, 1));
  return 1;
}

/* Prints the current working directory */
int cmd_pwd(unused struct tokens *tokens){
  char cur_dir[4096];
  if(getcwd(cur_dir, sizeof(cur_dir)) != NULL)
    printf("%s\n", cur_dir);
  return 1;
}

/* Prints a helpful description for the given command */
int cmd_help(unused struct tokens *tokens) {
  for (unsigned int i = 0; i < sizeof(cmd_table) / sizeof(fun_desc_t); i++)
    printf("%s - %s\n", cmd_table[i].cmd, cmd_table[i].doc);
  return 1;
}

/* Exits this shell */
int cmd_exit(unused struct tokens *tokens) {
  exit(0);
}

/* Looks up the built-in command, if it exists. */
int lookup(char cmd[]) {
  for (unsigned int i = 0; i < sizeof(cmd_table) / sizeof(fun_desc_t); i++)
    if (cmd && (strcmp(cmd_table[i].cmd, cmd) == 0))
      return i;
  return -1;
}


/* Intialization procedures for this shell */
void init_shell() {
  /* Our shell is connected to standard input. */
  shell_terminal = STDIN_FILENO;

  /* Check if we are running interactively */
  shell_is_interactive = isatty(shell_terminal);

  if (shell_is_interactive) {
    /* If the shell is not currently in the foreground, we must pause the shell until it becomes a
     * foreground process. We use SIGTTIN to pause the shell. When the shell gets moved to the
     * foreground, we'll receive a SIGCONT. */
    while (tcgetpgrp(shell_terminal) != (shell_pgid = getpgrp()))
      kill(-shell_pgid, SIGTTIN);


    /* Ignore signals from shell */
    signal (SIGINT, SIG_IGN);      /* Ignore CTRL-C  */ 
    //signal (SIGQUIT, SIG_IGN);     /* Ignore CTRL-\ */
    signal (SIGTSTP, SIG_IGN);     /* Ignore CTRL-Z */
    signal (SIGTTIN, SIG_IGN);     /* Ignore program pause */
    signal (SIGTTOU, SIG_IGN);     /* Ignore program pause */
    signal (SIGCHLD, SIG_IGN);     /* Ignore child process termination */

    /* Saves the shell's process id */
    shell_pgid = getpid();

    /* Take control of the terminal */
    tcsetpgrp(shell_terminal, shell_pgid);

    /* Save the current termios to a variable, so it can be restored later. */
    tcgetattr(shell_terminal, &shell_tmodes);
  }
}


void signal_handler(int sig,  siginfo_t *si, void *context){
    switch(sig){
        case SIGINT:
            printf("CTRL-C Detected!");
            killpg(getpid(), SIGINT);
            signal(SIGCONT, SIG_DFL);
            break;
        case EOF:
            printf("CTRL-D Detected!");
            exit(0);
            break;
        case SIGCHLD:
            bg_count--; // background running child process return 
            break;
    }
} 

int main(unused int argc, unused char *argv[]) {
  init_shell();

  static char line[4096];
  int line_num = 0;

  /* Please only print shell prompts when standard input is not a tty */
  if (shell_is_interactive)
    fprintf(stdout, "%d: ", line_num);

  while (fgets(line, 4096, stdin)) {
    /* Split our line into words. */
    struct tokens *tokens = tokenize(line);

    /* Find which built-in function to run. */
    char *cmd = tokens_get_token(tokens, 0);
    int fundex = lookup(cmd);
    char ch = cmd[0]; /* check for path resolution */

    if (fundex >= 0) {
      cmd_table[fundex].fun(tokens);
    } else if (ch != '/' ) {
        DIR *d;
        char *env = getenv("PATH");
        char directory[4096];
        char **paths = NULL;
        size_t size = 0; // size for **paths
        size_t n = 0; 
        struct dirent *dir; // hold file info
        char ch;
        size_t length = strlen(env);
       
        /* Turn the environment variable into a list of paths*/
        for(int i = 0; i < length; i++){
            ch = env[i];
            if(ch == ':' || ch == '\n'){
                if (n > 0) {
                    void *word = copy_word(directory, n);
                    vector_push(&paths, &size, word);
                    n = 0;
                }
            } else
                directory[n++] = ch;
        }

        if(n > 0){
            void *word = copy_word(directory, n);
            vector_push(&paths, &size, word);
            n = 0;
        }
        /* Search the cmd in each directory */
        for(int i = 0; i < size; i++){
            d = opendir(paths[i]);
            if (d){
                /* if found, we get the full path and call execv */
                while ((dir = readdir(d)) != NULL ){
                    if(strcmp(cmd, dir->d_name) == 0){
                        int len = tokens_get_length(tokens);
                        //char *args[len];
                        char **args;
                        int path_len = strlen(paths[i]) + strlen(cmd) + 2;
                        char *path_cmd = malloc(sizeof(char)* path_len);
                        strcpy(path_cmd, paths[i]);
                        strcat(path_cmd, "/");
                        strcat(path_cmd, cmd);
                        int saved_stdin = dup(0); /* save stdin fd for later restore */
                        int saved_stdout = dup(1); /* save stdout fd for later restore */
                            
                        pid_t pid = fork(); /* fork a child to do the job */
                        if(pid == 0 && len >= 3 && strcmp(tokens_get_token(tokens, len-2), ">") == 0){
                            char *filename = tokens_get_token(tokens, len-1);
                            int out = open(filename, O_WRONLY| O_CREAT, S_IRWXU | O_TRUNC, 644);
                            dup2(out, 1); /* replace standard output with output file */
                            close(out);
                            //char *args[len-2];
                            args = (char **) malloc(sizeof(char*) * (len-2));
                            for(int i=0; i<len-2; i++)
                                args[i] = tokens_get_token(tokens, i);
                            args[len-2] = NULL;
                            execv(path_cmd, args);
                            dup2(saved_stdout, 1); /* restore to stdout */
                            close(saved_stdout);

                        } else if (pid == 0 && len >= 3 && strcmp(tokens_get_token(tokens,len-2), "<") == 0){
                            char *filename = tokens_get_token(tokens, len-1);
                            int in = open(filename, O_RDONLY);
                            dup2(in, 0);/* replace standard input with input file */
                            close(in);
                            //char *args[len-2];
                            args = (char **) malloc(sizeof(char*) * (len-2));
                            for(int i=0; i<len-2; i++)
                                args[i] = tokens_get_token(tokens, i);
                            args[len-2] = NULL;
                            execv(path_cmd, args);
                            dup2(saved_stdin, 0); /* restore to stdin */
                            close(saved_stdin);

                        } else if (pid == 0){
                            args = (char **) malloc(sizeof(char*) * len);
                            for(int i=0; i<len; i++)
                                args[i] = tokens_get_token(tokens, i);
                            args[len] = NULL;
                            execv(path_cmd, args);
                        }
                        /*
                        pid_t pid = fork();
                        if(pid == 0){ 
                            execv(path_cmd, args);
                        } */
                        wait(NULL);
                        free(path_cmd);
                        free(args);
                        break;
                    }
                }
                closedir(d);
            }
        }
      
      if(paths != NULL) {
        for(int i =0; i<size; i++)
            free(paths[i]);
      }
      
    } else {  /*full path to a program */
        int len = tokens_get_length(tokens);
        char **args;
        int saved_stdin = dup(0);
        int saved_stdout = dup(1);
        int background = 0;  // backgrounf off initially
        //here
        struct sigaction act;
        act.sa_sigaction = signal_handler;
        act.sa_flags = SA_NODEFER;

        //check for background running
        if (strcmp(tokens_get_token(tokens, len-1), "&") == 0){
            bg_count++; // about to fork a child process in background
            len = len - 1;
            background = 1; // background running on
        }

        pid_t pid = fork();
        if(pid == 0 && len >= 3 && strcmp(tokens_get_token(tokens, len-2), ">") == 0){
            char *filename = tokens_get_token(tokens, len-1);
            int out = open(filename, O_WRONLY| O_CREAT, S_IRWXU | O_TRUNC, 644);
            dup2(out, 1);
            setpgrp();
            signal(SIGTTOU, SIG_IGN);
            tcsetpgrp(out,getpgrp());
            close(out);
            sigaction(SIGINT, &act, NULL);

            args = (char **) malloc(sizeof(char *) * (len-2));
            for(int i=0; i<len-2; i++)
                args[i] = tokens_get_token(tokens, i);
            args[len-2] = NULL;
            execv((tokens_get_token(tokens,0)), args);
            dup2(saved_stdout, 1);
            close(saved_stdout);

        } else if (pid == 0 && len >= 3 && strcmp(tokens_get_token(tokens,len-2), "<") == 0){
            char *filename = tokens_get_token(tokens, len-1);
            int in = open(filename, O_RDONLY);
            dup2(in, 0);
            setpgrp();
            signal(SIGTTOU, SIG_IGN);
            tcsetpgrp(in,getpgrp());
            close(in);
            sigaction(SIGINT, &act, NULL);
            args = (char **) malloc(sizeof(char*) * (len-2));
            for(int i=0; i<len-2; i++)
                args[i] = tokens_get_token(tokens, i);
            args[len-2] = NULL;
            execv((tokens_get_token(tokens,0)), args);
            dup2(saved_stdin, 0);
            close(saved_stdin);

        } else if (pid == 0){
            setpgrp();
            signal(SIGTTOU, SIG_IGN);
            tcsetpgrp(STDIN_FILENO, getpgrp()); /* send the child process to foreground*/
            sigaction(SIGINT, &act, NULL);
            sigaction(EOF, &act, NULL);  // may not need
            args = (char **) malloc(sizeof(char*) * len);
            for(int i=0; i<len; i++)
                args[i] = tokens_get_token(tokens, i);
            args[len] = NULL;
            execv((tokens_get_token(tokens,0)), args);
        } 

        setpgid(pid, pid);
        sigaction(SIGCHLD, &act, NULL);
        //if background on, then dont wait for any child
        if (background != 1)
            waitpid(pid, NULL, 0);

        tcsetpgrp(shell_terminal, shell_pgid); /* Send the shell back to foreground */
        free(args);
    }

    if (shell_is_interactive)
      /* Please only print shell prompts when standard input is not a tty */
      fprintf(stdout, "%d: ", ++line_num);

    /* Clean up memory */
    tokens_destroy(tokens);
  }

  return 0;
}
