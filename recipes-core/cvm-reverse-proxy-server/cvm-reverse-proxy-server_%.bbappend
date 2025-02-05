INHERIT:remove = "update-rc.d"

# Remove the installed init script after the parent recipe places it.
do_install:append() {
    rm -f "${D}${sysconfdir}/init.d/cvm-reverse-proxy-server-init" || true

    # remove empty /etc/init.d and /etc 
    rmdir --ignore-fail-on-non-empty "${D}${sysconfdir}/init.d" || true
    rmdir --ignore-fail-on-non-empty "${D}${sysconfdir}"         || true
}

# Only ship the proxy binary, not the init script
FILES:${PN} = "${bindir}/proxy-server"

# Override the postinstall script so it doesn’t fail at build time
pkg_postinst:${PN} () {
    exit 0
}