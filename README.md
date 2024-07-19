So far, this layer adds the searcher's SSH pubkey to the authorized_keys file. 
It uses [dropbear SSH](https://matt.ucc.asn.au/dropbear/dropbear.html), a minimal SSH server with support for public key authentication. 
Because root and password login are disabled by default, the TDX machine can only be accessed via SSH and only by that searcher's ssh key pair. 

The meta-searcher layer appends:
- searcher-ssh-key package to meta-confidential-compute layer's cvm-initramfs.bb
- a new dropbear configuration to meta layer's dropbear that disables password logins

The meta-searcher layer is assigned priority = 30 to override configurations in other layers (meta-confidential-compute = 20).

The searcher-ssh-key package creates the .ssh directory and adds the searcher's SSH pubkey to the authorized_keys file to the root user.
The shell script is configured to run at the last stage of the init process. 

Note: some local networking commands add a static IP to enable testing via qemu. 

**flashbots internal testing**:

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
ssh -i ~/.ssh/id_rsa_princess princess@192.168.7.2
```