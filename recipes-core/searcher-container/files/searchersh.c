#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    if (argc != 3) {
        fprintf(stderr, "Invalid number of arguments\n");
        return 1;
    }

    // Make a copy of argv[2], because strtok modifies the string
    char *arg_copy = strdup(argv[2]);
    if (!arg_copy) {
        perror("strdup failed");
        return 1;
    }

    // Split argv[2] on whitespace into tokens
    char *command = strtok(arg_copy, " ");
    char *arg = strtok(NULL, " ");  // for example, "3" if argv[2] = "logs 3"

    if (command) {
        if (strcmp(command, "toggle") == 0) {
            execl("/usr/bin/sudo", "sudo", "-S", "/usr/bin/toggle", NULL);
            perror("execl failed (toggle)");
            free(arg_copy);
            return 1;
        }
        else if (strcmp(command, "status") == 0) {
            execl("/bin/cat", "cat", "/etc/searcher-network.state", NULL);
            perror("execl failed (status)");
            free(arg_copy);
            return 1;
        }

        // EXTRA TEMP VISIBILITY

        else if (strcmp(command, "disk") == 0) {
            execl("/bin/cat", "cat", "/var/log/disk-encryption.log", NULL);
            perror("execl failed (disk)");
            free(arg_copy);
            return 1;
        }
        else if (strcmp(command, "lighthouse") == 0) {
            execl("/usr/bin/tail", "tail", "-f", "/var/log/lighthouse.log", NULL);
            perror("execl failed (lighthouse)");
            free(arg_copy);
            return 1;
        }
        else if (strcmp(command, "http") == 0) {
            execl("/bin/cat", "cat", "/tmp/httpserver.log", NULL);
            perror("execl failed (http)");
            free(arg_copy);
            return 1;
        }
        else if (strcmp(command, "proxy") == 0) {
            execl("/bin/cat", "cat", "/tmp/cvm-reverse-proxy-server.log", NULL);
            perror("execl failed (proxy)");
            free(arg_copy);
            return 1;
        }
        else if (strcmp(command, "pubkey") == 0) {
            execl("/usr/bin/wget", "wget", "-q", "http://127.0.0.1:8645/pubkey", "-O", "-", NULL);
            perror("execl failed (pubkey)");
            free(arg_copy);
            return 1;
        }
    
        else if (strcmp(command, "logs") == 0) {
            // If someone wrote "logs 3", 'arg' should be "3"
            if (!arg) {
                fprintf(stderr, "Usage: logs <number_of_lines>\n");
                free(arg_copy);
                return 1;
            }
            execl("/usr/bin/tail", "tail", "-n", arg, "/delayed_logs/output.log", (char *)NULL);
            perror("execl failed (logs)");
            free(arg_copy);
            return 1;
        }
    }
    
    // If none of the above matched, it's an invalid command
    fprintf(stderr, "Invalid command. Valid commands are: toggle, status, logs\n");
    free(arg_copy);
    return 1;
}

