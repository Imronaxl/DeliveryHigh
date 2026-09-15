import { request } from '../api.js';
import { saveSession } from '../auth.js';
import { navigate, homeFor } from '../router.js';
import { toast, logoHtml } from '../ui.js';

export function loginView(root) {
  root.innerHTML = `
    <div class="auth-wrap">
      <form class="card auth-card" id="login-form">
        <div class="auth-logo">${logoHtml()} DeliveryFlow</div>
        <h1>Вход</h1>
        <label>Email
          <input name="email" type="email" required placeholder="you@example.com">
        </label>
        <label>Пароль
          <input name="password" type="password" required placeholder="минимум 6 символов">
        </label>
        <p class="form-error" hidden></p>
        <button class="btn btn-primary btn-block" type="submit">Войти</button>
        <p class="auth-switch">Нет аккаунта? <a href="#/register">Зарегистрироваться</a></p>
      </form>
    </div>`;

  const form = root.querySelector('#login-form');

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const errorBox = form.querySelector('.form-error');
    errorBox.hidden = true;

    try {
      const auth = await request('/api/v1/auth/login', {
        method: 'POST',
        body: {
          email: form.email.value.trim(),
          password: form.password.value,
        },
      });
      saveSession(auth);
      toast(`С возвращением, ${auth.name}`, 'success');
      navigate(homeFor(auth.role));
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

const ROLES = [
  { value: 'CLIENT', title: 'Клиент', hint: 'Создаю заказы' },
  { value: 'COURIER', title: 'Курьер', hint: 'Вожу посылки' },
  { value: 'ADMIN', title: 'Админ', hint: 'Вижу всё' },
];

export function registerView(root) {
  root.innerHTML = `
    <div class="auth-wrap">
      <form class="card auth-card" id="register-form">
        <div class="auth-logo">${logoHtml()} DeliveryFlow</div>
        <h1>Регистрация</h1>

        <div class="role-cards">
          ${ROLES.map((role, i) => `
            <label class="role-card">
              <input type="radio" name="role" value="${role.value}" ${i === 0 ? 'checked' : ''}>
              <span class="role-title">${role.title}</span>
              <span class="role-hint">${role.hint}</span>
            </label>`).join('')}
        </div>

        <label>Имя
          <input name="name" required placeholder="Как вас зовут">
        </label>
        <label>Email
          <input name="email" type="email" required placeholder="you@example.com">
        </label>
        <label>Пароль
          <input name="password" type="password" required minlength="6" placeholder="минимум 6 символов">
        </label>
        <label>Телефон
          <input name="phone" placeholder="необязательно">
        </label>
        <p class="form-error" hidden></p>
        <button class="btn btn-primary btn-block" type="submit">Создать аккаунт</button>
        <p class="auth-switch">Уже есть аккаунт? <a href="#/login">Войти</a></p>
      </form>
    </div>`;

  const form = root.querySelector('#register-form');

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const errorBox = form.querySelector('.form-error');
    errorBox.hidden = true;

    try {
      const auth = await request('/api/v1/auth/register', {
        method: 'POST',
        body: {
          name: form.name.value.trim(),
          email: form.email.value.trim(),
          password: form.password.value,
          phone: form.phone.value.trim() || null,
          role: form.role.value,
        },
      });
      saveSession(auth);
      toast(`Добро пожаловать, ${auth.name}!`, 'success');
      navigate(homeFor(auth.role));
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}
