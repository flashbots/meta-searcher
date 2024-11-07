# Load specific modules
KERNEL_MODULE_AUTOLOAD += "ip_tables xt_multiport"
KERNEL_FEATURES:append:tdx=" features/netfilter/netfilter.scc"
