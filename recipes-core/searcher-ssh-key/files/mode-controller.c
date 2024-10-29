#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <time.h>
#include <sys/file.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <signal.h>
#include <fcntl.h>
#include <syslog.h>
#include <stdarg.h>

#define STATE_FILE "/run/system-mode.state"
#define LOCK_FILE "/run/mode-transition.lock"
#define MODE_MAINTENANCE "maintenance"
#define MODE_PRODUCTION "production"
#define MODE_TRANSITION "transition"
#define SEARCHER_UID 1000
#define TIMEOUT_SECONDS 300  // 5 minutes

static int lock_fd = -1;

// Function prototypes
static int acquire_lock(void);
static void release_lock(void);
static int read_current_mode(char *mode, size_t size);
static int write_mode(const char *mode);
static int execute_transition_script(const char *from_mode, const char *to_mode);
static void log_message(int priority, const char *format, ...);
static int kill_existing_connections(void);

static void log_message(int priority, const char *format, ...) {
    va_list args;
    va_start(args, format);
    vsyslog(priority, format, args);
    vfprintf(stderr, format, args);
    fprintf(stderr, "\n");
    va_end(args);
}

static int acquire_lock(void) {
    if (lock_fd != -1) return 0;  // Already have lock

    lock_fd = open(LOCK_FILE, O_RDWR | O_CREAT, 0600);
    if (lock_fd == -1) {
        log_message(LOG_ERR, "Failed to open lock file: %s", strerror(errno));
        return -1;
    }

    if (flock(lock_fd, LOCK_EX | LOCK_NB) == -1) {
        close(lock_fd);
        lock_fd = -1;
        log_message(LOG_ERR, "Failed to acquire lock: %s", strerror(errno));
        return -1;
    }
    return 0;
}

static void release_lock(void) {
    if (lock_fd != -1) {
        flock(lock_fd, LOCK_UN);
        close(lock_fd);
        lock_fd = -1;
    }
}

static int read_current_mode(char *mode, size_t size) {
    if (acquire_lock() != 0) {
        return -1;
    }

    FILE *f = fopen(STATE_FILE, "r");
    if (!f) {
        release_lock();
        return -1;
    }

    if (fgets(mode, size, f) == NULL) {
        fclose(f);
        release_lock();
        return -1;
    }

    char *newline = strchr(mode, '\n');
    if (newline) *newline = '\0';

    fclose(f);
    return 0;
}

static int write_mode(const char *mode) {
    FILE *f = fopen(STATE_FILE, "w");
    if (!f) return -1;

    fprintf(f, "%s\n", mode);
    fclose(f);
    return 0;
}

static int kill_existing_connections(void) {
    uid_t real_uid = getuid();
    uid_t real_gid = getgid();
    int ret = 0;
    FILE *fp;
    char line[256];
    char *endptr;
    long pid;

    // Elevate privileges
    if (setuid(0) != 0 || setgid(0) != 0) {
        log_message(LOG_ERR, "Failed to elevate privileges for killing connections");
        return -1;
    }

    // Using BusyBox ps to get PIDs
    fp = popen("ps | grep ^'[ ]*[0-9]'", "r");
    if (!fp) {
        log_message(LOG_ERR, "Failed to get process list");
        if (setgid(real_gid) != 0 || setuid(real_uid) != 0) {
            log_message(LOG_ERR, "Failed to drop privileges after popen error");
        }
        return -1;
    }

    // Skip header line
    fgets(line, sizeof(line), fp);

    // Process each line
    while (fgets(line, sizeof(line), fp)) {
        pid = strtol(line, &endptr, 10);
        if (pid > 1) {  // Skip init process
            struct stat st;
            char procpath[32];
            snprintf(procpath, sizeof(procpath), "/proc/%ld", pid);
            
            if (stat(procpath, &st) == 0 && st.st_uid == SEARCHER_UID) {
                log_message(LOG_INFO, "Sending SIGTERM to process %ld", pid);
                if (kill(pid, SIGTERM) != 0) {
                    log_message(LOG_WARNING, "Failed to send SIGTERM to PID %ld: %s", 
                              pid, strerror(errno));
                }
            }
        }
    }
    pclose(fp);

    // Wait for processes to terminate
    sleep(2);

    // Force kill remaining processes
    fp = popen("ps | grep ^'[ ]*[0-9]'", "r");
    if (!fp) {
        if (setgid(real_gid) != 0 || setuid(real_uid) != 0) {
            log_message(LOG_ERR, "Failed to drop privileges after second popen error");
        }
        return -1;
    }

    // Skip header line
    fgets(line, sizeof(line), fp);

    while (fgets(line, sizeof(line), fp)) {
        pid = strtol(line, &endptr, 10);
        if (pid > 1) {
            struct stat st;
            char procpath[32];
            snprintf(procpath, sizeof(procpath), "/proc/%ld", pid);
            
            if (stat(procpath, &st) == 0 && st.st_uid == SEARCHER_UID) {
                log_message(LOG_INFO, "Sending SIGKILL to process %ld", pid);
                if (kill(pid, SIGKILL) != 0) {
                    log_message(LOG_WARNING, "Failed to send SIGKILL to PID %ld: %s", 
                              pid, strerror(errno));
                    ret = -1;
                }
            }
        }
    }
    pclose(fp);

    // Drop privileges
    if (setgid(real_gid) != 0 || setuid(real_uid) != 0) {
        log_message(LOG_ERR, "Failed to drop privileges");
        return -1;
    }

    return ret;
}

static int execute_transition_script(const char *from_mode, const char *to_mode) {
    char cmd[256];
    int ret;
    uid_t real_uid = getuid();
    uid_t real_gid = getgid();

    log_message(LOG_INFO, "Applying transition rules...");
    if (setuid(0) != 0) {
        log_message(LOG_ERR, "Failed to elevate privileges");
        return -1;
    }

    // Apply transition rules first
    snprintf(cmd, sizeof(cmd), "/usr/sbin/nft -f /etc/nftables-transition.conf");
    ret = system(cmd);
    if (ret != 0) {
        log_message(LOG_ERR, "Failed to apply transition rules");
        return -1;
    }

    // Special handling based on transition type
    if (strcmp(to_mode, MODE_PRODUCTION) == 0) {
        // For production transition, kill connections before final rules
        pid_t pid = fork();
        if (pid == 0) {  // Child process
            sleep(1);  // Brief delay to ensure parent finishes
            kill_existing_connections();
            exit(0);
        } else if (pid < 0) {
            log_message(LOG_ERR, "Failed to fork for connection cleanup");
            return -1;
        }
    } else if (strcmp(to_mode, MODE_MAINTENANCE) == 0) {
        log_message(LOG_INFO, "Waiting %d seconds before maintenance transition", TIMEOUT_SECONDS);
        sleep(TIMEOUT_SECONDS);
    }

    // Apply final rules
    snprintf(cmd, sizeof(cmd), "/usr/sbin/nft -f /etc/nftables-%s.conf", to_mode);
    ret = system(cmd);
    if (ret != 0) {
        log_message(LOG_ERR, "Failed to apply %s rules", to_mode);
        return -1;
    }

    return 0;
}

static int handle_production_transition(void) {
    char current_mode[32];
    pid_t child_pid;

    if (getuid() != SEARCHER_UID) {
        fprintf(stderr, "This command can only be run by the searcher user\n");
        return 1;
    }

    if (read_current_mode(current_mode, sizeof(current_mode)) != 0) {
        log_message(LOG_ERR, "Failed to read current mode");
        return 1;
    }

    if (strcmp(current_mode, MODE_MAINTENANCE) != 0) {
        log_message(LOG_ERR, "Can only transition to production from maintenance mode");
        release_lock();
        return 1;
    }

    // Fork to handle transition
    child_pid = fork();
    if (child_pid == 0) {  // Child process
        // Close stdin/stdout/stderr
        close(0);
        close(1);
        close(2);
        
        // Create new session
        setsid();

        log_message(LOG_INFO, "Initiating transition to production mode");

        if (write_mode(MODE_TRANSITION) != 0) {
            log_message(LOG_ERR, "Failed to set transition mode");
            exit(1);
        }

        if (execute_transition_script(MODE_MAINTENANCE, MODE_PRODUCTION) != 0) {
            log_message(LOG_ERR, "Transition failed");
            write_mode(MODE_MAINTENANCE);
            exit(1);
        }

        if (write_mode(MODE_PRODUCTION) != 0) {
            log_message(LOG_ERR, "Failed to set production mode");
            exit(1);
        }

        log_message(LOG_INFO, "Successfully transitioned to production mode");
        exit(0);
    } else if (child_pid > 0) {  // Parent process
        printf("Transition to production mode initiated\n");
        exit(0);
    } else {
        log_message(LOG_ERR, "Failed to fork for transition");
        return 1;
    }

    return 0;
}

static int handle_maintenance_transition(void) {
    char current_mode[32];

    if (read_current_mode(current_mode, sizeof(current_mode)) != 0) {
        log_message(LOG_ERR, "Failed to read current mode");
        return 1;
    }

    if (strcmp(current_mode, MODE_PRODUCTION) != 0) {
        log_message(LOG_ERR, "Can only transition to maintenance from production mode");
        release_lock();
        return 1;
    }

    log_message(LOG_INFO, "Initiating transition to maintenance mode");

    // Set transition mode first
    if (write_mode(MODE_TRANSITION) != 0) {
        log_message(LOG_ERR, "Failed to set transition mode");
        release_lock();
        return 1;
    }

    // Apply transition rules
    if (execute_transition_script(MODE_PRODUCTION, MODE_MAINTENANCE) != 0) {
        log_message(LOG_ERR, "Transition failed");
        write_mode(MODE_PRODUCTION);
        release_lock();
        return 1;
    }

    // Fork for the delay and final transition
    pid_t child_pid = fork();
    if (child_pid == 0) {  // Child process
        // Detach from parent
        setsid();
        
        // Wait for the timeout
        sleep(TIMEOUT_SECONDS);
        
        // Set final mode
        if (write_mode(MODE_MAINTENANCE) != 0) {
            log_message(LOG_ERR, "Failed to set maintenance mode");
            exit(1);
        }
        
        log_message(LOG_INFO, "Successfully transitioned to maintenance mode");
        exit(0);
    } else if (child_pid > 0) {  // Parent process
        log_message(LOG_INFO, "Transition initiated, system is now in transition mode");
        release_lock();
        return 0;
    } else {
        log_message(LOG_ERR, "Failed to fork for transition delay");
        write_mode(MODE_PRODUCTION);  // Revert on failure
        release_lock();
        return 1;
    }

    return 0;
}

int main(int argc, char *argv[]) {
    openlog("mode-controller", LOG_PID, LOG_DAEMON);

    if (argc < 2) {
        fprintf(stderr, "Usage: %s {transition-to-maintenance|to-production|status}\n", argv[0]);
        return 1;
    }

    if (strcmp(argv[1], "transition-to-maintenance") == 0) {
        return handle_maintenance_transition();
    } else if (strcmp(argv[1], "to-production") == 0) {
        return handle_production_transition();
    } else if (strcmp(argv[1], "status") == 0) {
        char mode[32];
        if (read_current_mode(mode, sizeof(mode)) == 0) {
            printf("Current mode: %s\n", mode);
            release_lock();
            return 0;
        }
        release_lock();
        return 1;
    }

    fprintf(stderr, "Unknown command: %s\n", argv[1]);
    return 1;
}
