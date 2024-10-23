#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <sched.h>

#define MAX_RETRIES 5
#define RETRY_DELAY 1
#define SEARCHER_UID 1000
#define SEARCHER_GID 1000

int wait_for_namespace() {
    int retries = 0;
    while (retries < MAX_RETRIES) {
        if (access("/var/run/netns/searcher-ns", F_OK) == 0) {
            return 1;
        }
        sleep(RETRY_DELAY);
        retries++;
    }
    return 0;
}

int enter_namespace() {
    int fd;
    
    // Open namespace file
    fd = open("/var/run/netns/searcher-ns", O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        perror("open namespace");
        return -1;
    }

    // Enter namespace
    if (setns(fd, CLONE_NEWNET) < 0) {
        perror("setns");
        close(fd);
        return -1;
    }

    close(fd);
    return 0;
}

int main(int argc, char *argv[]) {
    uid_t original_uid = getuid();
    gid_t original_gid = getgid();

    // Only allow searcher user
    if (original_uid != SEARCHER_UID) {
        fprintf(stderr, "This program can only be run by the searcher user\n");
        return 1;
    }

    // Wait for namespace to be available
    if (!wait_for_namespace()) {
        fprintf(stderr, "Network namespace not available, running in default namespace\n");
        execl("/bin/sh", "sh", NULL);
        perror("execl");
        return 1;
    }

    // Temporarily elevate privileges
    if (setuid(0) != 0) {
        perror("setuid");
        return 1;
    }

    // Enter the namespace while we have root privileges
    if (enter_namespace() != 0) {
        fprintf(stderr, "Failed to enter namespace\n");
        return 1;
    }

    // Drop back to searcher user permanently
    if (setgid(original_gid) != 0 || setuid(original_uid) != 0) {
        perror("dropping privileges");
        return 1;
    }

    // Execute shell as searcher user in the new namespace
    execl("/bin/sh", "sh", NULL);
    perror("execl");
    return 1;
}
