
## "Random by IP (match proxy)" button
Fetches the device's current public IP location from ipinfo.io (HTTPS) — which,
if your proxy/VPN is system-wide, is the PROXY's IP -> its city — adds a small
random jitter (~2.5 km) so it's a natural point each time, fills the fields and
saves automatically. Then force-stop the target app. Needs INTERNET permission;
if the proxy is app-scoped (not system-wide) the lookup may see your real IP.
