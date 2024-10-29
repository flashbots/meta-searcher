// ns-enter.c
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/stat.h>

#define MAX_RETRIES 5
#define RETRY_DELAY 1
#define SEARCHER_UID 1000
#define SEARCHER_GID 1000
#define STATE_FILE "/run/system-mode.state"
#define MODE_PRODUCTION "production"

int check_production_mode() {
    FILE *f;
    char mode[32];
    char *newline;

    f = fopen(STATE_FILE, "r");
    if (!f) {
        return 0;  // If can't read, assume not production
    }

    if (fgets(mode, sizeof(mode), f) == NULL) {
        fclose(f);
        return 0;
    }
    fclose(f);

    // Remove newline if present
    newline = strchr(mode, '\n');
    if (newline) *newline = '\0';

    return (strcmp(mode, MODE_PRODUCTION) == 0);
}

int trigger_maintenance_transition() {
    pid_t pid;
    int status;

    // Run mode-controller as the searcher user
    pid = fork();
    if (pid == 0) {
        // Child process
        execl("/usr/bin/mode-controller", "mode-controller", "transition-to-maintenance", NULL);
        exit(1);  // If execl fails
    } else if (pid > 0) {
        // Parent process
        waitpid(pid, &status, 0);
        return WIFEXITED(status) && WEXITSTATUS(status) == 0;
    }
    return 0;
}

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
    
    fd = open("/var/run/netns/searcher-ns", O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        perror("open namespace");
        return -1;
    }

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
    
    // Set up environment variables
    char ps1[] = "PS1=searcher@tdx:\\w\\$ ";
    char user[] = "USER=searcher";
    char logname[] = "LOGNAME=searcher";
    char home[] = "HOME=/home/searcher";
    char shell[] = "SHELL=/bin/sh";
    char *env[] = {
        ps1,
        user,
        logname,
        home,
        shell,
        NULL
    };

    // Only allow searcher user
    if (original_uid != SEARCHER_UID) {
        fprintf(stderr, "This program can only be run by the searcher user\n");
        return 1;
    }

    // Wait for namespace to be available
    if (!wait_for_namespace()) {
        fprintf(stderr, "Network namespace not available\n");
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

    // Execute shell with proper environment
    execle("/bin/sh", "sh", NULL, env);
    perror("execl");
    return 1;
}
