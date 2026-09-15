// статусы заказов → как они выглядят в UI
export const STATUS = {
  CREATED:    { label: 'Новый',     cls: 'created' },
  ASSIGNED:   { label: 'Назначен',  cls: 'assigned' },
  PICKED_UP:  { label: 'Забран',    cls: 'picked-up' },
  IN_TRANSIT: { label: 'В пути',    cls: 'in-transit' },
  DELIVERED:  { label: 'Доставлен', cls: 'delivered' },
  CANCELLED:  { label: 'Отменён',   cls: 'cancelled' },
};

export const STEPS = ['CREATED', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'];

const timeFormat = new Intl.DateTimeFormat('ru-RU', {
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
});

export function formatDate(iso) {
  return iso ? timeFormat.format(new Date(iso)) : '—';
}

export function shortId(id) {
  return id ? id.slice(0, 8) : '—';
}

export function statusBadge(status) {
  const meta = STATUS[status] || { label: status, cls: 'created' };
  return `<span class="badge badge-${meta.cls}">${meta.label}</span>`;
}

// прогресс из 5 точек; отменённый заказ показываем красной плашкой
export function stepper(status) {
  if (status === 'CANCELLED') {
    return '<div class="stepper"><span class="stepper-cancel">Заказ отменён</span></div>';
  }
  const current = STEPS.indexOf(status);
  const dots = STEPS.map((step, i) => `
    <div class="step ${i < current ? 'done' : ''} ${i === current ? 'current' : ''}">
      <span class="step-dot"></span>
      <span class="step-label">${STATUS[step].label}</span>
    </div>`).join('');
  return `<div class="stepper">${dots}</div>`;
}

export function toast(message, type = 'info') {
  const box = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = message;
  box.appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

// пользовательский ввод попадает в innerHTML — экранируем
export function escapeHtml(value) {
  const div = document.createElement('div');
  div.textContent = value ?? '';
  return div.innerHTML;
}

export function logoHtml() {
  return `
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M3 7l9-4 9 4v10l-9 4-9-4V7z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <path d="M3 7l9 4 9-4M12 11v10" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
    </svg>`;
}
