import { request } from '../api.js';
import { connect, subscribe, topics, isConnected } from '../ws.js';
import { navigate } from '../router.js';
import {
  STATUS, statusBadge, stepper, formatDate, shortId,
  toast, escapeHtml,
} from '../ui.js';
import { createMap, addMarker, drawRoute } from '../map.js';

const isActive = order => !['DELIVERED', 'CANCELLED'].includes(order.status);
const canCancel = order => ['CREATED', 'ASSIGNED'].includes(order.status);

// ==================== мои заказы ====================

export async function clientOrdersView(root) {
  const orders = await request('/api/v1/orders/me');
  orders.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  let selectedId = null;
  let courierSub = null;
  let currentMap = null;

  root.innerHTML = `
    <div class="split">
      <section>
        <h2>Мои заказы <span class="count">${orders.length}</span></h2>
        <div class="order-list" id="order-list"></div>
      </section>
      <section id="order-detail">
        <div class="empty-state">Выберите заказ из списка</div>
      </section>
    </div>`;

  drawList();

  // live-обновления статусов; без websocket страница работает в обычном режиме
  try {
    await connect();
    orders.filter(isActive).forEach(order => {
      subscribe(topics.orderStatus(order.id), onStatusUpdate);
    });
  } catch {
    toast('Нет связи с WebSocket — статусы не будут обновляться в реальном времени', 'error');
  }

  async function select(orderId) {
    selectedId = orderId;
    drawList();
    renderDetail(orders.find(o => o.id === orderId));
  }

  function drawList() {
    const list = root.querySelector('#order-list');

    if (!orders.length) {
      list.innerHTML = '<div class="empty-state">Заказов пока нет — создайте первый</div>';
      return;
    }

    list.innerHTML = orders.map(order => `
      <article class="order-card ${order.id === selectedId ? 'selected' : ''}" data-id="${order.id}">
        <div class="order-card-top">
          <code>#${shortId(order.id)}</code>
          ${statusBadge(order.status)}
        </div>
        <p class="order-route">${escapeHtml(order.pickupAddress)} → ${escapeHtml(order.deliveryAddress)}</p>
        <span class="muted small">${formatDate(order.createdAt)}</span>
      </article>`).join('');

    list.querySelectorAll('.order-card').forEach(card =>
      card.addEventListener('click', () => select(card.dataset.id)));
  }

  function renderDetail(order) {
    const detail = root.querySelector('#order-detail');

    detail.innerHTML = `
      <div class="card">
        <div class="detail-head">
          <div>
            <h3>Заказ <code>#${shortId(order.id)}</code></h3>
            <p class="muted small">Создан ${formatDate(order.createdAt)}</p>
          </div>
          <span id="detail-badge">${statusBadge(order.status)}</span>
        </div>
        <div id="detail-stepper">${stepper(order.status)}</div>
        <div class="route-points">
          <div class="route-point"><span class="dot dot-a"></span>${escapeHtml(order.pickupAddress)}</div>
          <div class="route-point"><span class="dot dot-b"></span>${escapeHtml(order.deliveryAddress)}</div>
        </div>
        ${order.description ? `<p class="order-desc">${escapeHtml(order.description)}</p>` : ''}
        <div class="map" id="detail-map"></div>
        ${canCancel(order) ? '<br><button class="btn btn-danger" id="cancel-btn">Отменить заказ</button>' : ''}
      </div>`;

    // Leaflet требует, чтобы контейнер уже был в DOM
    currentMap = createMap('detail-map', [
      { key: 'pickup', lat: order.pickupLat, lng: order.pickupLng, cls: 'marker-a', label: 'A' },
      { key: 'delivery', lat: order.deliveryLat, lng: order.deliveryLng, cls: 'marker-b', label: 'B' },
    ]).map;
    drawRoute(
      currentMap,
      { lat: order.pickupLat, lng: order.pickupLng },
      { lat: order.deliveryLat, lng: order.deliveryLng }
    );

    // если курьер уже назначен — следим за его перемещениями
    if (order.courierId) watchCourier(order.courierId);

    const cancelBtn = detail.querySelector('#cancel-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', async () => {
        try {
          await request(`/api/v1/orders/${order.id}`, { method: 'DELETE' });
          order.status = 'CANCELLED';
          toast('Заказ отменён', 'info');
          drawList();
          renderDetail(order);
        } catch (error) {
          toast(error.message, 'error');
        }
      });
    }
  }

  function watchCourier(courierId) {
    if (!currentMap || !isConnected()) return;

    // переподписываемся заново, чтобы не плодить дубли на один и тот же топик
    courierSub?.unsubscribe();
    let marker = null;

    courierSub = subscribe(topics.courierLocation(courierId), point => {
      if (!marker) {
        marker = addMarker(currentMap, {
          lat: point.latitude,
          lng: point.longitude,
          cls: 'marker-courier',
        });
      } else {
        marker.setLatLng([point.latitude, point.longitude]);
      }
      currentMap.panTo([point.latitude, point.longitude]);
    });
  }

  function onStatusUpdate(payload) {
    const order = orders.find(o => o.id === payload.orderId);
    if (!order) return;

    order.status = payload.newStatus;
    drawList();

    if (order.id === selectedId) {
      const badge = root.querySelector('#detail-badge');
      const steps = root.querySelector('#detail-stepper');
      if (badge) badge.innerHTML = statusBadge(order.status);
      if (steps) steps.innerHTML = stepper(order.status);

      // курьер мог появиться только что — начинаем следить
      if (order.courierId) watchCourier(order.courierId);
    }

    toast(`Заказ #${shortId(order.id)}: ${STATUS[order.status].label}`, 'success');
  }
}

// ==================== создание заказа ====================

export async function clientNewOrderView(root) {
  let pickup = null;
  let delivery = null;
  let pickupMarker = null;
  let deliveryMarker = null;
  let routeLine = null;

  root.innerHTML = `
    <div class="split">
      <section>
        <h2>Новый заказ</h2>
        <form class="card" id="order-form" style="margin-top:14px">
          <p class="form-hint">Кликните по карте, чтобы поставить точку А (забор) и точку Б (доставка)</p>
          <div class="mode-switch">
            <label><input type="radio" name="mode" value="pickup" checked>Точка А</label>
            <label><input type="radio" name="mode" value="delivery">Точка Б</label>
          </div>
          <label>Адрес забора
            <input name="pickupAddress" required placeholder="ул. Тверская, 1">
          </label>
          <p class="coords" id="pickup-coords">координаты не выбраны</p>
          <label>Адрес доставки
            <input name="deliveryAddress" required placeholder="Ленинский проспект, 10">
          </label>
          <p class="coords" id="delivery-coords">координаты не выбраны</p>
          <label>Комментарий
            <input name="description" placeholder="хрупкое, не кидать">
          </label>
          <button class="btn btn-primary btn-block" type="submit">Создать заказ</button>
        </form>
      </section>
      <section>
        <div class="map map-tall" id="pick-map"></div>
      </section>
    </div>`;

  const form = root.querySelector('#order-form');
  const { map } = createMap('pick-map');
  map.on('click', onMapClick);

  function onMapClick(event) {
    const mode = form.querySelector('input[name="mode"]:checked').value;
    const { lat, lng } = event.latlng;

    if (mode === 'pickup') {
      pickup = { lat, lng };
      pickupMarker?.remove();
      pickupMarker = addMarker(map, { ...pickup, cls: 'marker-a', label: 'A' });
      form.querySelector('#pickup-coords').textContent = `[${lat.toFixed(5)}, ${lng.toFixed(5)}]`;
    } else {
      delivery = { lat, lng };
      deliveryMarker?.remove();
      deliveryMarker = addMarker(map, { ...delivery, cls: 'marker-b', label: 'B' });
      form.querySelector('#delivery-coords').textContent = `[${lat.toFixed(5)}, ${lng.toFixed(5)}]`;
    }

    routeLine?.remove();
    if (pickup && delivery) routeLine = drawRoute(map, pickup, delivery);
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();

    if (!pickup || !delivery) {
      toast('Поставьте обе точки на карте', 'error');
      return;
    }

    try {
      const order = await request('/api/v1/orders', {
        method: 'POST',
        body: {
          pickupAddress: form.pickupAddress.value.trim(),
          pickupLat: pickup.lat,
          pickupLng: pickup.lng,
          deliveryAddress: form.deliveryAddress.value.trim(),
          deliveryLat: delivery.lat,
          deliveryLng: delivery.lng,
          description: form.description.value.trim() || null,
        },
      });
      toast(`Заказ #${shortId(order.id)} создан`, 'success');
      navigate('/client/orders');
    } catch (error) {
      toast(error.message, 'error');
    }
  });
}
