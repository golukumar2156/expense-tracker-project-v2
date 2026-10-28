'use strict';

const API_BASE = window.EXPENSE_API_BASE || 'http://localhost:8080/api';
const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const money = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2
}).format(Number(value || 0));
const today = new Date();
const toIsoDate = (date) => {
  const value = new Date(date);
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
};
const dateLabel = (value) => new Date(`${value}T12:00:00`).toLocaleDateString('en-IN', {
  day: '2-digit', month: 'short', year: 'numeric'
});
const categoryIcons = {
  'Food & dining': '☕', Transport: '↗', Shopping: '◈',
  'Bills & utilities': '▤', Entertainment: '♫', Health: '✚', Other: '✦'
};
const categoryClasses = {
  'Food & dining': 'food', Transport: 'transport', Shopping: 'shopping',
  'Bills & utilities': 'bills', Entertainment: 'entertainment', Health: 'health', Other: 'food'
};
const categoryPalette = {
  'Food & dining': '#8c65ed', Transport: '#60b7e8', Shopping: '#f5b15f',
  'Bills & utilities': '#f18c9a', Entertainment: '#6ac8a3', Health: '#9b91ed', Other: '#d6d3e7'
};

let token = localStorage.getItem('spendly_token') || '';
let currentUser = JSON.parse(localStorage.getItem('spendly_user') || 'null');
let categories = [];
let expenses = [];
let activeView = 'dashboard';
let showAll = false;
let editingId = null;
let viewingExpenseId = null;
let authMode = 'login';

async function api(path, options = {}) {
  const headers = { Accept: 'application/json', ...(options.headers || {}) };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && token && !path.startsWith('/auth/')) {
      signOut(false);
      throw new Error('Your session expired. Please sign in again.');
    }
    const fieldMessages = body.fields ? Object.values(body.fields).join(' ') : '';
    throw new Error(fieldMessages || body.message || 'Something went wrong. Please try again.');
  }
  return body;
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove('show'), 2800);
}

function setAuthMode(mode) {
  authMode = mode;
  const isSignup = mode === 'signup';
  $('#authTitle').textContent = isSignup ? 'Create your account' : 'Welcome back';
  $('#authDescription').textContent = isSignup
    ? 'A few details and you can start tracking your expenses.'
    : 'Sign in to see your personal spending dashboard.';
  $('#authNameWrap').hidden = !isSignup;
  $('#authName').required = isSignup;
  $('#authPassword').autocomplete = isSignup ? 'new-password' : 'current-password';
  $('#authSubmit').innerHTML = isSignup ? 'Create account <span>→</span>' : 'Sign in <span>→</span>';
  $('#authSwitchText').textContent = isSignup ? 'Already have an account?' : 'New to Spendly?';
  $('#authSwitch').textContent = isSignup ? 'Sign in' : 'Create an account';
  $('#authError').textContent = '';
}

function saveSession(session) {
  token = session.token;
  currentUser = session.user;
  localStorage.setItem('spendly_token', token);
  localStorage.setItem('spendly_user', JSON.stringify(currentUser));
}

function signOut(showMessage = true) {
  token = '';
  currentUser = null;
  categories = [];
  expenses = [];
  localStorage.removeItem('spendly_token');
  localStorage.removeItem('spendly_user');
  $('.app-shell').classList.add('hidden');
  $('#authScreen').classList.remove('hidden');
  $('#authForm').reset();
  setAuthMode('login');
  if (showMessage) $('#authError').textContent = 'You have signed out.';
}

$('#authSwitch').addEventListener('click', () => setAuthMode(authMode === 'login' ? 'signup' : 'login'));
$('#authForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = $('#authSubmit');
  button.disabled = true;
  $('#authError').textContent = '';
  try {
    const payload = {
      email: $('#authEmail').value.trim(),
      password: $('#authPassword').value
    };
    if (authMode === 'signup') payload.name = $('#authName').value.trim();
    const session = await api(`/auth/${authMode === 'signup' ? 'signup' : 'login'}`, {
      method: 'POST', body: JSON.stringify(payload)
    });
    saveSession(session);
    await startApp();
  } catch (error) {
    $('#authError').textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

async function startApp() {
  if (!token) {
    $('#authScreen').classList.remove('hidden');
    $('.app-shell').classList.add('hidden');
    return;
  }
  try {
    const [loadedCategories, loadedExpenses] = await Promise.all([
      api('/categories'), api('/expenses')
    ]);
    categories = loadedCategories;
    expenses = loadedExpenses;
    $('#authScreen').classList.add('hidden');
    $('.app-shell').classList.remove('hidden');
    updateProfile();
    populateCategorySelects();
    render();
  } catch (error) {
    if (token) {
      $('#authScreen').classList.remove('hidden');
      $('.app-shell').classList.add('hidden');
      $('#authError').textContent = error.message;
    }
  }
}

function updateProfile() {
  const name = currentUser?.name || 'Your account';
  const email = currentUser?.email || '';
  const initials = name.split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase() || 'U';
  $('#profileName').textContent = name;
  $('#profileEmail').textContent = email;
  $('#profileAvatar').textContent = initials;
  $('#topAvatar').textContent = initials;
  const firstName = name.split(/\s+/)[0];
  $('#pageTitle').innerHTML = `Welcome, ${escapeHtml(firstName)} <span class="wave">✳</span>`;
}

function populateCategorySelects() {
  const expenseSelect = $('#expenseCategory');
  const filterSelect = $('#categoryFilter');
  const selectedExpense = expenseSelect.value;
  const selectedFilter = filterSelect.value;
  expenseSelect.innerHTML = '<option value="">Select category</option>' + categories.map(category =>
    `<option value="${category.id}">${escapeHtml(category.name)}</option>`).join('');
  filterSelect.innerHTML = '<option value="all">All categories</option>' + categories.map(category =>
    `<option value="${escapeHtml(category.name)}">${escapeHtml(category.name)}</option>`).join('');
  if (categories.some(category => String(category.id) === selectedExpense)) expenseSelect.value = selectedExpense;
  if (categories.some(category => category.name === selectedFilter)) filterSelect.value = selectedFilter;
}

function getPeriodRange() {
  const period = $('#periodSelect').value;
  const now = new Date();
  let from;
  let to;

  if (period === 'year') {
    from = new Date(now.getFullYear(), 0, 1);
    to = new Date(now.getFullYear(), 11, 31);
  } else if (period === 'week') {
    from = new Date(now);
    from.setHours(0, 0, 0, 0);
    from.setDate(from.getDate() - ((from.getDay() + 6) % 7));
    to = new Date(from);
    to.setDate(to.getDate() + 6);
  } else {
    from = new Date(now.getFullYear(), now.getMonth(), 1);
    to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  }

  return { from: toIsoDate(from), to: toIsoDate(to) };
}

/* Dashboard figures come from Spring Boot's GET /api/summary.
   Frontend only formats and displays the returned values. */
async function renderStats() {
  try {
    const { from, to } = getPeriodRange();
    const summary = await api(`/summary?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
    const sum = Number(summary.total || 0);
    const categoryRows = (summary.categories || []).map(item => ({
      name: item.category,
      amount: Number(item.total || 0),
      transactionCount: Number(item.transactionCount || 0)
    }));

    $('#totalSpent').textContent = money(sum);
    $('#transactionCount').textContent = Number(summary.transactionCount || 0);
    $('#chartTotal').textContent = money(sum);
    $('#donutTotal').textContent = sum >= 1000 ? `₹${(sum / 1000).toFixed(1)}k` : money(sum);

    const topCategory = [...categoryRows].sort((a, b) => b.amount - a.amount)[0];
    $('#topCategory').textContent = topCategory?.name || 'No expenses';
    $('#topCategoryAmount').textContent = `${money(topCategory?.amount || 0)} in selected period`;

    // Chart uses the category totals already calculated by the backend.
    const chartValues = categoryRows.map(row => row.amount);
    const maxValue = Math.max(1, ...chartValues);
    const points = categoryRows.map((row, index) => ({
      x: index * (620 / Math.max(1, categoryRows.length - 1)),
      y: 188 - (row.amount / maxValue) * 164
    }));

    if (points.length) {
      const line = points.map((point, index) =>
        `${index ? 'L' : 'M'}${point.x.toFixed(1)},${point.y.toFixed(1)}`
      ).join(' ');
      $('#chartLine').setAttribute('d', line);
      $('#chartArea').setAttribute('d', `${line} L620,210 L0,210 Z`);
      const last = points[points.length - 1];
      $('#chartPoint').setAttribute('cx', last.x);
      $('#chartPoint').setAttribute('cy', last.y);
    } else {
      $('#chartLine').setAttribute('d', 'M0,188 L620,188');
      $('#chartArea').setAttribute('d', 'M0,188 L620,188 L620,210 L0,210 Z');
      $('#chartPoint').setAttribute('cx', 620);
      $('#chartPoint').setAttribute('cy', 188);
    }

    $('.x-labels').innerHTML = categoryRows
      .slice(0, 7)
      .map(row => `<span>${escapeHtml(row.name)}</span>`)
      .join('');

    let cursor = 0;
    const stops = categoryRows.map(row => {
      const start = cursor;
      cursor += sum ? row.amount / sum * 100 : 0;
      return `${categoryPalette[row.name] || '#d6d3e7'} ${start}% ${cursor}%`;
    });
    $('#donutChart').style.background = stops.length
      ? `conic-gradient(${stops.join(',')})`
      : '#efedf5';

    $('#categoryList').innerHTML = categoryRows.length
      ? [...categoryRows].sort((a, b) => b.amount - a.amount).slice(0, 7).map(row => `
          <div class="category-row">
            <span class="cat-name">
              <i class="cat-color ${categoryClasses[row.name] || 'food'}" style="background:${categoryPalette[row.name] || '#d6d3e7'}"></i>
              ${escapeHtml(row.name)}
            </span>
            <strong>${money(row.amount)} <small>${sum ? Math.round(row.amount / sum * 100) : 0}%</small></strong>
          </div>`).join('')
      : '<div class="empty-state">No expenses in this period yet.</div>';
  } catch (error) {
    showToast(`Could not load dashboard summary: ${error.message}`);
  }
}

function filteredExpenses() {
  let list = [...expenses].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
  const query = $('#searchInput').value.trim().toLowerCase();
  const category = $('#categoryFilter').value;
  const date = $('#dateFilter').value;
  if (query) list = list.filter(expense => expense.title.toLowerCase().includes(query) || expense.category.toLowerCase().includes(query));
  if (category !== 'all') list = list.filter(expense => expense.category === category);
  if (date) list = list.filter(expense => expense.date === date);
  return list;
}

function renderTransactions() {
  const list = filteredExpenses();
  const visible = showAll || activeView === 'transactions' ? list : list.slice(0, 5);
  $('#transactionsBody').innerHTML = visible.length ? visible.map(expense => `
    <tr>
      <td><div class="transaction-name"><span class="transaction-icon ${categoryClasses[expense.category] || 'food'}">${categoryIcons[expense.category] || '✦'}</span><span>${escapeHtml(expense.title)}<small>Personal expense</small></span></div></td>
      <td><span class="category-tag">${escapeHtml(expense.category)}</span></td>
      <td>${dateLabel(expense.date)}</td><td class="amount">${money(expense.amount)}</td>
      <td><div class="row-actions"><button class="row-action view-expense" data-id="${expense.id}" title="View details" aria-label="View expense details">◉</button><button class="row-action edit-expense" data-id="${expense.id}" title="Edit expense" aria-label="Edit expense">✎</button><button class="row-action delete-expense" data-id="${expense.id}" title="Delete expense" aria-label="Delete expense">×</button></div></td>
    </tr>`).join('') : '<tr><td colspan="5" class="empty-state">No expenses found. Try changing your filters or add your first expense.</td></tr>';
  $('#tableCount').textContent = `Showing ${visible.length} of ${list.length} transaction${list.length === 1 ? '' : 's'}`;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[character]);
}

function renderCategoryManagement() {
  const container = $('#categoryManagementList');
  if (!container) return;
  container.innerHTML = categories.map(category => `
    <div class="manage-category-row">
      <span class="transaction-icon ${categoryClasses[category.name] || 'food'}">${categoryIcons[category.name] || '✦'}</span>
      <div class="manage-category-copy"><strong>${escapeHtml(category.name)}</strong><small>${escapeHtml(category.description || 'Personal category')}</small></div>
      <div class="manage-category-actions"><button type="button" data-action="edit" data-id="${category.id}">Edit</button><button type="button" data-action="delete" data-id="${category.id}">Delete</button></div>
    </div>`).join('') || '<p class="management-note">No categories yet.</p>';
}

function render() { renderStats(); renderTransactions(); renderCategoryManagement(); }

async function reloadExpensesFromBackend() {
  expenses = await api('/expenses');
  renderTransactions();
  await renderStats();
}

function openModal(expense = null) {
  editingId = expense?.id ?? null;
  $('#modalTitle').textContent = expense ? 'Edit expense' : 'Add an expense';
  $('#expenseTitle').value = expense?.title || '';
  $('#expenseAmount').value = expense?.amount ?? '';
  $('#expenseCategory').value = expense ? String(expense.categoryId) : '';
  $('#expenseDate').value = expense?.date || toIsoDate(today);
  $('#expenseModal').classList.add('open');
  $('#expenseModal').setAttribute('aria-hidden', 'false');
  setTimeout(() => $('#expenseTitle').focus(), 50);
}

function closeModal() {
  $('#expenseModal').classList.remove('open');
  $('#expenseModal').setAttribute('aria-hidden', 'true');
  $('#expenseForm').reset();
  editingId = null;
}

function openDetails(expense) {
  if (!expense) return;
  viewingExpenseId = expense.id;
  $('#detailsTitle').textContent = expense.title;
  const category = categories.find(item => item.id === expense.categoryId)?.name || expense.category || 'Other';
  $('#expenseDetailsContent').innerHTML = `
    <div class="detail-amount">${money(expense.amount)}</div>
    <div class="detail-row"><span>Expense title</span><strong>${escapeHtml(expense.title)}</strong></div>
    <div class="detail-row"><span>Category</span><strong>${escapeHtml(category)}</strong></div>
    <div class="detail-row"><span>Date</span><strong>${dateLabel(expense.date)}</strong></div>
    <div class="detail-row"><span>Transaction ID</span><strong>#${expense.id}</strong></div>`;
  $('#expenseDetailsModal').classList.add('open');
  $('#expenseDetailsModal').setAttribute('aria-hidden', 'false');
}

function closeDetails() {
  $('#expenseDetailsModal').classList.remove('open');
  $('#expenseDetailsModal').setAttribute('aria-hidden', 'true');
  viewingExpenseId = null;
}

function setView(view) {
  activeView = view;
  $$('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.view === view));
  const titles = { dashboard: 'Overview', transactions: 'Transactions', categories: 'Categories' };
  $('#crumbTitle').textContent = titles[view];
  if (view !== 'dashboard') $('#pageTitle').textContent = titles[view]; else updateProfile();
  $('#todayLabel').textContent = view === 'dashboard' ? 'YOUR FINANCES AT A GLANCE' : view === 'transactions' ? 'YOUR SPENDING HISTORY' : 'EXPLORE YOUR SPENDING';
  showAll = view !== 'dashboard';
  $('#categoryManagement').classList.toggle('hidden', view !== 'categories');
  renderTransactions(); renderCategoryManagement();
  if (view === 'categories') $('.category-panel').scrollIntoView({ behavior: 'smooth', block: 'center' });
  else if (view === 'transactions') $('#transactionsSection').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

$('#addExpenseBtn').addEventListener('click', () => openModal());
$('#closeModal').addEventListener('click', closeModal);
$('#cancelModal').addEventListener('click', closeModal);
$('#expenseModal').addEventListener('click', event => { if (event.target === $('#expenseModal')) closeModal(); });
document.addEventListener('keydown', event => { if (event.key === 'Escape') { closeModal(); closeDetails(); } });
$('#closeDetailsModal').addEventListener('click', closeDetails);
$('#closeDetailsFooter').addEventListener('click', closeDetails);
$('#expenseDetailsModal').addEventListener('click', event => { if (event.target === $('#expenseDetailsModal')) closeDetails(); });
$('#editFromDetails').addEventListener('click', () => { const expense = expenses.find(item => item.id === viewingExpenseId); closeDetails(); if (expense) openModal(expense); });

$('#expenseForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = {
    title: $('#expenseTitle').value.trim(),
    amount: Number($('#expenseAmount').value),
    categoryId: Number($('#expenseCategory').value),
    date: $('#expenseDate').value
  };
  if (!payload.title || !payload.categoryId || !payload.date || !Number.isFinite(payload.amount) || payload.amount <= 0) {
    showToast('Please enter valid expense details.'); return;
  }
  const wasEditing = editingId !== null;
  const saveButton = $('#expenseForm button[type="submit"]');
  saveButton.disabled = true;
  try {
    const saved = await api(wasEditing ? `/expenses/${editingId}` : '/expenses', {
      method: wasEditing ? 'PUT' : 'POST', body: JSON.stringify(payload)
    });
    closeModal();
    await reloadExpensesFromBackend();
    showToast(wasEditing ? 'Expense updated successfully.' : 'Expense added successfully.');
  } catch (error) { showToast(error.message); }
  finally { saveButton.disabled = false; }
});

$('#transactionsBody').addEventListener('click', async (event) => {
  const viewButton = event.target.closest('.view-expense');
  const editButton = event.target.closest('.edit-expense');
  const deleteButton = event.target.closest('.delete-expense');
  if (viewButton) {
    const expense = expenses.find(item => item.id === Number(viewButton.dataset.id));
    if (expense) openDetails(expense);
  }
  if (editButton) {
    const expense = expenses.find(item => item.id === Number(editButton.dataset.id));
    if (expense) openModal(expense);
  }
  if (deleteButton) {
    const expense = expenses.find(item => item.id === Number(deleteButton.dataset.id));
    if (!expense || !confirm(`Delete "${expense.title}"? This cannot be undone.`)) return;
    try {
      await api(`/expenses/${expense.id}`, { method: 'DELETE' });
      await reloadExpensesFromBackend();
      showToast('Expense deleted.');
    } catch (error) { showToast(error.message); }
  }
});

['searchInput', 'categoryFilter', 'dateFilter'].forEach(id => {
  $('#'+id).addEventListener(id === 'searchInput' ? 'input' : 'change', () => { showAll = true; renderTransactions(); });
});
$('#clearFilters').addEventListener('click', () => {
  $('#searchInput').value = ''; $('#categoryFilter').value = 'all'; $('#dateFilter').value = '';
  showAll = activeView !== 'dashboard'; renderTransactions();
});
$('#viewAllBtn').addEventListener('click', () => setView('transactions'));
$$('.nav-item').forEach(button => button.addEventListener('click', () => setView(button.dataset.view)));
$('#periodSelect').addEventListener('change', renderStats);
$('#insightBtn').addEventListener('click', () => $('.category-panel').scrollIntoView({ behavior: 'smooth', block: 'center' }));
$('#categoryMore').addEventListener('click', async () => {
  const name = prompt('New category name (2–60 characters):');
  if (name === null) return;
  const trimmed = name.trim();
  if (trimmed.length < 2 || trimmed.length > 60) { showToast('Category name must be 2–60 characters.'); return; }
  try {
    const created = await api('/categories', { method: 'POST', body: JSON.stringify({ name: trimmed, description: '' }) });
    categories.push(created); categories.sort((a, b) => a.name.localeCompare(b.name));
    populateCategorySelects(); showToast('Category created successfully.');
  } catch (error) { showToast(error.message); }
});
$('#categoryManagementList').addEventListener('click', async (event) => {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const category = categories.find(item => item.id === Number(button.dataset.id));
  if (!category) return;
  if (button.dataset.action === 'edit') {
    const name = prompt('Category name:', category.name);
    if (name === null) return;
    const trimmed = name.trim();
    if (trimmed.length < 2 || trimmed.length > 60) { showToast('Category name must be 2–60 characters.'); return; }
    const description = prompt('Description (optional):', category.description || '');
    if (description === null) return;
    try {
      const updated = await api(`/categories/${category.id}`, { method: 'PUT', body: JSON.stringify({ name: trimmed, description: description.trim() }) });
      categories = categories.map(item => item.id === updated.id ? updated : item);
      expenses = expenses.map(item => item.categoryId === updated.id ? { ...item, category: updated.name } : item);
      populateCategorySelects(); render(); showToast('Category updated.');
    } catch (error) { showToast(error.message); }
  }
  if (button.dataset.action === 'delete') {
    if (!confirm(`Delete category "${category.name}"?`)) return;
    try {
      await api(`/categories/${category.id}`, { method: 'DELETE' });
      categories = categories.filter(item => item.id !== category.id);
      populateCategorySelects(); render(); showToast('Category deleted.');
    } catch (error) { showToast(error.message); }
  }
});

$('#logoutBtn').addEventListener('click', () => signOut(true));
$('#topAvatar').addEventListener('click', () => signOut(true));
$('#todayLabel').textContent = `${today.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' }).toUpperCase()} · YOUR FINANCES AT A GLANCE`;

setAuthMode('login');
startApp();
