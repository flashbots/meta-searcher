SUMMARY = "Lighthouse"
DESCRIPTION = "Lighthouse is an Ethereum consensus client written in Rust"
HOMEPAGE = "https://lighthouse.sigmaprime.io/"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=139bc4b5f578ecacea78dc7b7ad3ed3c"

# Inherit the cargo_bin class to handle Rust/Cargo builds
# Inherit the update-rc.d class to install the service as a system service
inherit cargo_bin update-rc.d

# Define dependencies
DEPENDS += "openssl-native zlib-native postgresql"

# Add the init script to the source URI
SRC_URI += "file://init"
# Set the name of the init script
INITSCRIPT_NAME = "lighthouse"
# Set the init script parameters (run at boot time with default priority at order level 95)
INITSCRIPT_PARAMS = "defaults 95"

# Set the source directory
S = "${WORKDIR}/git"

# Specify the toolchain to use
TOOLCHAIN = "clang"

# Rust flags for build configuration
# Disable CPU-specific optimizations for better portability
RUSTFLAGS += "-C target-cpu=generic"

# Disable Link Time Optimization
RUSTFLAGS += "-C lto=off"

# Add zlib and PostgreSQL to the linker flags
RUSTFLAGS += "-L ${STAGING_LIBDIR_NATIVE} -l z -l pq"

# Flags for reproducible builds
# Compress debug sections
RUSTFLAGS += "-C link-arg=-Wl,--compress-debug-sections=zlib"
# Remove build ID for better reproducibility
RUSTFLAGS += "-C link-arg=-Wl,--build-id=none"
# Use a consistent symbol mangling version
RUSTFLAGS += "-C symbol-mangling-version=v0"
# Remap the build path to a generic path
RUSTFLAGS += "--remap-path-prefix=${WORKDIR}=/usr/src/lighthouse"

# Cargo profile settings for release builds
# Optimize for size
CARGO_PROFILE_RELEASE_OPT_LEVEL = "z"
# Use a single codegen unit for better optimization
CARGO_PROFILE_RELEASE_CODEGEN_UNITS = "1"
# Abort on panic for smaller binary size
CARGO_PROFILE_RELEASE_PANIC = "abort"
# Disable incremental compilation for reproducibility
CARGO_PROFILE_RELEASE_INCREMENTAL = "false"

# Set Cargo home directory
CARGO_HOME = "${WORKDIR}/cargo_home"
export CARGO_HOME

# Define the target subdirectory for Cargo build artifacts
CARGO_TARGET_SUBDIR = "x86_64-unknown-linux-gnu/release"

# Python function to set SOURCE_DATE_EPOCH for reproducible builds
python do_set_source_date_epoch() {
    import subprocess
    import time

    # Get the commit date of the latest commit
    cmd = f"git -C {d.getVar('S')} log -1 --pretty=%ct"
    commit_date = subprocess.check_output(cmd, shell=True).decode('utf-8').strip()

    # Set SOURCE_DATE_EPOCH to the commit date
    d.setVar('SOURCE_DATE_EPOCH', commit_date)

    # Log the date for debugging
    human_date = time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime(int(commit_date)))
    bb.note(f"Set SOURCE_DATE_EPOCH to {commit_date} ({human_date} UTC)")
}

# Add the source date epoch task to run after unpacking and before compiling
addtask set_source_date_epoch after do_unpack before do_compile

# Allow network access during compilation (needed for cargo to fetch dependencies)
do_compile[network] = "1"

# Set environment variables before compilation
do_compile:prepend() {
    # Use system git for fetching
    export CARGO_NET_GIT_FETCH_WITH_CLI=true
    # Configure OpenSSL
    export OPENSSL_STATIC=1
    export OPENSSL_DIR="${STAGING_DIR_NATIVE}/usr"
    # Configure zlib
    export ZLIB_DIR="${STAGING_DIR_NATIVE}/usr"
    # Configure PostgreSQL
    export PQ_LIB_DIR="${STAGING_LIBDIR_NATIVE}"
    export PQ_INCLUDE_DIR="${STAGING_INCDIR_NATIVE}"
}

# Additional Cargo build arguments
EXTRA_OECARGO_BUILDARGS += "--features modern,postgres --frozen"

# Installation task
do_install() {
    # Create the binary directory in the target root filesystem
    install -d ${D}${bindir}
    # Install the lighthouse binary
    install -m 0755 ${B}/${CARGO_TARGET_SUBDIR}/lighthouse ${D}${bindir}/lighthouse
    # Create the init script directory in the target root filesystem
    install -d ${D}${sysconfdir}/init.d
    # Install the init script
    install -m 0755 ${THISDIR}/init ${D}${sysconfdir}/init.d/lighthouse
}

# Ensure do_install task is re-run if CARGO_TARGET_SUBDIR changes
do_install[vardeps] += "CARGO_TARGET_SUBDIR"

# Define the files to be included in the package
FILES:${PN} += "${bindir}/lighthouse"
FILES:${PN} += "${sysconfdir}/init.d/lighthouse"
