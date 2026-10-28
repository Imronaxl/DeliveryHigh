import assert from 'node:assert/strict';

const base = process.env.API_URL || 'http://localhost:8080';
const suffix = Date.now();
const sockets = [];

async function request(path, { token, method = 'GET', body, status = 200 } = {}) {
  const response = await fetch(base + '/api/v1' + path, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  assert.equal(response.status, status, `${method} ${path}: ${text}`);
  return text ? JSON.parse(text) : null;
}

function register(label, role) {
  return request('/auth/register', { method: 'POST', body: { email: `${label}-${suffix}@example.com`, password: 'smoke-password-123', name: label, role } });
}

async function connect(token) {
  const socket = new WebSocket(base.replace(/^http/, 'ws') + '/ws');
  sockets.push(socket);
  const handlers = new Set();
  let connected;
  const ready = new Promise((resolve, reject) => {
    connected = resolve;
    socket.addEventListener('error', reject);
  });
  socket.addEventListener('open', () => socket.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\0`));
  let buffer = '';
  socket.addEventListener('message', event => {
    buffer += event.data;
    let end;
    while ((end = buffer.indexOf('\0')) !== -1) {
      const frame = buffer.slice(0, end).replace(/^\n+/, '');
      buffer = buffer.slice(end + 1);
      if (frame.startsWith('CONNECTED')) connected();
      if (frame.startsWith('ERROR')) throw new Error(frame);
      if (!frame.startsWith('MESSAGE')) continue;
      const split = frame.indexOf('\n\n');
      const headers = Object.fromEntries(frame.slice(0, split).split('\n').slice(1).map(line => { const colon = line.indexOf(':'); return [line.slice(0, colon), line.slice(colon + 1)]; }));
      const body = JSON.parse(frame.slice(split + 2));
      for (const handler of handlers) handler(headers.destination, body);
    }
  });
  await Promise.race([ready, new Promise((_, reject) => { const timer = setTimeout(() => reject(new Error('STOMP connect timeout')), 15000); timer.unref(); })]);
  let subscription = 0;
  return {
    subscribe(destination) { socket.send(`SUBSCRIBE\nid:s${subscription++}\ndestination:${destination}\nack:auto\n\n\0`); },
    next(destination, predicate = () => true) {
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => { handlers.delete(handler); reject(new Error(`Event timeout: ${destination}`)); }, 15000);
        const handler = (topic, body) => { if (topic === destination && predicate(body)) { clearTimeout(timer); handlers.delete(handler); resolve(body); } };
        handlers.add(handler);
      });
    },
  };
}

try {
  const [client, first, second] = await Promise.all([register('client', 'CLIENT'), register('first', 'COURIER'), register('second', 'COURIER')]);
  const courierSocket = await connect(first.accessToken);
  courierSocket.subscribe('/topic/orders/available');
  const created = courierSocket.next('/topic/orders/available', event => !event.newStatus);
  const order = await request('/orders', { token: client.accessToken, method: 'POST', status: 201, body: { pickupAddress: 'A', pickupLat: 55.75, pickupLng: 37.61, deliveryAddress: 'B', deliveryLat: 55.76, deliveryLng: 37.62 } });
  assert.equal((await created).orderId, order.id);
  const statuses = `/topic/orders/${order.id}/status`;
  const clientSocket = await connect(client.accessToken);
  clientSocket.subscribe(statuses);
  const assigned = clientSocket.next(statuses, event => event.newStatus === 'ASSIGNED');
  const responses = await Promise.all([first, second].map(async courier => {
    const response = await fetch(`${base}/api/v1/courier/orders/${order.id}/accept`, { method: 'PATCH', headers: { Authorization: `Bearer ${courier.accessToken}` } });
    return { courier, status: response.status };
  }));
  assert.deepEqual(responses.map(value => value.status).sort(), [200, 400]);
  const winner = responses.find(value => value.status === 200).courier;
  await assigned;
  const locations = `/topic/courier/${winner.userId}/location`;
  clientSocket.subscribe(locations);
  const moved = clientSocket.next(locations, event => event.latitude === 55.755);
  await request('/courier/location', { token: winner.accessToken, method: 'POST', status: 204, body: { latitude: 55.755, longitude: 37.615 } });
  assert.equal((await moved).courierId, winner.userId);
  for (const status of ['PICKED_UP', 'IN_TRANSIT', 'DELIVERED']) {
    const changed = clientSocket.next(statuses, event => event.newStatus === status);
    await request(`/courier/orders/${order.id}/status`, { token: winner.accessToken, method: 'PATCH', body: { status } });
    await changed;
  }
  await request(`/orders/${order.id}`, { token: client.accessToken, method: 'DELETE', status: 400 });
  const refreshed = await request('/auth/refresh', { method: 'POST', body: { refreshToken: client.refreshToken } });
  assert.equal((await request(`/orders/${order.id}`, { token: refreshed.accessToken })).status, 'DELIVERED');
  if (process.env.ADMIN_EMAIL && process.env.ADMIN_PASSWORD) {
    const admin = await request('/auth/login', { method: 'POST', body: { email: process.env.ADMIN_EMAIL, password: process.env.ADMIN_PASSWORD } });
    const history = await request(`/admin/orders/${order.id}/history`, { token: admin.accessToken });
    assert.equal(history.length, 5);
  }
  console.log('Delivery lifecycle, concurrent assignment, Kafka, Redis, STOMP and refresh: passed');
} finally {
  sockets.forEach(socket => socket.close());
}
