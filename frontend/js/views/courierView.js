import { request } from '../api.js';
import { connect, subscribe, topics } from '../ws.js';
import { navigate } from '../router.js';
import {
  STATUS, statusBadge, stepper, shortId, toast, escapeHtml,
} from '../ui.js';
import { createMap } from '../map.js';

// какую кнопку показывать для следующего шага
const NEXT_ACTION = {
  ASSIGNED:  { status: 'PICKED_UP',  label: 'Забрал посылку' },
  PICKED_UP: { status: 'IN_TRANSIT', label: 'Выехал к клиенту' },
  IN_TRANSIT: { status: 'DELIVERED', label: 'Доставлен' },
};

// ==================== доступные заказы ====================

export async function courierAvailableView(root) {
  let orders = await request('/api/v1/courier/orders/available');

  root.innerHTML = `
    <div class="section-head">
      <h2>Доступные заказы <span class="count" id="available-count"></span></h2>
      <p class="muted small">новые заказы появляются здесь автоматически</p>
    </div>
    <div class="card-grid" id="available-list"></div>`;

  drawList();
  try {
    await connect();
    subscribe(topics.availableOrders(), async () => {
      toast('Появился новый заказ', 'info');
      // событие содержит только адреса — перечитываем список целиком
      orders = await request('/api/v1/courier/orders/available');
      drawList();
    });
  } catch {
    // без websocket лента работает в режиме ручного обновления
  }

  function drawList() {
    root.querySelector('#available-count').textContent = orders.length;
    const list = root.querySelector('#available-list');

    if (!orders.length) {
      list.innerHTML = '<div class="empty-state" style="grid-column:1/-1">Свободных заказов нет — загляните позже</div>';
      return;
    }

    list.innerHTML = orders.map(order => `
      <div class="card">
        <div class="order-card-top">
          <code>#${shortId(order.id)}</code>
          ${statusBadge(order.status)}
        </div>
        <div class="route-points">
          <div class="route-point"><span class="dot dot-a"></span>${escapeHtml(order.pickupAddress)}</div>
          <div class="route-point"><span class="dot dot-b"></span>${escapeHtml(order.deliveryAddress)}</div>
        </div>
        ${order.description ? `<p class="order-desc">${escapeHtml(order.description)}</p>` : ''}
        <button class="btn btn-primary btn-block accept-btn" data-id="${order.id}">Взять заказ</button>
      </div>`).join('');

    list.querySelectorAll('.accept-btn').forEach(btn =>
      btn.addEventListener('click', () => accept(btn.dataset.id)));
  }

  async function accept(orderId) {
    try {
      await request(`/api/v1/courier/orders/${orderId}/accept`, { method: 'PATCH' });
      toast(`Заказ #${shortId(orderId)} ваш`, 'success');
      navigate('/courier/active');
    } catch (error) {
      // кто-то из курьеров мог перехватить заказ раньше
      toast(error.message, 'error');
      orders = await request('/api/v1/courier/orders/available');
      drawList();
    }
  }
}

// ==================== заказы в работе + смена ====================

const PING_INTERVAL = 3000;
const CITY_CENTER = { lat: 55.751244, lng: 37.6184 };

let shiftTimer = null;
let shiftProgress = 0;
let shiftPoint = null;

export async function courierActiveView(root) {
  let orders = await request('/api/v1/courier/orders/active');

  root.innerHTML = `
    <div class="section-head">
      <h2>Заказы в работе <span class="count" id="active-count"></span></h2>
      <button class="btn ${shiftTimer ? 'btn-danger' : 'btn-success'}" id="shift-btn"></button>
    </div>
    <div class="card-grid" id="active-list"></div>
    <p class="shift-status" id="shift-status"></p>`;

  drawList();
  updateShiftUi();

  // свои статусы тоже обновляем в реальном времени (на случай гонки вкладок)
  try {
    await connect();
    orders.forEach(order => {
      if (NEXT_ACTION[order.status]) {
        subscribe(topics.orderStatus(order.id), onStatusUpdate);
      }
    });
  } catch {
    // смена и кнопки статусов работают и без websocket
  }

  root.querySelector('#shift-btn').addEventListener('click', toggleShift);

  function drawList() {
    root.querySelector('#active-count').textContent = orders.length;
    const list = root.querySelector('#active-list');

    if (!orders.length) {
      list.innerHTML = `
        <div class="empty-state" style="grid-column:1/-1">
          Нет активных заказов. <a href="#/courier/available">Взять из доступных</a>
        </div>`;
      return;
    }

    list.innerHTML = orders.map(order => {
      const next = NEXT_ACTION[order.status];
      return `
        <div class="card">
          <div class="order-card-top">
            <code>#${shortId(order.id)}</code>
            ${statusBadge(order.status)}
          </div>
          <div class="route-points">
            <div class="route-point"><span class="dot dot-a"></span>${escapeHtml(order.pickupAddress)}</div>
            <div class="route-point"><span class="dot dot-b"></span>${escapeHtml(order.deliveryAddress)}</div>
          </div>
          ${stepper(order.status)}
          ${next
            ? `<button class="btn btn-primary btn-block next-btn" data-id="${order.id}" data-next="${next.status}">
                 ${next.label}
               </button>`
            : ''}
        </div>`;
    }).join('');

    list.querySelectorAll('.next-btn').forEach(btn =>
      btn.addEventListener('click', () => advance(btn.dataset.id, btn.dataset.next)));
  }

  async function advance(orderId, nextStatus) {
    try {
      const updated = await request(`/api/v1/courier/orders/${orderId}/status`, {
        method: 'PATCH',
        body: { status: nextStatus },
      });

      const order = orders.find(o => o.id === orderId);
      Object.assign(order, updated);
      toast(`Заказ #${shortId(orderId)}: ${STATUS[updated.status].label}`, 'success');
      drawList();

      if (updated.status === 'DELIVERED') {
        orders = orders.filter(o => o.id !== orderId);
        drawList();
      }
    } catch (error) {
      toast(error.message, 'error');
    }
  }

  function onStatusUpdate(payload) {
    const order = orders.find(o => o.id === payload.orderId);
    if (!order) return;
    order.status = payload.newStatus;
    drawList();
  }

  // ---- симулятор движения ----
  // настоящий GPS в демо неотслеживаем, поэтому курьер "едет" по маршруту
  // первого активного заказа, а без него — кружит вокруг центра города
  function toggleShift() {
    if (shiftTimer) {
      clearInterval(shiftTimer);
      shiftTimer = null;
      updateShiftUi();
      return;
    }

    const order = orders.find(o => o.status !== 'DELIVERED');
    const route = order
      ? { from: { lat: order.pickupLat, lng: order.pickupLng }, to: { lat: order.deliveryLat, lng: order.deliveryLng } }
      : null;
    shiftProgress = 0;

    shiftTimer = setInterval(async () => {
      shiftPoint = route ? alongRoute(route) : aroundCity();
      shiftProgress += 0.08;

      try {
        await request('/api/v1/courier/location', {
          method: 'POST',
          body: { latitude: shiftPoint.lat, longitude: shiftPoint.lng },
        });
        root.querySelector('#shift-status').textContent =
          `передаю координаты: [${shiftPoint.lat.toFixed(5)}, ${shiftPoint.lng.toFixed(5)}]`;
        updateShiftUi(true);
      } catch {
        root.querySelector('#shift-status').textContent = 'не удалось отправить координаты';
      }
    }, PING_INTERVAL);

    updateShiftUi();
  }

  function alongRoute({ from, to }) {
    // идём туда-обратно между точками А и Б
    const t = (Math.sin(shiftProgress) + 1) / 2;
    return {
      lat: from.lat + (to.lat - from.lat) * t,
      lng: from.lng + (to.lng - from.lng) * t,
    };
  }

  function aroundCity() {
    const angle = shiftProgress;
    return {
      lat: CITY_CENTER.lat + 0.012 * Math.sin(angle),
      lng: CITY_CENTER.lng + 0.012 * Math.cos(angle),
    };
  }

  function updateShiftUi(running = shiftTimer !== null) {
    const btn = root.querySelector('#shift-btn');
    btn.textContent = running ? 'Закончить смену' : 'Выйти на линию';
    btn.className = `btn ${running ? 'btn-danger' : 'btn-success'}`;
    if (!running) {
      root.querySelector('#shift-status').textContent = '';
    }
  }
}

// роутер вызывает при уходе со страницы, чтобы курьер не "ехал" в фоне
export function stopShift() {
  if (shiftTimer) {
    clearInterval(shiftTimer);
    shiftTimer = null;
  }
}
