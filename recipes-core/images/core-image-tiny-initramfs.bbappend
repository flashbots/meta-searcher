include ${@bb.utils.contains('DISTRO_FEATURES', 'searcher', 'core-image-tiny-initramfs.inc', '', d)}
