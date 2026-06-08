import os, sys
os.chdir("/Users/hf/Desktop/MediatorK/docs")
sys.argv = [""]
import http.server, socketserver
handler = http.server.SimpleHTTPRequestHandler
with socketserver.TCPServer(("", 3030), handler) as httpd:
    httpd.serve_forever()
