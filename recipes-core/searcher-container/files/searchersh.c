#include <stdio.h>      // For fprintf, perror
#include <stdlib.h>     // For exit, malloc/free, strdup, atoi
#include <string.h>     // For strcmp, strtok
#include <unistd.h>     // For execl
#include <ctype.h>      // For isdigit

#define MAX_LINES 10000000

// argc is the number of command-line arguments
// argv is an array of C-strings (character pointers)
int main(int argc, char *argv[]) {

    // We expect exactly 3 arguments: 
    // Example: ssh -i ~/.ssh/yocto-searcher -p 8084 searcher@localhost hello 5
    // argv[0] = 'searchersh'
    // argv[1] = '-c'
    // argv[2] = 'hello 5'

    if (argc != 3) {
        fprintf(stderr, "Invalid number of arguments\n");
        return 1; // return error code 1
    }

    // Verify argv[0] is "searchersh"
    if (strcmp(argv[0], "searchersh") != 0) {
        fprintf(stderr, "Error: This program must be invoked as 'searchersh'\n");
        return 1;
    }

    // Verify argv[1] is "-c"
    if (strcmp(argv[1], "-c") != 0) {
        fprintf(stderr, "Error: Second argument must be '-c'\n");
        return 1;
    }

    // Make a copy of argv[2], because strtok will modify the string
    // strdup() allocates memory and copies the entire string.
    // We must free() this memory later.
    char *arg_copy = strdup(argv[2]);
    if (!arg_copy) {
        perror("strdup failed"); 
        return 1; // return error code 1
    }

    // Use strtok() to split the string in arg_copy by spaces (" ")
    // strtok modifies the string by inserting '\0' to separate tokens.
    // 'command' will point to the first token, if it exists.
    char *command = strtok(arg_copy, " ");
    if (command == NULL) {
        // If there's no token at all (e.g., empty or whitespace-only string),
        // we print an error and quit.
        fprintf(stderr, "No command provided. Valid commands are: toggle, status, logs\n");
        free(arg_copy); // free the memory
        return 1;       // return error code 1
    }

    // If the first token (command) is not NULL, we try to get the next token
    // 'arg' is needed when the command is "logs <number_of_lines>"
    // e.g., if argv[2] = "logs 3", then:
    //   command = "logs"
    //   arg     = "3"
    char *arg = strtok(NULL, " ");

    // Compare the first token to see which command we want.
    // 1) "toggle"
    // 2) "status"
    // 3) "logs"
    // Anything else -> invalid.
    
    // If command == "toggle", call /usr/bin/toggle via sudo
    if (strcmp(command, "toggle") == 0) {
        // execl() replaces the current process with the new program
        // Arguments to execl:
        //   1) path to executable: "/usr/bin/sudo"
        //   2) argv[0] for new program: "sudo"
        //   3) "-S" accept password from stdin
        //   4) "/usr/bin/toggle" (the program we actually want to run via sudo)
        //   5) NULL terminator for argument list
        // execl("/usr/bin/sudo", "sudo", "-S", "/usr/bin/toggle", NULL);
        execl("/usr/bin/sudo", "sudo", "/usr/bin/toggle", NULL);
        
        // If execl fails, we reach here. perror prints error details.
        perror("execl failed (toggle)");
        
        // We must free the copied string before exiting
        free(arg_copy);
        return 1;
    }

    // If command == "status", print the contents of /etc/searcher-network.state
    else if (strcmp(command, "status") == 0) {
        // runs: cat /etc/searcher-network.state
        execl("/bin/cat", "cat", "/etc/searcher-network.state", NULL);
        
        perror("execl failed (status)");
        free(arg_copy);
        return 1;
    }

    // If command == "disk", print the contents of /var/log/disk-encryption.log
    else if (strcmp(command, "disk") == 0) {
        execl("/bin/cat", "cat", "/var/log/disk-encryption.log", NULL);
        perror("execl failed (disk)");
        free(arg_copy);
        return 1;
    }

    // If command == "lighthouse", print the contents of /var/log/lighthouse.log
    else if (strcmp(command, "lighthouse") == 0) {
        execl("/usr/bin/tail", "tail", "-f", "/var/log/lighthouse.log", NULL);
        perror("execl failed (lighthouse)");
        free(arg_copy);
        return 1;
    }

    // If command == "tail-logs", print the contents of /persistent/delayed_logs/output.log
    else if (strcmp(command, "tail-logs") == 0) {
        execl("/usr/bin/tail", "tail", "-f", "/persistent/delayed_logs/output.log", NULL);
        perror("execl failed (tail-logs)");
        free(arg_copy);
        return 1;
    }

    // If command == "http", print the contents of /tmp/httpserver.log
    else if (strcmp(command, "http") == 0) {
        execl("/bin/cat", "cat", "/tmp/httpserver.log", NULL);
        perror("execl failed (http)");
        free(arg_copy);
        return 1;
    }

    // If command == "proxy", print the contents of /tmp/cvm-reverse-proxy-server.log
    else if (strcmp(command, "proxy") == 0) {
        execl("/bin/cat", "cat", "/tmp/cvm-reverse-proxy-server.log", NULL);
        perror("execl failed (proxy)");
        free(arg_copy);
        return 1;
    }

    // If command == "pubkey", print the contents of /tmp/pubkey.log
    else if (strcmp(command, "pubkey") == 0) {
        execl("/usr/bin/wget", "wget", "-q", "http://127.0.0.1:8645/pubkey", "-O", "-", NULL);
        perror("execl failed (pubkey)");
        free(arg_copy);
        return 1;
    }

    // If command == "logs", we expect a second token representing number of lines
    else if (strcmp(command, "logs") == 0) {
        // If no second token, user didn't specify how many lines
        if (arg == NULL) {
            fprintf(stderr, "Usage: logs <number_of_lines>\n");
            free(arg_copy);
            return 1; // return error code 1
        }
        else if (strcmp(command, "logs") == 0) {
            // If someone wrote "logs 3", 'arg' should be "3"
            if (!arg) {
                fprintf(stderr, "Usage: logs <number_of_lines>\n");
                free(arg_copy);
                return 1;
            }
        }

        // 2) Convert to int
        int lines = atoi(arg);

        // 3) Check the range
        if (lines < 1 || lines > MAX_LINES) {
            fprintf(stderr, "Number of lines must be between 1 and %d\n", MAX_LINES);
            free(arg_copy);
            return 1;
        }

        // Call tail with the specified number of lines, e.g.:
        // tail -n <arg> /persistent/delayed_logs/output.log
        // If arg = "3", that's tail -n 3 /persistent/delayed_logs/output.log
        execl("/usr/bin/tail", "tail", "-n", arg, "/persistent/delayed_logs/output.log", (char *)NULL);
        
        perror("execl failed (logs)");
        free(arg_copy);
        return 1; // return error code 1
    }
    
    // If we reach here, the command didn't match toggle/status/logs
    fprintf(stderr, "Invalid command. Valid commands are: toggle, status, logs\n");
    free(arg_copy); // Clean up allocated memory
    return 1;       // Return error code 1
}