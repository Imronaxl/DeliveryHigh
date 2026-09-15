// когда приложение раздаётся nginx'ом (docker-compose), API живёт на том же
// origin и запросы идут через прокси /api и /ws; для dev-серверов
// (live server, python http.server) ходим напрямую на бэкенд
const devPorts = ['5500', '8000', '8081', '3001'];

export const API_BASE = devPorts.includes(location.port) ? 'http://localhost:8080' : '';
export const WS_URL = (API_BASE || location.origin) + '/ws';
