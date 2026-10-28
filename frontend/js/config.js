const devPorts = ['5500', '8000', '8081', '3001'];

export const API_BASE = devPorts.includes(location.port) ? 'http://localhost:8080' : '';
export const WS_URL = (API_BASE || location.origin).replace(/^http/, 'ws') + '/ws';
