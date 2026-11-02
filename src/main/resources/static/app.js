'use strict';

const $ = (id) => document.getElementById(id);
const page = document.body.dataset.page;
const session = {
  token: sessionStorage.getItem('recipehubToken') || '',
  username: sessionStorage.getItem('recipehubUsername') || '',
  role: ''
};
let categories = [];
let ingredients = [];
let currentRecipe = null;
let onlyMine = false;
let notificationTimer;

const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[char]));
const imageUrl = (url) => typeof url === 'string' && (/^https?:\/\//i.test(url) || /^\/images\/[\w./-]+$/.test(url)) ? url : '';
const imageMarkup = (recipe, className = 'recipe-art') => {
  const image = imageUrl(recipe.imageUrl);
  return `<div class="${className}">${image
    ? `<img src="${escapeHtml(image)}" alt="${escapeHtml(recipe.name)}" loading="lazy">`
    : '🍳'}</div>`;
};

function setSession(token, username) {
  session.token = token || '';
  session.username = username || '';
  if (!token) session.role = '';
  if (token) {
    sessionStorage.setItem('recipehubToken', token);
    sessionStorage.setItem('recipehubUsername', username);
  } else {
    sessionStorage.removeItem('recipehubToken');
    sessionStorage.removeItem('recipehubUsername');
  }
  document.querySelectorAll('[data-auth]').forEach((el) => { el.hidden = !token; });
  document.querySelectorAll('[data-guest]').forEach((el) => { el.hidden = Boolean(token); });
  if ($('loginState')) $('loginState').textContent = username;
  document.querySelectorAll('[data-admin]').forEach((el) => { el.hidden = !token || session.role !== 'ADMIN'; });
}

function toast(message, isError = false) {
  const status = $('status');
  if (!status) return;
  status.textContent = message;
  status.classList.toggle('error', isError);
  status.hidden = false;
  clearTimeout(notificationTimer);
  notificationTimer = setTimeout(() => { status.hidden = true; }, 6000);
}

function formError(message) {
  const error = $('formMessage');
  if (error) { error.textContent = message; error.hidden = false; }
  toast(message, true);
}
function clearFormError() {
  if ($('formMessage')) $('formMessage').hidden = true;
}
function needLogin() {
  const next = encodeURIComponent(location.pathname + location.search);
  location.href = `/login.html?next=${next}`;
}
function afterAuthPath() {
  const next = new URLSearchParams(location.search).get('next');
  return next && next.startsWith('/') && !next.startsWith('//') && !next.includes('\\') ? next : '/';
}

async function api(path, { auth = true, ...options } = {}) {
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) };
  if (auth && session.token) headers.Authorization = `Bearer ${session.token}`;
  let response;
  try {
    response = await fetch(path, { ...options, headers });
  } catch {
    throw new Error('Không kết nối được máy chủ. Kiểm tra Spring Boot/Docker.');
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && auth) {
      setSession('', '');
      throw new Error('HTTP 401 — Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.');
    }
    const error = new Error(`HTTP ${response.status} — ${data?.message || "Yêu cầu không được chấp nhận"}`);
    error.status = response.status;
    throw error;
  }
  return data;
}

async function loadCatalog() {
  [categories, ingredients] = await Promise.all([
    api('/api/categories', { auth: false }), api('/api/ingredients', { auth: false })
  ]);
}

function setOptions(select, entries, placeholder, selected = '') {
  select.replaceChildren();
  select.add(new Option(placeholder, ''));
  entries.forEach((entry) => select.add(new Option(entry.name, String(entry.id))));
  select.value = String(selected || '');
}

function ownRecipe(recipe) {
  return Boolean(session.token && session.username && session.username === recipe.username);
}

function renderCard(recipe) {
  return `<article class="recipe-card">${imageMarkup(recipe)}<div class="recipe-content">
    <span class="pill">${escapeHtml(recipe.categoryName)}</span>
    <h3>${escapeHtml(recipe.name)}</h3><div class="recipe-author">Chia sẻ bởi ${escapeHtml(recipe.username)}</div>
    <p>${escapeHtml(recipe.description || 'Khám phá nguyên liệu và cách làm món ăn.')}</p>
    <div class="recipe-meta"><span>⏱ ${Number(recipe.cookingTime) || 0} phút</span><span>🍽 ${Number(recipe.servings) || 1} phần</span></div>
    <div class="recipe-actions"><a class="button secondary" href="/recipe.html?id=${recipe.id}">Xem công thức</a>
    ${ownRecipe(recipe) ? `<a class="button outline" href="/editor.html?id=${recipe.id}">Sửa</a>` : ''}</div>
  </div></article>`;
}

async function loadRecipes() {
  const params = new URLSearchParams();
  const keyword = $('search').value.trim();
  if (keyword) params.set('keyword', keyword);
  if ($('filterCategory').value) params.set('categoryId', $('filterCategory').value);
  const allRecipes = await api('/api/recipes' + (params.size ? `?${params}` : ''), { auth: false });
  const recipes = onlyMine ? allRecipes.filter(ownRecipe) : allRecipes;
  $('recipeCount').textContent = `${recipes.length} công thức`;
  $('recipeCards').innerHTML = recipes.length ? recipes.map(renderCard).join('') :
    '<p class="empty">Chưa có công thức phù hợp. Hãy thử từ khóa hoặc danh mục khác.</p>';
}

async function home() {
  await loadCatalog();
  setOptions($('filterCategory'), categories, 'Tất cả danh mục');
  await loadRecipes();
  $('myRecipesBtn').addEventListener('click', async () => {
    onlyMine = !onlyMine;
    $('myRecipesBtn').textContent = onlyMine ? 'Hiển thị tất cả' : 'Chỉ bài của tôi';
    try { await loadRecipes(); } catch (error) { toast(error.message, true); }
  });
  $('searchForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    try { await loadRecipes(); } catch (error) { toast(error.message, true); }
  });
}

async function authentication() {
  if (session.token) { location.href = '/'; return; }
  const mode = page;
  const form = $(`${mode}Form`);
  form.addEventListener('submit', async (event) => {
    event.preventDefault(); clearFormError();
    if (!form.reportValidity()) return;
    const submit = form.querySelector('button[type="submit"]');
    const body = Object.fromEntries(new FormData(form));
    submit.disabled = true;
    try {
      const result = await api(`/api/auth/${mode}`, { auth: false, method: 'POST', body: JSON.stringify(body) });
      setSession(result.accessToken, result.username);
      location.href = afterAuthPath();
    } catch (error) {
      formError(error.message);
      submit.disabled = false;
    }
  });
}

async function detail() {
  const id = Number(new URLSearchParams(location.search).get('id'));
  if (!Number.isSafeInteger(id) || id < 1) { $('detailContent').innerHTML = '<p class="empty">ID công thức không hợp lệ.</p>'; return; }
  const recipe = await api(`/api/recipes/${id}`, { auth: false });
  currentRecipe = recipe;
  document.title = `${recipe.name} | RecipeHub Enterprise`;
  const list = (recipe.ingredients || []).map((item) => `<li><strong>${escapeHtml(item.ingredientName)}</strong><span>${escapeHtml(item.quantity)} ${escapeHtml(item.unit)}</span></li>`).join('');
  $('detailContent').innerHTML = `<div class="detail-top">${imageMarkup(recipe, 'detail-photo')}
    <div class="detail-copy"><span class="pill">${escapeHtml(recipe.categoryName)}</span><h1>${escapeHtml(recipe.name)}</h1>
    <p>${escapeHtml(recipe.description || 'Một công thức được chia sẻ từ cộng đồng RecipeHub.')}</p>
    <p class="recipe-author">Tác giả: ${escapeHtml(recipe.username)} · ${escapeHtml(String(recipe.createdAt || '').slice(0, 10))}</p>
    <div class="detail-meta"><span>⏱ ${Number(recipe.cookingTime) || 0} phút</span><span>🍽 ${Number(recipe.servings) || 1} người</span></div>
    <div class="detail-actions">${ownRecipe(recipe) ? `<a href="/editor.html?id=${id}" class="button">✏️ Sửa công thức</a><button type="button" id="deleteRecipe" class="button outline">🗑️ Xóa</button>` :
      `<a href="/login.html?next=${encodeURIComponent('/editor.html')}" class="button outline" data-guest ${session.token ? 'hidden' : ''}>Đăng nhập để chia sẻ</a>`}</div></div></div>
    <div class="detail-grid"><section class="panel"><h2>🥕 Nguyên liệu</h2>
    ${list ? `<ul class="ingredient-list">${list}</ul>` : '<p class="muted">Chưa có nguyên liệu.</p>'}</section>
    <section class="panel"><h2>👨‍🍳 Các bước chế biến</h2><p class="preline">${escapeHtml(recipe.instructions)}</p></section></div>`;
  await initComments(id);
  if ($('deleteRecipe')) $('deleteRecipe').addEventListener('click', async () => {
    if (!confirm(`Bạn chắc chắn muốn xóa công thức "${recipe.name}"?`)) return;
    try {
      await api(`/api/recipes/${id}`, { method: 'DELETE' });
      location.href = '/#recipes';
    } catch (error) { toast(error.message, true); }
  });
}

function ingredientRow(initial = {}) {
  const row = document.createElement('div');
  row.className = 'ingredient-row';
  row.innerHTML = `<label>Nguyên liệu *<select class="ingredient-choice" required></select></label>
    <label>Số lượng *<input type="number" class="ingredient-qty" step="0.01" min="0.01" max="99999999" required value="${Number(initial.quantity) > 0 ? Number(initial.quantity) : 1}"></label>
    <label>Đơn vị *<input class="ingredient-unit" required maxlength="30" value="${escapeHtml(initial.unit || 'gram')}" placeholder="gram, quả, muỗng..."></label>
    <button class="remove" type="button" title="Xóa dòng nguyên liệu">✕ Xóa</button>`;
  setOptions(row.querySelector('.ingredient-choice'), ingredients, '-- Chọn nguyên liệu --', initial.ingredientId);
  row.querySelector('.remove').addEventListener('click', () => {
    row.remove();
    if (!$('ingredientRows').children.length) ingredientRow();
  });
  $('ingredientRows').append(row);
  return row;
}

function readIngredients() {
  const rows = [...$('ingredientRows').querySelectorAll('.ingredient-row')];
  const used = new Set();
  const result = [];
  for (const row of rows) {
    const id = Number(row.querySelector('.ingredient-choice').value);
    const quantity = Number(row.querySelector('.ingredient-qty').value);
    const unit = row.querySelector('.ingredient-unit').value.trim();
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Hãy chọn nguyên liệu cho tất cả các dòng.');
    if (used.has(id)) throw new Error('Một nguyên liệu không được nhập hai lần.');
    if (!Number.isFinite(quantity) || quantity <= 0) throw new Error('Số lượng nguyên liệu phải lớn hơn 0.');
    if (!unit) throw new Error('Đơn vị nguyên liệu không được để trống.');
    used.add(id);
    result.push({ ingredientId: id, quantity, unit });
  }
  if (!result.length) throw new Error('Phải có ít nhất một nguyên liệu.');
  return result;
}

function fillRecipeForm(recipe) {
  const form = $('recipeForm');
  for (const name of ['name', 'categoryId', 'cookingTime', 'servings', 'description', 'instructions', 'imageUrl']) {
    form.elements[name].value = recipe[name] ?? '';
  }
  $('ingredientRows').replaceChildren();
  (recipe.ingredients || []).forEach(ingredientRow);
  if (!recipe.ingredients?.length) ingredientRow();
}

async function createCatalogItem(form, endpoint, name) {
  const submit = form.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    const data = Object.fromEntries(new FormData(form));
    await api(endpoint, { method: 'POST', body: JSON.stringify(data) });
    form.reset();
    await loadCatalog();
    const selectedCategory = $('recipeCategory').value;
    setOptions($('recipeCategory'), categories, '-- Chọn danh mục --', selectedCategory);
    for (const select of document.querySelectorAll('.ingredient-choice')) {
      setOptions(select, ingredients, '-- Chọn nguyên liệu --', select.value);
    }
    toast(`Đã tạo ${name} thành công.`);
  } catch (error) { toast(error.message, true); }
  finally { submit.disabled = false; }
}

async function editor() {
  if (!session.token) { needLogin(); return; }
  await loadCatalog();
  setOptions($('recipeCategory'), categories, '-- Chọn danh mục --');
  const idValue = new URLSearchParams(location.search).get('id');
  if (idValue !== null) {
    const id = Number(idValue);
    if (!Number.isSafeInteger(id) || id < 1) throw new Error('ID công thức không hợp lệ');
    currentRecipe = await api(`/api/recipes/${id}`, { auth: false });
    if (!ownRecipe(currentRecipe)) {
      $('recipeForm').hidden = true;
      $('editorHeading').textContent = 'Bạn không có quyền sửa công thức này';
      toast('HTTP 403 — Chỉ tác giả được sửa công thức của mình.', true);
      return;
    }
    $('editorHeading').textContent = `Sửa công thức: ${currentRecipe.name}`;
    fillRecipeForm(currentRecipe);
  } else ingredientRow();
  $('addIngredientRow').addEventListener('click', () => ingredientRow());
  for (const [formId, path, label] of [
    ['categoryForm', '/api/categories', 'danh mục'], ['ingredientForm', '/api/ingredients', 'nguyên liệu']
  ]) {
    $(formId).addEventListener('submit', async (event) => {
      event.preventDefault();
      await createCatalogItem(event.currentTarget, path, label);
    });
  }
  $('recipeForm').addEventListener('submit', async (event) => {
    event.preventDefault(); clearFormError();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    let items;
    try { items = readIngredients(); }
    catch (error) {
      $('ingredientError').textContent = error.message;
      $('ingredientError').hidden = false;
      return;
    }
    $('ingredientError').hidden = true;
    const fields = Object.fromEntries(new FormData(form));
    const body = {
      ...fields,
      name: fields.name.trim(), instructions: fields.instructions.trim(),
      categoryId: Number(fields.categoryId), cookingTime: Number(fields.cookingTime),
      servings: Number(fields.servings), ingredients: items
    };
    if (body.name.length < 2 || body.instructions.length < 5) {
      formError('Tên món ăn phải có ít nhất 2 ký tự và hướng dẫn ít nhất 5 ký tự.');
      return;
    }
    const submit = $('saveRecipe'); submit.disabled = true;
    try {
      const recipe = await api(currentRecipe ? `/api/recipes/${currentRecipe.id}` : '/api/recipes', {
        method: currentRecipe ? 'PUT' : 'POST', body: JSON.stringify(body)
      });
      location.href = `/recipe.html?id=${recipe.id}`;
    } catch (error) { formError(error.message); submit.disabled = false; }
  });
}



let loadedComments = [];
async function initComments(recipeId) {
  const form = $('commentForm');
  if (!form) return;
  await refreshComments(recipeId);
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const input = $('commentText');
    const content = input.value.trim();
    if (!content) { toast('Nội dung bình luận không được để trống.', true); return; }
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
      await api(`/api/recipes/${recipeId}/comments`, {
        method: 'POST', body: JSON.stringify({ content })
      });
      input.value = '';
      await refreshComments(recipeId);
      toast('Đã đăng bình luận.');
    } catch (error) { toast(error.message, true); }
    finally { button.disabled = false; }
  });
  $('commentsList').addEventListener('click', async (event) => {
    const btn = event.target.closest('button[data-comment-action]');
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const comment = loadedComments.find((item) => item.id === id);
    if (!comment || !session.token) return;
    const action = btn.dataset.commentAction;
    if (action === 'delete') {
      if (!confirm('Bạn chắc chắn muốn xóa bình luận này?')) return;
      btn.disabled = true;
      try {
        await api(`/api/recipes/${recipeId}/comments/${id}`, { method: 'DELETE' });
        await refreshComments(recipeId);
        toast('Đã xóa bình luận.');
      } catch (error) { toast(error.message, true); btn.disabled = false; }
      return;
    }
    if (action === 'cancel') { await refreshComments(recipeId); return; }
    if (action === 'edit') {
      const holder = $(`commentEdit${id}`);
      const textarea = document.createElement('textarea');
      textarea.maxLength = 1000;
      textarea.rows = 3;
      textarea.value = comment.content;
      const save = document.createElement('button');
      save.type = 'button';
      save.className = 'button';
      save.dataset.commentAction = 'save';
      save.dataset.id = String(id);
      save.textContent = 'Lưu bình luận';
      const cancel = document.createElement('button');
      cancel.type = 'button';
      cancel.className = 'button outline';
      cancel.dataset.commentAction = 'cancel';
      cancel.dataset.id = String(id);
      cancel.textContent = 'Hủy';
      holder.replaceChildren(textarea, save, cancel);
      textarea.focus();
      return;
    }
    if (action === 'save') {
      const content = $(`commentEdit${id}`).querySelector('textarea')?.value.trim();
      if (!content) { toast('Bình luận không được để trống.', true); return; }
      btn.disabled = true;
      try {
        await api(`/api/recipes/${recipeId}/comments/${id}`, {
          method: 'PUT', body: JSON.stringify({ content })
        });
        await refreshComments(recipeId);
        toast('Đã cập nhật bình luận.');
      } catch (error) { toast(error.message, true); btn.disabled = false; }
    }
  });
}
async function refreshComments(recipeId) {
  loadedComments = await api(`/api/recipes/${recipeId}/comments`, { auth: false });
  $('commentsCount').textContent = `${loadedComments.length} bình luận`;
  $('commentsList').innerHTML = loadedComments.length ? loadedComments.map((item) => {
    const canEdit = session.token && item.username === session.username;
    const canDelete = canEdit || session.role === 'ADMIN';
    return `<article class="comment-item"><div class="comment-head"><strong>${escapeHtml(item.fullName || item.username)}</strong>
      <span>${escapeHtml(String(item.createdAt || '').replace('T', ' ').slice(0, 16))}</span></div>
      <p class="preline">${escapeHtml(item.content)}</p><div class="comment-actions">
      ${canEdit ? `<button class="text-button" data-comment-action="edit" data-id="${item.id}">Sửa</button>` : ''}
      ${canDelete ? `<button class="text-button" data-comment-action="delete" data-id="${item.id}">Xóa</button>` : ''}
      </div><div class="comment-edit" id="commentEdit${item.id}"></div></article>`;
  }).join('') : '<p class="muted">Chưa có bình luận. Hãy là người đầu tiên chia sẻ cảm nhận!</p>';
  $('commentForm').hidden = !session.token;
  $('commentGuest').hidden = Boolean(session.token);
}

function renderAdminUsers(users) {
  $('adminUsers').innerHTML = users.map((user) => {
    const reason = user.lockReason ? ` — ${user.lockReason}` : '';
    const status = user.enabled ? 'Hoạt động' : 'Đã khóa';
    const control = user.role === 'ADMIN' ? '—' :
      `<button class="text-button" data-admin-toggle="${user.enabled ? 'lock' : 'unlock'}"
       data-id="${user.id}" data-username="${escapeHtml(user.username)}">
       ${user.enabled ? 'Khóa' : 'Mở khóa'}</button>`;
    return `<tr><td>${escapeHtml(user.username)}</td>
      <td>${escapeHtml(user.fullName || '')}</td><td>${escapeHtml(user.email)}</td>
      <td>${escapeHtml(user.role)}</td><td title="${escapeHtml(reason)}">${status}${escapeHtml(reason)}</td>
      <td>${control}</td></tr>`;
  }).join('');
}

async function adminDashboard() {
  if (!session.token) { needLogin(); return; }
  if (session.role !== 'ADMIN') { location.href = '/'; return; }
  const [summary, users, comments, recipes] = await Promise.all([
    api('/api/admin/summary'), api('/api/admin/users'),
    api('/api/admin/comments'), api('/api/recipes', { auth: false })
  ]);
  const figures = [
    ['Người dùng', summary.totalUsers], ['Công thức', summary.totalRecipes],
    ['Danh mục', summary.totalCategories], ['Nguyên liệu', summary.totalIngredients],
    ['Bình luận', summary.totalComments]
  ];
  $('adminMetrics').innerHTML = figures.map(([name, value]) =>
    `<div class="metric"><span>${escapeHtml(name)}</span><strong>${Number(value)}</strong></div>`).join('');
  renderAdminUsers(users);
  $('adminRecipes').innerHTML = recipes.map((recipe) => `<tr><td><a href="/recipe.html?id=${recipe.id}">${escapeHtml(recipe.name)}</a></td>
    <td>${escapeHtml(recipe.username)}</td><td>${escapeHtml(recipe.categoryName)}</td>
    <td><button class="text-button" data-admin-delete="recipe" data-id="${recipe.id}">Xóa</button></td></tr>`).join('');
  $('adminComments').innerHTML = comments.map((item) => `<tr><td>${escapeHtml(item.username)}</td>
    <td class="truncate-cell">${escapeHtml(item.content)}</td>
    <td><a href="/recipe.html?id=${item.recipeId}">Xem bài</a></td>
    <td><button class="text-button" data-admin-delete="comment" data-id="${item.id}">Xóa</button></td></tr>`).join('');
  $('adminContent').addEventListener('click', async (event) => {
    const toggle = event.target.closest('button[data-admin-toggle]');
    if (toggle) {
      const isLock = toggle.dataset.adminToggle === 'lock';
      const username = toggle.dataset.username;
      const reason = isLock ? prompt(`Lý do khóa tài khoản ${username}:`) : null;
      if (isLock && (reason === null || !reason.trim())) {
        if (reason !== null) toast('Bạn phải nhập lý do khóa.', true);
        return;
      }
      if (!isLock && !confirm(`Mở khóa tài khoản ${username}?`)) return;
      toggle.disabled = true;
      try {
        const url = `/api/admin/users/${toggle.dataset.id}/${isLock ? 'lock' : 'unlock'}`;
        await api(url, { method: 'PATCH', ...(isLock ? { body: JSON.stringify({ reason: reason.trim() }) } : {}) });
        toast(isLock ? 'Đã khóa tài khoản. Các JWT cũ đã bị thu hồi.' : 'Đã mở khóa. Người dùng cần đăng nhập lại.');
        await adminDashboardRefresh();
      } catch (error) { toast(error.message, true); toggle.disabled = false; }
      return;
    }
    const button = event.target.closest('button[data-admin-delete]');
    if (!button) return;
    const type = button.dataset.adminDelete;
    if (!confirm(`Bạn muốn xóa ${type === 'recipe' ? 'công thức' : 'bình luận'} này?`)) return;
    button.disabled = true;
    try {
      const path = type === 'recipe' ? `/api/admin/recipes/${button.dataset.id}` :
        `/api/admin/comments/${button.dataset.id}`;
      await api(path, { method: 'DELETE' });
      toast('Đã xóa thành công.');
      await adminDashboardRefresh();
    } catch (error) { toast(error.message, true); button.disabled = false; }
  });
}
async function adminDashboardRefresh() {
  const [summary, users, comments, recipes] = await Promise.all([
    api('/api/admin/summary'), api('/api/admin/users'),
    api('/api/admin/comments'), api('/api/recipes', { auth: false })
  ]);
  renderAdminUsers(users);
  const numbers = $('adminMetrics').querySelectorAll('.metric strong');
  [summary.totalUsers, summary.totalRecipes, summary.totalCategories,
    summary.totalIngredients, summary.totalComments].forEach((value, i) => { numbers[i].textContent = value; });
  $('adminRecipes').innerHTML = recipes.map((recipe) => `<tr><td><a href="/recipe.html?id=${recipe.id}">${escapeHtml(recipe.name)}</a></td>
    <td>${escapeHtml(recipe.username)}</td><td>${escapeHtml(recipe.categoryName)}</td>
    <td><button class="text-button" data-admin-delete="recipe" data-id="${recipe.id}">Xóa</button></td></tr>`).join('');
  $('adminComments').innerHTML = comments.map((item) => `<tr><td>${escapeHtml(item.username)}</td>
    <td class="truncate-cell">${escapeHtml(item.content)}</td>
    <td><a href="/recipe.html?id=${item.recipeId}">Xem bài</a></td>
    <td><button class="text-button" data-admin-delete="comment" data-id="${item.id}">Xóa</button></td></tr>`).join('');
}

setSession(session.token, session.username);
if ($('logoutBtn')) $('logoutBtn').addEventListener('click', () => {
  setSession('', ''); location.href = '/';
});
const launch = { home, login: authentication, register: authentication, detail, editor, admin: adminDashboard };
async function launchPage() {
  if (session.token) {
    try {
      const profile = await api('/api/users/me');
      session.role = profile.role || 'USER';
      setSession(session.token, session.username);
    } catch (error) {
      if (error.status !== 401) toast(error.message, true);
    }
  }
  if (launch[page]) await launch[page]();
}
launchPage().catch((error) => {
  toast(error.message, true);
  if (page === 'detail') $('detailContent').innerHTML = `<div class="panel"><p class="field-error">${escapeHtml(error.message)}</p><a href="/#recipes">Quay lại danh sách</a></div>`;
});
