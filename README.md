Butterfly-proxy
===============

a simple reverse proxy server which can distinguish 
http and windows remote desktop, which is to say, 
a single 80 port can be used to serve http page 
and at the same time, allow remote desktop client to connect  

how to run
----------
1. modify  com.butterfly.ProxyDecoder 
to point to the target http server and target window machine
2. mvn package
3. ./script/run.sh

caution
-------
no security is enforced
