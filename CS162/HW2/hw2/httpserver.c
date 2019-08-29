#include <arpa/inet.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <unistd.h>
#include <stdbool.h>
#include <poll.h>

#include "libhttp.h"
#include "wq.h"

/* Convient af*/

/*
 * Global configuration variables.
 * You need to use these in your implementation of handle_files_request and
 * handle_proxy_request. Their values are set up in main() using the
 * command line arguments (already implemented for you).
 */
wq_t work_queue;
int num_threads;
int server_port;
char *server_files_directory;
char *server_proxy_hostname;
int server_proxy_port;

/* Helper functions */
void send_file_content(int fd, const char* path);
void send_subdir(int fd, char* path);
bool is_file_exist(const char* path);
void proxy_hanlder(int client_fd, int server_fd);

/*
 * Reads an HTTP request from stream (fd), and writes an HTTP response
 * containing:
 *
 *   1) If user requested an existing file, respond with the file
 *   2) If user requested a directory and index.html exists in the directory,
 *      send the index.html file.
 *   3) If user requested a directory and index.html doesn't exist, send a list
 *      of files in the directory with links to each.
 *   4) Send a 404 Not Found response.
 */
void handle_files_request(int fd) {

  struct http_request *request = http_request_parse(fd);
  char *path = malloc(sizeof(char)*(strlen(server_files_directory) + strlen(request->path) +1));
  strcpy(path, server_files_directory);
  strcat(path, request->path);
  struct stat file_stat;

  //check if file exists
  if(stat(path, &file_stat) != 0){
      http_start_response(fd, 404);
      return;
  }

  //check file type
  if(S_ISREG(file_stat.st_mode)){
    send_file_content(fd, path);
  }
  else if (S_ISDIR(file_stat.st_mode)){

       if(is_file_exist(path))
           send_file_content(fd, !strcmp(request->path, "/") ? request->path : path);
       else
           send_subdir(fd, path);
  }
  else{
    http_start_response(fd, 404);
  }

  free(path);
}


/*
 * Opens a connection to the proxy target (hostname=server_proxy_hostname and
 * port=server_proxy_port) and relays traffic to/from the stream fd and the
 * proxy target. HTTP requests from the client (fd) should be sent to the
 * proxy target, and HTTP responses from the proxy target should be sent to
 * the client (fd).
 *
 *   +--------+     +------------+     +--------------+
 *   | client | <-> | httpserver | <-> | proxy target |
 *   +--------+     +------------+     +--------------+
 */
void handle_proxy_request(int fd) {
  struct addrinfo hints;
  struct addrinfo *infoptr, *result;

  hints.ai_family = AF_INET;
  hints.ai_socktype = SOCK_STREAM;
  hints.ai_protocol = 0;

  /* Convert port number to string */
  char service[10];
  sprintf(service, "%d", server_proxy_port);

  result = getaddrinfo(server_proxy_hostname, service, &hints, &result);
  if(result != 0){
    perror("getaddrinfo error\n");
    exit(1);
  }

  int sockfd, connection_status;

  /* DNS look up for server and connect to it */
  for(infoptr = result; infoptr != NULL; infoptr = infoptr->ai_next){
        sockfd = socket(infoptr->ai_family, infoptr->ai_socktype, infoptr->ai_protocol);

        if(sockfd == -1)
            continue;   /* Failed to create socket */

        connection_status = connect(sockfd, infoptr->ai_addr, infoptr->ai_addrlen);
        if(connection_status != -1)
            break;      /* Success */

        close(sockfd);
  }

  if(infoptr == NULL){          /* No address succeeded */
    perror("Could not connect\n");
  }

  freeaddrinfo(result);           /* No longer needed */


   /* Here I didn't specify what actions should be perform
      if sockfd == -1 */
   int on = 1;
   setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));

  if (connection_status < 0) {
    /* Dummy request parsing, just to be compliant. */
    http_request_parse(fd);
    http_start_response(fd, 502);
    http_send_header(fd, "Content-Type", "text/html");
    http_end_headers(fd);
    http_send_string(fd, "<center><h1>502 Bad Gateway</h1><hr></center>");
    return;
  }

    /* Handle 2-way communication between clients and HTTP servers */
    proxy_handler(fd, sockfd);
}

/* Jobs for threads */
void dispatch(void (*request_handler)(int)){
    int client_socket = wq_pop(&work_queue);
    request_handler(client_socket);
    shutdown(client_socket, SHUT_WR);
    close(client_socket);
}


void init_thread_pool(int num_threads, void (*request_handler)(int)) {
    pthread_t threads[num_threads];

    for(int i = 0; i < num_threads; i++){
        pthread_create(&threads[i], NULL, dispatch, request_handler);
    }
}

/*
 * Opens a TCP stream socket on all interfaces with port number PORTNO. Saves
 * the fd number of the server socket in *socket_number. For each accepted
 * connection, calls request_handler with the accepted fd number.
 */
void serve_forever(int *socket_number, void (*request_handler)(int)) {

  /* Create server socket */
  *socket_number = socket(AF_INET, SOCK_STREAM, 0);
  if(socket_number < 0){
    perror("Failed to create a new socket.\n");
    exit(1);
  }

  /* Initialize socket structure */
  struct sockaddr_in server_address;
  memset(&server_address, 0, sizeof(server_address));
  server_address.sin_family = AF_INET;
  server_address.sin_port = htons(server_port);
  server_address.sin_addr.s_addr = INADDR_ANY;

  /* Bind the address and the port to the socket */
  int status = bind(*socket_number, (struct sockaddr *)&server_address, sizeof(server_address));
  if(status == -1){
    perror("Failed to assign address and port to socket\n");
    exit(1);
  }

  /* Setting some mysterious settings for our socket. See man setsockopt for
     more info if curious. */
  int socket_option = 1;
  if (setsockopt(*socket_number, SOL_SOCKET, SO_REUSEADDR, &socket_option,
        sizeof(socket_option)) == -1) {
    perror("Failed to set socket options");
    exit(errno);
  }

  /* Listen for new connections */
  status = listen(*socket_number, 50);
  if(status == -1)
      perror("Failed to listen for new connections\n");

  /* Initialize work queue */
  wq_init(&work_queue);

  init_thread_pool(num_threads, request_handler);

  /* Serve forever and a day */
  while (1) {
   /* Accept new client connection */
   int client_socket = accept(*socket_number, NULL, NULL);
   if(client_socket < 0){
     perror("Failed to accept new client connection\n");
     continue;
   }
   else {
        wq_push(&work_queue, client_socket);  /* Push client socket into work_queue */
   }
  }

  close(socket_number);
}

int server_fd;
void signal_callback_handler(int signum) {
  printf("Caught signal %d: %s\n", signum, strsignal(signum));
  exit(0);
}

char *USAGE =
  "Usage: ./httpserver --files www_directory/ --port 8000 [--num-threads 5]\n"
  "       ./httpserver --proxy inst.eecs.berkeley.edu:80 --port 8000 [--num-threads 5]\n";

void exit_with_usage() {
  fprintf(stderr, "%s", USAGE);
  exit(EXIT_SUCCESS);
}

int main(int argc, char **argv) {
  /* Registering signal handler. When user enteres Ctrl-C, signal_callback_handler will be invoked. */
  struct sigaction sa;
  sa.sa_flags = SA_RESTART;
  sa.sa_handler = &signal_callback_handler;
  sigemptyset(&sa.sa_mask);
  sigaction (SIGINT, &sa, NULL);

  /* Default settings */
  server_port = 8000;
  void (*request_handler)(int) = NULL;


  int i;
  for (i = 1; i < argc; i++) {
    if (strcmp("--files", argv[i]) == 0) {
      request_handler = handle_files_request;
      free(server_files_directory);
      server_files_directory = argv[++i];
      if (!server_files_directory) {
        fprintf(stderr, "Expected argument after --files\n");
        exit_with_usage();
      }
    } else if (strcmp("--proxy", argv[i]) == 0) {
      request_handler = handle_proxy_request;

      char *proxy_target = argv[++i];
      if (!proxy_target) {
        fprintf(stderr, "Expected argument after --proxy\n");
        exit_with_usage();
      }

      char *colon_pointer = strchr(proxy_target, ':');
      if (colon_pointer != NULL) {
        *colon_pointer = '\0';
        server_proxy_hostname = proxy_target;
        server_proxy_port = atoi(colon_pointer + 1);
      } else {
        server_proxy_hostname = proxy_target;
        server_proxy_port = 80;
      }
    } else if (strcmp("--port", argv[i]) == 0) {
      char *server_port_string = argv[++i];
      if (!server_port_string) {
        fprintf(stderr, "Expected argument after --port\n");
        exit_with_usage();
      }
      server_port = atoi(server_port_string);
    } else if (strcmp("--num-threads", argv[i]) == 0) {
      char *num_threads_str = argv[++i];
      if (!num_threads_str || (num_threads = atoi(num_threads_str)) < 1) {
        fprintf(stderr, "Expected positive integer after --num-threads\n");
        exit_with_usage();
      }
    } else if (strcmp("--help", argv[i]) == 0) {
      exit_with_usage();
    } else {
      fprintf(stderr, "Unrecognized option: %s\n", argv[i]);
      exit_with_usage();
    }
  }

  if (server_files_directory == NULL && server_proxy_hostname == NULL) {
    fprintf(stderr, "Please specify either \"--files [DIRECTORY]\" or \n"
                    "                      \"--proxy [HOSTNAME:PORT]\"\n");
    exit_with_usage();
  }

  serve_forever(&server_fd, request_handler);

  return EXIT_SUCCESS;
}

bool is_file_exist(const char* path){

    DIR* dir = opendir(path);
    for(struct dirent *f = readdir(dir); f != NULL; f = readdir(dir)){
        if(!strcmp(f->d_name, "index.html"))
            return true;
    }

    closedir(dir);
    dir = NULL;
    return false;
}

void send_file_content(int fd, const char* path){
    int file_d;

    /* Fail to open the file */
    if(!strcmp(path, "/")){
        char *name = malloc(sizeof(char) * (strlen(server_files_directory) + strlen("index.html")+2));
        strcpy(name, server_files_directory);
        strcat(name, "/index.html");
        file_d = open(name, O_RDONLY);
        free(name);
    }
    else
        file_d = open(path, O_RDONLY);

    /* Get file size */
    int len = lseek(file_d, 0, SEEK_END);
    lseek(file_d, 0, SEEK_SET); //reset file position

    char *read_buffer = malloc(len + 1);
    int bytes_read = read(file_d, read_buffer, len);

    /* Convert int to string */
    char content_len[10];
    sprintf(content_len, "%d", file_d < 0 ? file_d : bytes_read);

    http_start_response(fd, 200);
    http_send_header(fd, "Content-Type", (!strcmp(path, "/") ? "text/html" : http_get_mime_type(path)));
    http_send_header(fd, "Content-Length", content_len);
    http_end_headers(fd);


    /* Send content */
    http_send_data(fd, read_buffer, bytes_read);

    printf("%d and %s\n", bytes_read, content_len);
    free(read_buffer);
    close(file_d);
}

/* get the directory listring */
void send_subdir(int fd, char* path){

    http_start_response(fd, 200);
    http_send_header(fd, "Content-Type", "text/html");
    http_end_headers(fd);

    /* send subdir link */
    char **links = (char **) malloc(sizeof(char *));
    links[0] = malloc(sizeof(char) * strlen("<a href=") + strlen("></a>") + 4);
    strcpy(links[0], "<a href=");
    strcat(links[0], "../");
    strcat(links[0],"></a>");
    http_send_string(fd, links[0]);
    DIR* dir = opendir(path);
    int j = 1;
    for(struct dirent *f = readdir(dir); f != NULL; f = readdir(dir), j++){
        links = realloc(links, sizeof(char*) *(j+1));
        links[j] = malloc(sizeof(char) * strlen("<a href=") + strlen("></a>") + strlen(f->d_name) + 1);
        strcpy(links[j], "<a href=");
        strcat(links[j], f->d_name);
        strcat(links[j], "></a>");
        http_send_string(fd, links[j]);
    }

    closedir(dir);
    dir = NULL;

    int num = sizeof(links)/sizeof(links[0]);
    for(int i = 0; i< num; i++){
        free(links[i]);
    }

    free(links);
}

/* Handle 2 ways communication between clients and servers */
void proxy_handler(int client_fd, int server_fd){

    char buffer[128];
    int bytes_read;
    struct pollfd fds[2];
    fds[0].fd = client_fd;
    fds[0].events = POLLIN;
    fds[1].fd = server_fd;
    fds[1].events = POLLIN;

    while (1){
        /* check who is ready for I/O.
            0 here means timeout interval
            2 indicates the # of pollfd struct in the fds array */
        poll(fds, 2, 0);

        /* Check if client  has send something */
        if(fds[0].revents & POLLIN){
            bytes_read = read(fds[0].fd, buffer, sizeof(buffer) - 1);
            write(fds[1].fd, buffer, bytes_read);
        }

        /* Check if server has send something */
        if(fds[1].revents & POLLIN){
            bytes_read = read(fds[1].fd, buffer, sizeof(buffer) - 1);
            write(fds[0].fd, buffer, bytes_read);
        }
    }
}
