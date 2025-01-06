#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    if (argc != 3) {
        fprintf(stderr, "Invalid number of arguments\n");
        return 1;
    }

    const char *command = argv[2];

    if (strcmp(command, "toggle") == 0) {
        execl("/usr/bin/sudo", "sudo", "-S", "/usr/bin/toggle", NULL);
        perror("execl failed");
        return 1;
    }
    else if (strcmp(command, "status") == 0) {
        execl("/bin/cat", "cat", "/tmp/searcher-network.state", NULL);
        perror("execl failed");
        return 1;
    }
    else if (strcmp(command, "failsafe") == 0) {
        execl("/bin/sh", "sh", "-c", "podman rm -f searcher-container", NULL);
        perror("execl failed");
        return 1;
    }

    fprintf(stderr, "Invalid command. Valid commands are: toggle, status, failsafe\n");
    return 1;
}

