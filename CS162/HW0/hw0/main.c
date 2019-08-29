#include <stdio.h>
#include <sys/resource.h>

int main() {
    struct rlimit lim;

    getrlimit(RLIMIT_STACK, &lim);
    printf("stack size: %ld\n", (long int)lim.rlim_cur);

    getrlimit(RLIMIT_NPROC, &lim);
    printf("process limit: %ld\n", (long int)lim.rlim_cur);

    getrlimit(RLIMIT_NOFILE, &lim);
    printf("max file descriptors: %ld\n", (long int)lim.rlim_cur);
    return 0;
}
