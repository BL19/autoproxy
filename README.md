# AutoProxy
A http proxy
## Idea
A http proxy that proxies to different websites depending on suburl of the request to the proxyserver

## Config
The config is a YAML config that is located in 'config.yml' in the same directory as the runnable.
```yaml
port: 8101
adresses:
  local:
    enabled: true
    url: "http://localhost"
    suburl: "/local"
  google:
    enabled: true
    url: "http://google.com"
    suburl: "/google"
```
