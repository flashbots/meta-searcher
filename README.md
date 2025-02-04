Meta-searcher implements three key features for bottom of block backrunning: 

1. **Network namespaces and firewall rules** that enforce a searcher cannot SSH into the container while state diffs are being streamed in, and the only way information can leave is through the block builder’s endpoints.
2. A **log delay** **script that enforces a two-minute (~10 block) delay until the searcher can view their bot’s logs. 
3. **Mode switching** which allows a searcher to toggle between production and maintenance modes, where the SSH connection is cut and restored respectively. 

Together, they provide the “no-frontrunning” guarantee to order flow providers while balancing searcher bot visibility and maintenance.

Docs: https://flashbots.notion.site/Bob-V2-Image-Guide-1506b4a0d87680b2979de36288b48d9a?pvs=4

![image](https://github.com/user-attachments/assets/aaad8a4e-f640-4a94-b16f-657eb3ff6bdb)
