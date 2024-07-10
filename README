flashbots internal testing:

- to add to your yocto build, clone this repo and add as a layer in bblayers.conf file
- turn off debug-tweaks in cvm-initramfs.bb from the meta-confidential-compute layer which allows passwordless root logins

- to build & start locally:

```
cd poky
source oe-init-build-env
bitbake cvm-image-azure
runqemu cvm-image-azure wic nographic kvm ovmf qemuparams="-m 8G"
```

- to test:
  - open another terminal and SSH into our dev server
```
ssh-keygen -f "/home/ubuntu/.ssh/known_hosts" -R "192.168.7.2"
ssh -i ~/.ssh/id_rsa_princess root@192.168.7.2
```