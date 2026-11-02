/**
 * EXPENSE SPLITTER | Complete Interactive Financial State Machine
 */

const STORAGE_KEY = 'EXPENSE_SPLITTER_DATA_V1';

// Initial Seed Data matching the SQLite database
const INITIAL_DATA = {
  currency: '₹',
  theme: 'dark',
  currentUserId: 1, // Rahul Sharma
  users: [
    { id: 1, name: 'Rahul Sharma', username: 'rahul', email: 'rahul@example.com' },
    { id: 2, name: 'Aman Verma', username: 'aman', email: 'aman@example.com' },
    { id: 3, name: 'Rohit Kumar', username: 'rohit', email: 'rohit@example.com' },
    { id: 4, name: 'Priya Patel', username: 'priya', email: 'priya@example.com' }
  ],
  groups: [
    { id: 1, name: 'Goa Trip 2026', desc: 'Summer vacation beach villa & dining', memberIds: [1, 2, 3] },
    { id: 2, name: 'Flat 402 Roommates', desc: 'Apartment rent, groceries & broadband', memberIds: [1, 2, 4] },
    { id: 3, name: 'Office Tech Team', desc: 'Team lunches and hackathon celebrations', memberIds: [1, 3, 4] }
  ],
  expenses: [
    {
      id: 1,
      groupId: 1,
      title: 'Beachside Dinner',
      category: 'Food',
      amountPaise: 90000, // ₹900.00
      paidByUserId: 1,
      date: '2026-10-25',
      splitMethod: 'EQUAL',
      shares: { 1: 30000, 2: 30000, 3: 30000 },
      description: 'Seafood and drinks at Baga'
    },
    {
      id: 2,
      groupId: 1,
      title: 'Scuba Diving & Boat Rental',
      category: 'Entertainment',
      amountPaise: 450000, // ₹4,500.00
      paidByUserId: 1,
      date: '2026-10-26',
      splitMethod: 'EQUAL',
      shares: { 1: 150000, 2: 150000, 3: 150000 },
      description: 'Grand Island scuba package'
    },
    {
      id: 3,
      groupId: 2,
      title: 'Apartment Wi-Fi & Electricity',
      category: 'Electricity',
      amountPaise: 240000, // ₹2,400.00
      paidByUserId: 2,
      date: '2026-10-27',
      splitMethod: 'EQUAL',
      shares: { 1: 80000, 2: 80000, 4: 80000 },
      description: 'Fiber broadband monthly recharge'
    },
    {
      id: 4,
      groupId: 2,
      title: 'Weekly Organic Groceries',
      category: 'Groceries',
      amountPaise: 180000, // ₹1,800.00
      paidByUserId: 1,
      date: '2026-10-28',
      splitMethod: 'EQUAL',
      shares: { 1: 60000, 2: 60000, 4: 60000 },
      description: 'Supermarket supplies'
    },
    {
      id: 5,
      groupId: 3,
      title: 'Client Demo Celebration Buffet',
      category: 'Food',
      amountPaise: 360000, // ₹3,600.00
      paidByUserId: 4,
      date: '2026-10-29',
      splitMethod: 'EQUAL',
      shares: { 1: 120000, 3: 120000, 4: 120000 },
      description: 'Team outing barbecue'
    }
  ],
  settlements: [
    {
      id: 1,
      groupId: 1,
      payerUserId: 2,
      receiverUserId: 1,
      amountPaise: 50000, // ₹500.00
      date: '2026-10-27',
      notes: 'UPI Transaction ref 8392109'
    }
  ],
  notifications: [
    { id: 1, userId: 2, title: 'New Expense in Goa Trip 2026', msg: 'Rahul added Beachside Dinner (₹900.00). Your share: ₹300.00', date: '2026-10-25', read: false },
    { id: 2, userId: 1, title: 'Settlement Received', msg: 'Aman Verma sent ₹500.00 via UPI.', date: '2026-10-27', read: true },
    { id: 3, userId: 3, title: 'Group Invite', msg: 'You have been added to Office Tech Team.', date: '2026-10-24', read: false }
  ]
};

// Global App State
let appData = loadState();

function loadState() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved) {
    try { return JSON.parse(saved); } catch (e) { console.error(e); }
  }
  return JSON.parse(JSON.stringify(INITIAL_DATA));
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(appData));
}

// Helpers
function formatCurrency(paise) {
  const val = (paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return `${appData.currency} ${val}`;
}

function getUser(id) {
  return appData.users.find(u => u.id === parseInt(id)) || { name: 'Unknown', username: 'user' };
}

function getGroup(id) {
  return appData.groups.find(g => g.id === parseInt(id)) || { name: 'Group' };
}

function getCurrentUser() {
  return getUser(appData.currentUserId);
}

// Net Balance Calculator (Paise)
function calculateGroupBalances(groupId) {
  const g = getGroup(groupId);
  if (!g || !g.memberIds) return [];

  const balanceMap = {};
  g.memberIds.forEach(uid => {
    balanceMap[uid] = { userId: uid, paid: 0, share: 0, settledGiven: 0, settledReceived: 0 };
  });

  // Expenses in this group
  appData.expenses.filter(e => e.groupId === g.id).forEach(e => {
    if (balanceMap[e.paidByUserId]) {
      balanceMap[e.paidByUserId].paid += e.amountPaise;
    }
    if (e.shares) {
      Object.entries(e.shares).forEach(([uid, share]) => {
        const u = parseInt(uid);
        if (balanceMap[u]) balanceMap[u].share += share;
      });
    }
  });

  // Settlements in this group
  appData.settlements.filter(s => s.groupId === g.id).forEach(s => {
    if (balanceMap[s.payerUserId]) balanceMap[s.payerUserId].settledGiven += s.amountPaise;
    if (balanceMap[s.receiverUserId]) balanceMap[s.receiverUserId].settledReceived += s.amountPaise;
  });

  return Object.values(balanceMap).map(b => {
    const net = (b.paid - b.share) + (b.settledGiven - b.settledReceived);
    return {
      ...b,
      net,
      user: getUser(b.userId)
    };
  });
}

// Greedy Min-Cash-Flow Algorithm
function simplifyDebts(balances) {
  // balances: array of { userId, net, user }
  const debtors = []; // net < 0
  const creditors = []; // net > 0

  balances.forEach(b => {
    if (b.net < -0.01) debtors.push({ userId: b.userId, name: b.user.name, amount: -b.net });
    else if (b.net > 0.01) creditors.push({ userId: b.userId, name: b.user.name, amount: b.net });
  });

  // Sort descending by magnitude
  debtors.sort((a, b) => b.amount - a.amount);
  creditors.sort((a, b) => b.amount - a.amount);

  const transactions = [];
  let i = 0, j = 0;

  while (i < debtors.length && j < creditors.length) {
    const debtor = debtors[i];
    const creditor = creditors[j];
    const settleAmt = Math.min(debtor.amount, creditor.amount);

    if (settleAmt > 0) {
      transactions.push({
        fromUserId: debtor.userId,
        fromName: debtor.name,
        toUserId: creditor.userId,
        toName: creditor.name,
        amountPaise: Math.round(settleAmt)
      });
    }

    debtor.amount -= settleAmt;
    creditor.amount -= settleAmt;

    if (debtor.amount <= 0.01) i++;
    if (creditor.amount <= 0.01) j++;
  }

  return transactions;
}

// Overall User Financial Overview
function getUserOverview(userId) {
  let totalPaid = 0;
  let totalShare = 0;
  let youAreOwed = 0;
  let youOwe = 0;

  appData.groups.filter(g => g.memberIds.includes(userId)).forEach(g => {
    const bList = calculateGroupBalances(g.id);
    const myB = bList.find(b => b.userId === userId);
    if (myB) {
      totalPaid += myB.paid;
      totalShare += myB.share;
      if (myB.net > 0) youAreOwed += myB.net;
      else if (myB.net < 0) youOwe += (-myB.net);
    }
  });

  return { totalPaid, totalShare, youAreOwed, youOwe };
}

/* ==========================================================================
   UI NAVIGATION & VIEW CONTROLLER
   ========================================================================== */

let activeView = 'dashboard';
let monthlyChart = null;
let categoryChart = null;

function setView(viewId) {
  activeView = viewId;
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  const navBtn = document.getElementById(`nav-${viewId}`);
  if (navBtn) navBtn.classList.add('active');

  const contentArea = document.getElementById('view-container');
  if (viewId === 'dashboard') renderDashboard(contentArea);
  else if (viewId === 'groups') renderGroups(contentArea);
  else if (viewId === 'add-expense') renderAddExpense(contentArea);
  else if (viewId === 'expenses') renderExpenses(contentArea);
  else if (viewId === 'balances') renderBalances(contentArea);
  else if (viewId === 'settlements') renderSettlements(contentArea);
  else if (viewId === 'reports') renderReports(contentArea);
  else if (viewId === 'notifications') renderNotifications(contentArea);
  else if (viewId === 'settings') renderSettings(contentArea);

  updateNotificationBadge();
}

function updateNotificationBadge() {
  const unread = appData.notifications.filter(n => n.userId === appData.currentUserId && !n.read).length;
  const badge = document.getElementById('sidebar-notification-badge');
  if (badge) {
    badge.textContent = unread;
    badge.style.display = unread > 0 ? 'inline-block' : 'none';
  }
}

/* ==========================================================================
   VIEW 1: DASHBOARD
   ========================================================================== */
function renderDashboard(container) {
  const user = getCurrentUser();
  const overview = getUserOverview(user.id);
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  
  let totalGroupSpending = 0;
  userGroups.forEach(g => {
    appData.expenses.filter(e => e.groupId === g.id).forEach(e => totalGroupSpending += e.amountPaise);
  });

  // Recent 6 expenses
  const recent = [...appData.expenses]
    .filter(e => userGroups.some(g => g.id === e.groupId))
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 6);

  // Pending simplified debts for this user
  const myDebts = [];
  userGroups.forEach(g => {
    const debts = simplifyDebts(calculateGroupBalances(g.id));
    debts.forEach(d => {
      if (d.fromUserId === user.id || d.toUserId === user.id) {
        myDebts.push({ ...d, groupName: g.name });
      }
    });
  });

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Welcome back, ${user.name}!</h1>
        <p class="page-subtitle">Here is your financial overview across all active groups.</p>
      </div>
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-primary" onclick="setView('add-expense')">➕ Add Expense</button>
        <button class="btn btn-success" onclick="setView('settlements')">🤝 Settle Up</button>
      </div>
    </div>

    <!-- KPI Metric Cards -->
    <div class="grid-kpi">
      <div class="card kpi-card">
        <span class="kpi-title">Total Group Spending</span>
        <span class="kpi-value">${formatCurrency(totalGroupSpending)}</span>
        <span class="kpi-sub">${userGroups.length} active expense groups</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">Total Paid By You</span>
        <span class="kpi-value text-blue">${formatCurrency(overview.totalPaid)}</span>
        <span class="kpi-sub">Out-of-pocket spending</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">You Are Owed</span>
        <span class="kpi-value text-green">${formatCurrency(overview.youAreOwed)}</span>
        <span class="kpi-sub">Credits to receive from others</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">You Owe</span>
        <span class="kpi-value text-red">${formatCurrency(overview.youOwe)}</span>
        <span class="kpi-sub">Debts to settle up</span>
      </div>
    </div>

    <!-- Charts Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 18px; margin-bottom: 24px;">
      <div class="card">
        <h3 class="card-title">Monthly Spending Trend</h3>
        <div style="height: 240px; position: relative;">
          <canvas id="monthlyChartCanvas"></canvas>
        </div>
      </div>
      <div class="card">
        <h3 class="card-title">Category Distribution</h3>
        <div style="height: 240px; position: relative;">
          <canvas id="categoryChartCanvas"></canvas>
        </div>
      </div>
    </div>

    <!-- Tables Row -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap: 18px;">
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <h3 class="card-title" style="margin:0;">Recent Group Expenses</h3>
          <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 11px;" onclick="setView('expenses')">View All →</button>
        </div>
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Title</th>
                <th>Paid By</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              ${recent.length === 0 ? '<tr><td colspan="4" style="text-align:center; color:var(--text-muted);">No expenses logged yet.</td></tr>' : ''}
              ${recent.map(e => `
                <tr>
                  <td>${e.date}</td>
                  <td><strong>${escapeHtml(e.title)}</strong><br><small style="color:var(--text-muted);">${getGroup(e.groupId).name}</small></td>
                  <td>${getUser(e.paidByUserId).name}</td>
                  <td><strong>${formatCurrency(e.amountPaise)}</strong></td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <h3 class="card-title" style="margin:0;">Active Minimized Debts</h3>
          <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 11px;" onclick="setView('balances')">Inspect →</button>
        </div>
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Debtor (Owes)</th>
                <th>Creditor (Gets)</th>
                <th>Amount</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              ${myDebts.length === 0 ? '<tr><td colspan="4" style="text-align:center; color:var(--success);">🎉 All debts are settled!</td></tr>' : ''}
              ${myDebts.map(d => `
                <tr>
                  <td><span class="badge badge-red">${escapeHtml(d.fromName)}</span></td>
                  <td><span class="badge badge-green">${escapeHtml(d.toName)}</span></td>
                  <td><strong>${formatCurrency(d.amountPaise)}</strong></td>
                  <td>
                    <button class="btn btn-success" style="padding: 4px 10px; font-size: 11px;" onclick="openQuickSettle(${d.fromUserId}, ${d.toUserId}, ${d.amountPaise})">Settle</button>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  renderCharts();
}

function renderCharts() {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const expenses = appData.expenses.filter(e => userGroups.some(g => g.id === e.groupId));

  // Category counts
  const catMap = {};
  expenses.forEach(e => {
    catMap[e.category] = (catMap[e.category] || 0) + (e.amountPaise / 100);
  });

  // Monthly counts
  const monthMap = {};
  expenses.forEach(e => {
    const d = new Date(e.date);
    const key = d.toLocaleString('default', { month: 'short' });
    monthMap[key] = (monthMap[key] || 0) + (e.amountPaise / 100);
  });

  const isLight = document.documentElement.getAttribute('data-theme') === 'light';
  const textColor = isLight ? '#475569' : '#94a3b8';

  // Monthly Chart
  const ctx1 = document.getElementById('monthlyChartCanvas');
  if (ctx1) {
    if (monthlyChart) monthlyChart.destroy();
    monthlyChart = new Chart(ctx1, {
      type: 'bar',
      data: {
        labels: Object.keys(monthMap).length ? Object.keys(monthMap) : ['Oct', 'Nov'],
        datasets: [{
          label: 'Spending',
          data: Object.values(monthMap).length ? Object.values(monthMap) : [1200, 3400],
          backgroundColor: '#3b82f6',
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: textColor }, grid: { display: false } },
          y: { ticks: { color: textColor }, grid: { color: isLight ? '#e2e8f0' : '#1e293b' } }
        }
      }
    });
  }

  // Category Chart
  const ctx2 = document.getElementById('categoryChartCanvas');
  if (ctx2) {
    if (categoryChart) categoryChart.destroy();
    categoryChart = new Chart(ctx2, {
      type: 'doughnut',
      data: {
        labels: Object.keys(catMap).length ? Object.keys(catMap) : ['Food', 'Travel', 'Entertainment'],
        datasets: [{
          data: Object.values(catMap).length ? Object.values(catMap) : [40, 30, 30],
          backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right', labels: { color: textColor, boxWidth: 12 } }
        }
      }
    });
  }
}

/* ==========================================================================
   VIEW 2: GROUPS MANAGEMENT
   ========================================================================== */
function renderGroups(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">My Expense Groups</h1>
        <p class="page-subtitle">Manage shared trips, apartment roommates, and project teams.</p>
      </div>
      <button class="btn btn-primary" onclick="openCreateGroupModal()">➕ Create New Group</button>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 18px;">
      ${userGroups.map(g => {
        const balances = calculateGroupBalances(g.id);
        const myBal = balances.find(b => b.userId === user.id);
        let myBalText = '₹0.00';
        let myBalClass = '';
        if (myBal) {
          if (myBal.net > 0) { myBalText = `You are owed +${formatCurrency(myBal.net)}`; myBalClass = 'text-green'; }
          else if (myBal.net < 0) { myBalText = `You owe -${formatCurrency(-myBal.net)}`; myBalClass = 'text-red'; }
          else { myBalText = 'Fully settled'; myBalClass = 'text-blue'; }
        }

        let totalGroupExp = 0;
        appData.expenses.filter(e => e.groupId === g.id).forEach(e => totalGroupExp += e.amountPaise);

        return `
          <div class="card" style="display:flex; flex-direction:column; justify-content:space-between;">
            <div>
              <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
                <h3 style="font-size:17px; font-weight:800;">${escapeHtml(g.name)}</h3>
                <span class="badge badge-blue">${g.memberIds.length} members</span>
              </div>
              <p style="font-size:12px; color:var(--text-muted); margin-bottom:14px;">${escapeHtml(g.desc || 'No description.')}</p>
              
              <div style="background:var(--bg-base); padding:10px 14px; border-radius:8px; margin-bottom:14px;">
                <div style="font-size:11px; color:var(--text-muted);">YOUR BALANCE IN THIS GROUP</div>
                <div style="font-size:15px; font-weight:bold;" class="${myBalClass}">${myBalText}</div>
                <div style="font-size:11px; color:var(--text-subtle); margin-top:4px;">Total Group Spending: ${formatCurrency(totalGroupExp)}</div>
              </div>

              <div style="font-size:12px; font-weight:600; margin-bottom:6px;">Members:</div>
              <div style="display:flex; flex-wrap:wrap; gap:6px; margin-bottom:16px;">
                ${g.memberIds.map(uid => `<span class="badge badge-blue" style="background:var(--bg-card-hover); color:var(--text-main);">${escapeHtml(getUser(uid).name)}</span>`).join('')}
              </div>
            </div>

            <div style="display:flex; gap:8px; border-top:1px solid var(--border-color); padding-top:12px;">
              <button class="btn btn-secondary" style="flex:1; padding:6px 10px; font-size:12px;" onclick="openAddMemberModal(${g.id})">➕ Add Member</button>
              <button class="btn btn-primary" style="flex:1; padding:6px 10px; font-size:12px;" onclick="setView('balances')">View Balances</button>
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

/* ==========================================================================
   VIEW 3: ADD EXPENSE (WITH 3 SPLIT ALGORITHMS)
   ========================================================================== */
function renderAddExpense(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const defaultGroup = userGroups[0] || { id: 1, memberIds: [] };

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Add New Group Expense</h1>
        <p class="page-subtitle">Record a shared bill and allocate fair shares with live mathematical verification.</p>
      </div>
    </div>

    <div style="max-width: 820px;">
      <div class="card" style="margin-bottom: 20px;">
        <h3 class="card-title">1. Expense Information</h3>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px;">
          <div class="form-group">
            <label class="form-label">Expense Title *</label>
            <input type="text" id="exp-title" class="form-control" placeholder="e.g. Dinner, Rent, Uber Ride" required />
          </div>
          <div class="form-group">
            <label class="form-label">Total Amount (${appData.currency}) *</label>
            <input type="number" id="exp-amount" class="form-control" placeholder="e.g. 900.00" step="0.01" min="1" oninput="onExpenseAmountChanged()" required />
          </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px;">
          <div class="form-group">
            <label class="form-label">Category</label>
            <select id="exp-category" class="form-control">
              <option value="Food">Food & Dining</option>
              <option value="Travel">Travel & Fuel</option>
              <option value="Rent">Rent & Housing</option>
              <option value="Entertainment">Entertainment</option>
              <option value="Groceries">Groceries</option>
              <option value="Electricity">Electricity & Utility</option>
              <option value="Shopping">Shopping</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Group *</label>
            <select id="exp-group" class="form-control" onchange="onGroupChanged()">
              ${userGroups.map(g => `<option value="${g.id}">${escapeHtml(g.name)}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Paid By *</label>
            <select id="exp-payer" class="form-control">
              ${defaultGroup.memberIds.map(uid => `<option value="${uid}" ${uid === user.id ? 'selected' : ''}>${escapeHtml(getUser(uid).name)}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Expense Date</label>
            <input type="date" id="exp-date" class="form-control" value="${new Date().toISOString().split('T')[0]}" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Description / Itemized Notes</label>
          <input type="text" id="exp-desc" class="form-control" placeholder="Optional notes about the bill..." />
        </div>
      </div>

      <!-- Split Method & Participants -->
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
          <h3 class="card-title" style="margin: 0;">2. Split Strategy</h3>
          <div style="display: flex; gap: 8px;">
            <button class="btn btn-secondary active-split-tab" id="tab-equal" onclick="setSplitMethod('EQUAL')">Equal Split</button>
            <button class="btn btn-secondary" id="tab-exact" onclick="setSplitMethod('EXACT')">Exact Amounts</button>
            <button class="btn btn-secondary" id="tab-percentage" onclick="setSplitMethod('PERCENTAGE')">Percentage Split</button>
          </div>
        </div>

        <div id="split-status-banner" style="padding: 10px 14px; background: var(--bg-base); border-radius: 8px; margin-bottom: 14px; font-size: 13px; font-weight: 600; color: var(--success);">
          ✓ Equal split selected. All selected participants share equally.
        </div>

        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th style="width: 50px;">Include</th>
                <th>Participant</th>
                <th id="custom-col-header" style="display: none;">Custom Value</th>
                <th>Calculated Share</th>
              </tr>
            </thead>
            <tbody id="participant-rows">
              <!-- Rendered via updateParticipantRows() -->
            </tbody>
          </table>
        </div>

        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px;">
          <button class="btn btn-secondary" onclick="renderAddExpense(document.getElementById('view-container'))">Reset</button>
          <button class="btn btn-primary" style="padding: 11px 28px; font-size: 14px;" onclick="saveExpense()">💾 Save &amp; Split Expense</button>
        </div>
      </div>
    </div>
  `;

  currentSplitMethod = 'EQUAL';
  updateParticipantRows();
}

let currentSplitMethod = 'EQUAL';

function setSplitMethod(method) {
  currentSplitMethod = method;
  document.getElementById('tab-equal').className = method === 'EQUAL' ? 'btn btn-primary' : 'btn btn-secondary';
  document.getElementById('tab-exact').className = method === 'EXACT' ? 'btn btn-primary' : 'btn btn-secondary';
  document.getElementById('tab-percentage').className = method === 'PERCENTAGE' ? 'btn btn-primary' : 'btn btn-secondary';
  
  const customHeader = document.getElementById('custom-col-header');
  if (method === 'EXACT') {
    customHeader.style.display = 'table-cell';
    customHeader.textContent = `Exact Amount (${appData.currency})`;
  } else if (method === 'PERCENTAGE') {
    customHeader.style.display = 'table-cell';
    customHeader.textContent = 'Percentage (%)';
  } else {
    customHeader.style.display = 'none';
  }

  updateParticipantRows();
}

function onGroupChanged() {
  const gId = parseInt(document.getElementById('exp-group').value);
  const group = getGroup(gId);
  const payerSelect = document.getElementById('exp-payer');
  payerSelect.innerHTML = group.memberIds.map(uid => `<option value="${uid}">${escapeHtml(getUser(uid).name)}</option>`).join('');
  updateParticipantRows();
}

function onExpenseAmountChanged() {
  updateParticipantRows();
}

function updateParticipantRows() {
  const gId = parseInt(document.getElementById('exp-group').value);
  const group = getGroup(gId);
  const amount = parseFloat(document.getElementById('exp-amount').value) || 0;
  const amountPaise = Math.round(amount * 100);

  const tbody = document.getElementById('participant-rows');
  if (!tbody) return;

  const prevChecked = {};
  const prevCustom = {};
  document.querySelectorAll('.part-check').forEach(cb => { prevChecked[cb.dataset.uid] = cb.checked; });
  document.querySelectorAll('.part-custom').forEach(inp => { prevCustom[inp.dataset.uid] = inp.value; });

  const activeUids = group.memberIds.filter(uid => prevChecked[uid] !== false);
  const count = activeUids.length;

  let html = '';
  group.memberIds.forEach(uid => {
    const isChecked = prevChecked[uid] !== false;
    const user = getUser(uid);
    let calculatedShare = 0;

    if (isChecked && count > 0) {
      if (currentSplitMethod === 'EQUAL') {
        const base = Math.floor(amountPaise / count);
        const rem = amountPaise % count;
        const idx = activeUids.indexOf(uid);
        calculatedShare = base + (idx < rem ? 1 : 0);
      }
    }

    html += `
      <tr>
        <td>
          <input type="checkbox" class="part-check" data-uid="${uid}" ${isChecked ? 'checked' : ''} onchange="updateParticipantRows()" />
        </td>
        <td>
          <strong>${escapeHtml(user.name)}</strong> (@${escapeHtml(user.username)})
        </td>
        ${currentSplitMethod !== 'EQUAL' ? `
          <td>
            <input type="number" class="form-control part-custom" data-uid="${uid}" style="width:120px;" 
                   value="${prevCustom[uid] || (currentSplitMethod === 'PERCENTAGE' ? (100 / count).toFixed(2) : (amount / count).toFixed(2))}" 
                   step="0.01" oninput="recalcCustomShares()" ${!isChecked ? 'disabled' : ''} />
          </td>
        ` : ''}
        <td class="share-cell" data-uid="${uid}">
          <strong>${formatCurrency(calculatedShare)}</strong>
        </td>
      </tr>
    `;
  });

  tbody.innerHTML = html;
  recalcCustomShares();
}

function recalcCustomShares() {
  const amount = parseFloat(document.getElementById('exp-amount').value) || 0;
  const amountPaise = Math.round(amount * 100);
  const banner = document.getElementById('split-status-banner');

  if (currentSplitMethod === 'EQUAL') {
    const checked = document.querySelectorAll('.part-check:checked');
    banner.style.color = 'var(--success)';
    banner.textContent = `✓ Split equally among ${checked.length} participants (${formatCurrency(amountPaise)})`;
    return;
  }

  let totalCustom = 0;
  const customInputs = document.querySelectorAll('.part-custom');

  customInputs.forEach(inp => {
    const uid = inp.dataset.uid;
    const isChecked = document.querySelector(`.part-check[data-uid="${uid}"]`).checked;
    if (isChecked) {
      const val = parseFloat(inp.value) || 0;
      totalCustom += val;
      const shareCell = document.querySelector(`.share-cell[data-uid="${uid}"]`);
      if (shareCell) {
        if (currentSplitMethod === 'EXACT') {
          shareCell.innerHTML = `<strong>${formatCurrency(Math.round(val * 100))}</strong>`;
        } else if (currentSplitMethod === 'PERCENTAGE') {
          const paise = Math.round((val / 100) * amountPaise);
          shareCell.innerHTML = `<strong>${formatCurrency(paise)}</strong>`;
        }
      }
    }
  });

  if (currentSplitMethod === 'EXACT') {
    const diff = amount - totalCustom;
    if (Math.abs(diff) < 0.01) {
      banner.style.color = 'var(--success)';
      banner.textContent = `✓ Exact amounts match total: ${formatCurrency(amountPaise)}`;
    } else if (diff > 0) {
      banner.style.color = 'var(--warning)';
      banner.textContent = `⚠ ${formatCurrency(Math.round(diff * 100))} remaining to be allocated.`;
    } else {
      banner.style.color = 'var(--danger)';
      banner.textContent = `⚠ Sum exceeds total by ${formatCurrency(Math.round(-diff * 100))}`;
    }
  } else if (currentSplitMethod === 'PERCENTAGE') {
    const diff = 100.0 - totalCustom;
    if (Math.abs(diff) < 0.05) {
      banner.style.color = 'var(--success)';
      banner.textContent = `✓ Total: 100.0% (${formatCurrency(amountPaise)})`;
    } else {
      banner.style.color = 'var(--danger)';
      banner.textContent = `⚠ Current total is ${totalCustom.toFixed(2)}%. Must equal 100.00%`;
    }
  }
}

function saveExpense() {
  const title = document.getElementById('exp-title').value.trim();
  const amount = parseFloat(document.getElementById('exp-amount').value) || 0;
  const category = document.getElementById('exp-category').value;
  const groupId = parseInt(document.getElementById('exp-group').value);
  const paidByUserId = parseInt(document.getElementById('exp-payer').value);
  const date = document.getElementById('exp-date').value;
  const desc = document.getElementById('exp-desc').value.trim();

  if (!title) { alert('Please enter an expense title.'); return; }
  if (amount <= 0) { alert('Please enter a valid positive amount.'); return; }

  const amountPaise = Math.round(amount * 100);
  const checkedBoxes = Array.from(document.querySelectorAll('.part-check:checked'));
  if (checkedBoxes.length === 0) { alert('Please select at least one participant.'); return; }

  const shares = {};
  if (currentSplitMethod === 'EQUAL') {
    const count = checkedBoxes.length;
    const base = Math.floor(amountPaise / count);
    const rem = amountPaise % count;
    checkedBoxes.forEach((cb, idx) => {
      const uid = parseInt(cb.dataset.uid);
      shares[uid] = base + (idx < rem ? 1 : 0);
    });
  } else if (currentSplitMethod === 'EXACT') {
    let sum = 0;
    checkedBoxes.forEach(cb => {
      const uid = parseInt(cb.dataset.uid);
      const val = parseFloat(document.querySelector(`.part-custom[data-uid="${uid}"]`).value) || 0;
      const paise = Math.round(val * 100);
      shares[uid] = paise;
      sum += paise;
    });
    if (sum !== amountPaise) {
      alert(`Exact split total (${formatCurrency(sum)}) does not equal total amount (${formatCurrency(amountPaise)}).`);
      return;
    }
  } else if (currentSplitMethod === 'PERCENTAGE') {
    let pctSum = 0;
    checkedBoxes.forEach(cb => {
      const uid = parseInt(cb.dataset.uid);
      const val = parseFloat(document.querySelector(`.part-custom[data-uid="${uid}"]`).value) || 0;
      pctSum += val;
      shares[uid] = Math.round((val / 100) * amountPaise);
    });
    if (Math.abs(pctSum - 100) > 0.05) {
      alert(`Percentages must total 100.00% (currently ${pctSum.toFixed(2)}%).`);
      return;
    }
  }

  const newId = appData.expenses.length ? Math.max(...appData.expenses.map(e => e.id)) + 1 : 1;
  const newExp = {
    id: newId,
    groupId,
    title,
    category,
    amountPaise,
    paidByUserId,
    date,
    splitMethod: currentSplitMethod,
    shares,
    description: desc
  };

  appData.expenses.unshift(newExp);

  // Send notifications to participants
  const payer = getUser(paidByUserId);
  const group = getGroup(groupId);
  Object.entries(shares).forEach(([uid, share]) => {
    const u = parseInt(uid);
    if (u !== paidByUserId) {
      appData.notifications.unshift({
        id: Date.now() + Math.random(),
        userId: u,
        title: `New Expense in ${group.name}`,
        msg: `${payer.name} added '${title}' (${formatCurrency(amountPaise)}). Your share: ${formatCurrency(share)}`,
        date: new Date().toISOString().split('T')[0],
        read: false
      });
    }
  });

  saveState();
  alert(`Expense "${title}" (${formatCurrency(amountPaise)}) saved successfully!`);
  setView('expenses');
}

/* ==========================================================================
   VIEW 4: EXPENSES LIST
   ========================================================================== */
function renderExpenses(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Expenses Ledger</h1>
        <p class="page-subtitle">Search, filter, inspect breakdowns, and manage expense records.</p>
      </div>
      <button class="btn btn-primary" onclick="setView('add-expense')">➕ Add Expense</button>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="margin-bottom: 20px; padding: 14px 20px; display: flex; gap: 14px; flex-wrap: wrap; align-items: center;">
      <input type="text" id="expense-search" class="form-control" style="flex: 2; min-width: 220px;" placeholder="🔍 Search by title, payer or notes..." oninput="filterExpensesTable()" />
      <select id="expense-group-filter" class="form-control" style="flex: 1; min-width: 160px;" onchange="filterExpensesTable()">
        <option value="ALL">All Groups</option>
        ${userGroups.map(g => `<option value="${g.id}">${escapeHtml(g.name)}</option>`).join('')}
      </select>
      <select id="expense-category-filter" class="form-control" style="flex: 1; min-width: 140px;" onchange="filterExpensesTable()">
        <option value="ALL">All Categories</option>
        <option value="Food">Food & Dining</option>
        <option value="Travel">Travel & Fuel</option>
        <option value="Rent">Rent & Housing</option>
        <option value="Entertainment">Entertainment</option>
        <option value="Groceries">Groceries</option>
        <option value="Electricity">Electricity & Utility</option>
        <option value="Shopping">Shopping</option>
        <option value="Other">Other</option>
      </select>
    </div>

    <div class="card">
      <div class="table-container">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Title</th>
              <th>Group</th>
              <th>Category</th>
              <th>Paid By</th>
              <th>Split</th>
              <th>Total Amount</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="expenses-table-body">
            <!-- Populated via filterExpensesTable() -->
          </tbody>
        </table>
      </div>
    </div>
  `;

  filterExpensesTable();
}

function filterExpensesTable() {
  const query = (document.getElementById('expense-search')?.value || '').toLowerCase();
  const groupFilter = document.getElementById('expense-group-filter')?.value || 'ALL';
  const catFilter = document.getElementById('expense-category-filter')?.value || 'ALL';

  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const userGroupIds = userGroups.map(g => g.id);

  const filtered = appData.expenses.filter(e => {
    if (!userGroupIds.includes(e.groupId)) return false;
    if (groupFilter !== 'ALL' && e.groupId !== parseInt(groupFilter)) return false;
    if (catFilter !== 'ALL' && e.category !== catFilter) return false;
    if (query) {
      const matchTitle = e.title.toLowerCase().includes(query);
      const matchPayer = getUser(e.paidByUserId).name.toLowerCase().includes(query);
      const matchDesc = (e.description || '').toLowerCase().includes(query);
      return matchTitle || matchPayer || matchDesc;
    }
    return true;
  });

  const tbody = document.getElementById('expenses-table-body');
  if (!tbody) return;

  if (filtered.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding: 24px; color:var(--text-muted);">No expenses match your filters.</td></tr>';
    return;
  }

  tbody.innerHTML = filtered.map(e => `
    <tr>
      <td>${e.date}</td>
      <td><strong>${escapeHtml(e.title)}</strong></td>
      <td><span class="badge badge-blue">${escapeHtml(getGroup(e.groupId).name)}</span></td>
      <td>${escapeHtml(e.category)}</td>
      <td>${escapeHtml(getUser(e.paidByUserId).name)}</td>
      <td><small style="color:var(--text-muted);">${e.splitMethod || 'EQUAL'}</small></td>
      <td><strong>${formatCurrency(e.amountPaise)}</strong></td>
      <td>
        <div style="display:flex; gap:6px;">
          <button class="btn btn-secondary" style="padding:4px 8px; font-size:11px;" onclick="viewExpenseDetails(${e.id})">👁️</button>
          <button class="btn btn-danger" style="padding:4px 8px; font-size:11px;" onclick="deleteExpense(${e.id})">🗑️</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function viewExpenseDetails(id) {
  const e = appData.expenses.find(exp => exp.id === id);
  if (!e) return;
  const payer = getUser(e.paidByUserId);
  const group = getGroup(e.groupId);

  let sharesHtml = '';
  if (e.shares) {
    sharesHtml = Object.entries(e.shares).map(([uid, share]) => `
      <div style="display:flex; justify-content:space-between; padding:6px 0; border-bottom:1px solid var(--border-subtle);">
        <span>${escapeHtml(getUser(uid).name)}</span>
        <strong>${formatCurrency(share)}</strong>
      </div>
    `).join('');
  }

  openModal(`
    <h2 style="font-size:20px; font-weight:800; margin-bottom:4px;">${escapeHtml(e.title)}</h2>
    <div style="color:var(--text-muted); font-size:12px; margin-bottom:16px;">${escapeHtml(group.name)} • ${e.date}</div>

    <div style="background:var(--bg-base); padding:14px; border-radius:8px; margin-bottom:16px;">
      <div style="font-size:11px; color:var(--text-muted);">TOTAL EXPENSE AMOUNT</div>
      <div style="font-size:24px; font-weight:800; color:var(--primary);">${formatCurrency(e.amountPaise)}</div>
      <div style="font-size:12px; margin-top:4px;">Paid by: <strong>${escapeHtml(payer.name)}</strong></div>
      <div style="font-size:12px; color:var(--text-subtle);">Category: ${e.category} | Method: ${e.splitMethod || 'EQUAL'}</div>
    </div>

    <div style="margin-bottom:14px;">
      <div style="font-weight:700; font-size:13px; margin-bottom:6px;">Participant Breakdown:</div>
      ${sharesHtml || '<p style="color:var(--text-muted);">Equal split.</p>'}
    </div>

    <div style="margin-bottom:20px;">
      <div style="font-weight:700; font-size:13px; margin-bottom:4px;">Notes:</div>
      <div style="font-size:12px; color:var(--text-muted);">${escapeHtml(e.description || 'No additional notes.')}</div>
    </div>

    <div style="display:flex; justify-content:flex-end;">
      <button class="btn btn-secondary" onclick="closeModal()">Close</button>
    </div>
  `);
}

function deleteExpense(id) {
  if (confirm('Are you sure you want to delete this expense? Group balances will be automatically recalculated.')) {
    appData.expenses = appData.expenses.filter(e => e.id !== id);
    saveState();
    filterExpensesTable();
  }
}

/* ==========================================================================
   VIEW 5: BALANCES & MIN-CASH-FLOW SIMPLIFIED DEBTS
   ========================================================================== */
function renderBalances(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const activeGroupId = userGroups[0]?.id || 1;

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Balances &amp; Debt Simplification</h1>
        <p class="page-subtitle">Live net balance ledgers and the minimal transactions needed to clear all group debts.</p>
      </div>
      <div>
        <select id="balance-group-select" class="form-control" onchange="loadGroupBalanceView()">
          ${userGroups.map(g => `<option value="${g.id}">${escapeHtml(g.name)}</option>`).join('')}
        </select>
      </div>
    </div>

    <div id="balance-view-content">
      <!-- Populated via loadGroupBalanceView() -->
    </div>
  `;

  loadGroupBalanceView();
}

function loadGroupBalanceView() {
  const gId = parseInt(document.getElementById('balance-group-select')?.value || 1);
  const group = getGroup(gId);
  const container = document.getElementById('balance-view-content');
  if (!container) return;

  const balances = calculateGroupBalances(gId);
  const user = getCurrentUser();
  const myBal = balances.find(b => b.userId === user.id);

  let bannerText = `You are fully settled up in ${group.name} (₹0.00).`;
  let bannerColor = 'var(--primary)';
  if (myBal && myBal.net > 0) {
    bannerText = `You are owed a total of +${formatCurrency(myBal.net)} in ${group.name}.`;
    bannerColor = 'var(--success)';
  } else if (myBal && myBal.net < 0) {
    bannerText = `You owe a total of -${formatCurrency(-myBal.net)} in ${group.name}.`;
    bannerColor = 'var(--danger)';
  }

  // Calculate Greedy Simplified Debts
  const simplified = simplifyDebts(balances);

  container.innerHTML = `
    <div class="card" style="margin-bottom: 20px; padding: 14px 20px; border-left: 4px solid ${bannerColor};">
      <span style="font-size: 15px; font-weight: bold; color: ${bannerColor};">${bannerText}</span>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap: 20px;">
      <!-- Left: Net Balance Table -->
      <div class="card">
        <h3 class="card-title">Member Net Balances</h3>
        <p style="font-size: 11px; color: var(--text-muted); margin-bottom: 12px;">Net = Total Paid − Total Share + Settled Given − Settled Received</p>
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Member</th>
                <th>Paid</th>
                <th>Share</th>
                <th>Net Balance</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${balances.map(b => {
                let statusBadge = '<span class="badge" style="background:var(--bg-card-hover); color:var(--text-subtle);">Settled</span>';
                let netClass = '';
                if (b.net > 0) {
                  statusBadge = `<span class="badge badge-green">Gets back ${formatCurrency(b.net)}</span>`;
                  netClass = 'text-green';
                } else if (b.net < 0) {
                  statusBadge = `<span class="badge badge-red">Owes ${formatCurrency(-b.net)}</span>`;
                  netClass = 'text-red';
                }
                return `
                  <tr>
                    <td><strong>${escapeHtml(b.user.name)}</strong></td>
                    <td>${formatCurrency(b.paid)}</td>
                    <td>${formatCurrency(b.share)}</td>
                    <td class="${netClass}"><strong>${b.net > 0 ? '+' : ''}${formatCurrency(b.net)}</strong></td>
                    <td>${statusBadge}</td>
                  </tr>
                `;
              }).join('')}
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right: Greedy Min-Cash-Flow Debt Simplification -->
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <h3 class="card-title" style="margin: 0;">⚡ Simplified Direct Payments</h3>
          <span class="badge badge-blue">${simplified.length} optimal transaction${simplified.length === 1 ? '' : 's'}</span>
        </div>
        <p style="font-size: 11px; color: var(--text-muted); margin-bottom: 16px;">
          Using the Greedy Min-Cash-Flow algorithm to eliminate circular debts.
        </p>

        <div style="display: flex; flex-direction: column; gap: 10px;">
          ${simplified.length === 0 ? '<div style="padding: 24px; text-align: center; color: var(--success); font-weight: bold;">🎉 All group debts are resolved!</div>' : ''}
          ${simplified.map(d => `
            <div style="background: var(--bg-base); border: 1px solid var(--border-color); border-radius: 8px; padding: 12px 16px; display: flex; align-items: center; justify-content: space-between; gap: 12px;">
              <div>
                <span style="font-weight: bold; color: var(--danger);">${escapeHtml(d.fromName)}</span>
                <span style="font-size: 11px; color: var(--text-muted); margin: 0 6px;">── pays ──►</span>
                <span style="font-weight: bold; color: var(--success);">${escapeHtml(d.toName)}</span>
              </div>
              <div style="display: flex; align-items: center; gap: 12px;">
                <strong style="font-size: 15px;">${formatCurrency(d.amountPaise)}</strong>
                <button class="btn btn-success" style="padding: 4px 10px; font-size: 11px;" onclick="openQuickSettle(${d.fromUserId}, ${d.toUserId}, ${d.amountPaise}, ${gId})">🤝 Settle</button>
              </div>
            </div>
          `).join('')}
        </div>

        <div style="background: var(--bg-card-hover); padding: 12px; border-radius: 8px; margin-top: 18px; font-size: 11px; color: var(--text-muted);">
          <strong>💡 Mathematical Guarantee:</strong> For any group with $N$ members, the greedy cash-flow engine resolves all debts in at most $N-1$ payments without precision drift.
        </div>
      </div>
    </div>
  `;
}

/* ==========================================================================
   VIEW 6: SETTLEMENTS & RECORD PAYMENT
   ========================================================================== */
function renderSettlements(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const activeGroupId = userGroups[0]?.id || 1;
  const activeGroup = getGroup(activeGroupId);

  // Settlements relevant to user's groups
  const groupIds = userGroups.map(g => g.id);
  const relevantSettlements = appData.settlements.filter(s => groupIds.includes(s.groupId));

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Settle Up &amp; Payment Records</h1>
        <p class="page-subtitle">Record cash, UPI, or bank transfer payments to clear debts.</p>
      </div>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 20px;">
      <!-- Record Payment Card -->
      <div class="card">
        <h3 class="card-title">Record a Settlement</h3>
        <div class="form-group">
          <label class="form-label">Group *</label>
          <select id="settle-group" class="form-control" onchange="onSettleGroupChanged()">
            ${userGroups.map(g => `<option value="${g.id}">${escapeHtml(g.name)}</option>`).join('')}
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Who Paid? (Debtor) *</label>
          <select id="settle-payer" class="form-control">
            ${activeGroup.memberIds.map(uid => `<option value="${uid}">${escapeHtml(getUser(uid).name)}</option>`).join('')}
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Who Received? (Creditor) *</label>
          <select id="settle-receiver" class="form-control">
            ${activeGroup.memberIds.map(uid => `<option value="${uid}" ${uid === user.id ? 'selected' : ''}>${escapeHtml(getUser(uid).name)}</option>`).join('')}
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">Amount Paid (${appData.currency}) *</label>
          <input type="number" id="settle-amount" class="form-control" placeholder="e.g. 500.00" step="0.01" min="1" required />
        </div>

        <div class="form-group">
          <label class="form-label">Payment Date</label>
          <input type="date" id="settle-date" class="form-control" value="${new Date().toISOString().split('T')[0]}" />
        </div>

        <div class="form-group">
          <label class="form-label">Reference / Notes</label>
          <input type="text" id="settle-notes" class="form-control" placeholder="e.g. Google Pay UPI, Cash handed over" />
        </div>

        <button class="btn btn-success" style="width: 100%; padding: 12px; margin-top: 10px;" onclick="saveSettlement()">🤝 Record Settlement</button>
      </div>

      <!-- History Table -->
      <div class="card">
        <h3 class="card-title">Settlement Proofs History</h3>
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Group</th>
                <th>Payer</th>
                <th>Receiver</th>
                <th>Amount</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              ${relevantSettlements.length === 0 ? '<tr><td colspan="6" style="text-align:center; padding:20px; color:var(--text-muted);">No settlements recorded yet.</td></tr>' : ''}
              ${relevantSettlements.map(s => `
                <tr>
                  <td>${s.date}</td>
                  <td>${escapeHtml(getGroup(s.groupId).name)}</td>
                  <td><span class="badge badge-red">${escapeHtml(getUser(s.payerUserId).name)}</span></td>
                  <td><span class="badge badge-green">${escapeHtml(getUser(s.receiverUserId).name)}</span></td>
                  <td><strong>${formatCurrency(s.amountPaise)}</strong></td>
                  <td><small style="color:var(--text-muted);">${escapeHtml(s.notes || '-')}</small></td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}

function onSettleGroupChanged() {
  const gId = parseInt(document.getElementById('settle-group').value);
  const group = getGroup(gId);
  const payerSel = document.getElementById('settle-payer');
  const recSel = document.getElementById('settle-receiver');

  const options = group.memberIds.map(uid => `<option value="${uid}">${escapeHtml(getUser(uid).name)}</option>`).join('');
  payerSel.innerHTML = options;
  recSel.innerHTML = options;
  if (group.memberIds.length > 1) recSel.selectedIndex = 1;
}

function saveSettlement() {
  const gId = parseInt(document.getElementById('settle-group').value);
  const payerId = parseInt(document.getElementById('settle-payer').value);
  const recId = parseInt(document.getElementById('settle-receiver').value);
  const amt = parseFloat(document.getElementById('settle-amount').value) || 0;
  const date = document.getElementById('settle-date').value;
  const notes = document.getElementById('settle-notes').value.trim();

  if (payerId === recId) { alert('Payer and receiver cannot be the same person.'); return; }
  if (amt <= 0) { alert('Please enter a valid positive amount.'); return; }

  const amountPaise = Math.round(amt * 100);
  const newId = appData.settlements.length ? Math.max(...appData.settlements.map(s => s.id)) + 1 : 1;

  appData.settlements.unshift({
    id: newId,
    groupId: gId,
    payerUserId: payerId,
    receiverUserId: recId,
    amountPaise,
    date,
    notes
  });

  // Notifications
  const payer = getUser(payerId);
  const rec = getUser(recId);
  appData.notifications.unshift({
    id: Date.now(),
    userId: recId,
    title: 'Payment Received',
    msg: `${payer.name} recorded a settlement of ${formatCurrency(amountPaise)} to you.`,
    date,
    read: false
  });

  saveState();
  alert(`Settlement of ${formatCurrency(amountPaise)} recorded successfully!`);
  setView('settlements');
}

function openQuickSettle(fromId, toId, amountPaise, gId) {
  setView('settlements');
  setTimeout(() => {
    if (gId) document.getElementById('settle-group').value = gId;
    document.getElementById('settle-payer').value = fromId;
    document.getElementById('settle-receiver').value = toId;
    document.getElementById('settle-amount').value = (amountPaise / 100).toFixed(2);
    document.getElementById('settle-notes').value = 'Quick debt settlement';
  }, 100);
}

/* ==========================================================================
   VIEW 7: ANALYTICS & REPORTS (CSV EXPORT)
   ========================================================================== */
function renderReports(container) {
  const user = getCurrentUser();
  const userGroups = appData.groups.filter(g => g.memberIds.includes(user.id));
  const overview = getUserOverview(user.id);

  let totalGroupExp = 0;
  userGroups.forEach(g => {
    appData.expenses.filter(e => e.groupId === g.id).forEach(e => totalGroupExp += e.amountPaise);
  });

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Financial Analytics &amp; Reports</h1>
        <p class="page-subtitle">Inspect comprehensive spending trends, category distributions, and export data.</p>
      </div>
      <div style="display:flex; gap:10px;">
        <button class="btn btn-primary" onclick="exportExpensesCSV()">📥 Export Expenses CSV</button>
        <button class="btn btn-secondary" onclick="exportSettlementsCSV()">📥 Export Settlements CSV</button>
      </div>
    </div>

    <!-- Summary Metrics -->
    <div class="grid-kpi">
      <div class="card kpi-card">
        <span class="kpi-title">Total Spending Across Groups</span>
        <span class="kpi-value">${formatCurrency(totalGroupExp)}</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">Your Out-of-Pocket</span>
        <span class="kpi-value text-blue">${formatCurrency(overview.totalPaid)}</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">Your True Expense Share</span>
        <span class="kpi-value text-green">${formatCurrency(overview.totalShare)}</span>
      </div>
      <div class="card kpi-card">
        <span class="kpi-title">Total Expenses Logged</span>
        <span class="kpi-value">${appData.expenses.length}</span>
      </div>
    </div>

    <!-- Charts -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(380px, 1fr)); gap: 18px; margin-bottom: 24px;">
      <div class="card">
        <h3 class="card-title">Category Expense Allocation</h3>
        <div style="height: 260px; position: relative;">
          <canvas id="categoryChartCanvas"></canvas>
        </div>
      </div>
      <div class="card">
        <h3 class="card-title">Spending History Trend</h3>
        <div style="height: 260px; position: relative;">
          <canvas id="monthlyChartCanvas"></canvas>
        </div>
      </div>
    </div>
  `;

  renderCharts();
}

function exportExpensesCSV() {
  let csv = 'Expense ID,Date,Title,Group,Paid By,Category,Split Method,Amount (INR),Description\n';
  appData.expenses.forEach(e => {
    csv += `${e.id},"${e.date}","${e.title.replace(/"/g, '""')}","${getGroup(e.groupId).name}","${getUser(e.paidByUserId).name}","${e.category}","${e.splitMethod || 'EQUAL'}",${(e.amountPaise / 100).toFixed(2)},"${(e.description || '').replace(/"/g, '""')}"\n`;
  });
  downloadFile('expenses_report.csv', csv);
}

function exportSettlementsCSV() {
  let csv = 'Settlement ID,Date,Group,Paid By,Received By,Amount (INR),Notes\n';
  appData.settlements.forEach(s => {
    csv += `${s.id},"${s.date}","${getGroup(s.groupId).name}","${getUser(s.payerUserId).name}","${getUser(s.receiverUserId).name}",${(s.amountPaise / 100).toFixed(2)},"${(s.notes || '').replace(/"/g, '""')}"\n`;
  });
  downloadFile('settlements_report.csv', csv);
}

function downloadFile(filename, content) {
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/* ==========================================================================
   VIEW 8: NOTIFICATIONS
   ========================================================================== */
function renderNotifications(container) {
  const user = getCurrentUser();
  const notes = appData.notifications.filter(n => n.userId === user.id);

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Notifications &amp; Activity</h1>
        <p class="page-subtitle">Track incoming bills, settlement alerts, and group invites.</p>
      </div>
      <button class="btn btn-primary" onclick="markAllNotificationsRead()">✓✓ Mark All Read</button>
    </div>

    <div class="card">
      <div style="display: flex; flex-direction: column; gap: 10px;">
        ${notes.length === 0 ? '<div style="padding: 24px; text-align: center; color: var(--text-muted);">No notifications.</div>' : ''}
        ${notes.map(n => `
          <div style="padding: 14px; background: ${n.read ? 'transparent' : 'var(--bg-card-hover)'}; border: 1px solid var(--border-color); border-radius: 8px; display: flex; justify-content: space-between; align-items: center;">
            <div>
              <div style="font-size: 14px; font-weight: bold; color: ${n.read ? 'var(--text-main)' : 'var(--primary)'};">${escapeHtml(n.title)}</div>
              <div style="font-size: 13px; color: var(--text-muted); margin-top: 2px;">${escapeHtml(n.msg)}</div>
              <div style="font-size: 11px; color: var(--text-subtle); margin-top: 4px;">${n.date}</div>
            </div>
            ${!n.read ? `<button class="btn btn-secondary" style="font-size:11px; padding:4px 8px;" onclick="markNotificationRead(${n.id})">Mark read</button>` : ''}
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

function markNotificationRead(id) {
  const note = appData.notifications.find(n => n.id === id);
  if (note) { note.read = true; saveState(); renderNotifications(document.getElementById('view-container')); }
}

function markAllNotificationsRead() {
  appData.notifications.filter(n => n.userId === appData.currentUserId).forEach(n => n.read = true);
  saveState();
  renderNotifications(document.getElementById('view-container'));
}

/* ==========================================================================
   VIEW 9: SETTINGS
   ========================================================================== */
function renderSettings(container) {
  const user = getCurrentUser();

  container.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Application Settings</h1>
        <p class="page-subtitle">Customize profile details, active currency, and theme preferences.</p>
      </div>
    </div>

    <div style="max-width: 640px; display: flex; flex-direction: column; gap: 20px;">
      <!-- Profile Card -->
      <div class="card">
        <h3 class="card-title">User Profile</h3>
        <div class="form-group">
          <label class="form-label">Full Name</label>
          <input type="text" id="setting-name" class="form-control" value="${escapeHtml(user.name)}" />
        </div>
        <div class="form-group">
          <label class="form-label">Email Address</label>
          <input type="email" id="setting-email" class="form-control" value="${escapeHtml(user.email)}" />
        </div>
        <button class="btn btn-primary" onclick="saveProfile()">Save Profile</button>
      </div>

      <!-- System Preferences -->
      <div class="card">
        <h3 class="card-title">Preferences &amp; Currency</h3>
        <div class="form-group">
          <label class="form-label">Default Currency Symbol</label>
          <select id="setting-currency" class="form-control">
            <option value="₹" ${appData.currency === '₹' ? 'selected' : ''}>₹ - Indian Rupee (INR)</option>
            <option value="$" ${appData.currency === '$' ? 'selected' : ''}>$ - US Dollar (USD)</option>
            <option value="€" ${appData.currency === '€' ? 'selected' : ''}>€ - Euro (EUR)</option>
            <option value="£" ${appData.currency === '£' ? 'selected' : ''}>£ - British Pound (GBP)</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Interface Theme</label>
          <div style="display: flex; gap: 16px; margin-top: 4px;">
            <label style="cursor: pointer;"><input type="radio" name="theme-radio" value="dark" ${appData.theme === 'dark' ? 'checked' : ''} onchange="setTheme('dark')"> Dark Navy</label>
            <label style="cursor: pointer;"><input type="radio" name="theme-radio" value="light" ${appData.theme === 'light' ? 'checked' : ''} onchange="setTheme('light')"> Modern Light</label>
          </div>
        </div>
        <button class="btn btn-primary" onclick="savePreferences()">Save Preferences</button>
      </div>
    </div>
  `;
}

function saveProfile() {
  const name = document.getElementById('setting-name').value.trim();
  const email = document.getElementById('setting-email').value.trim();
  const u = getCurrentUser();
  if (name) u.name = name;
  if (email) u.email = email;
  saveState();
  updateUserBadge();
  alert('Profile updated successfully!');
}

function savePreferences() {
  appData.currency = document.getElementById('setting-currency').value;
  saveState();
  alert('Preferences saved!');
  setView(activeView);
}

function setTheme(theme) {
  appData.theme = theme;
  document.documentElement.setAttribute('data-theme', theme);
  saveState();
  if (activeView === 'dashboard' || activeView === 'reports') renderCharts();
}

function toggleTheme() {
  const newTheme = appData.theme === 'light' ? 'dark' : 'light';
  setTheme(newTheme);
}

// User Switcher (For testing debts from different perspectives)
function switchUser(userId) {
  appData.currentUserId = parseInt(userId);
  saveState();
  updateUserBadge();
  setView(activeView);
}

function updateUserBadge() {
  const user = getCurrentUser();
  document.getElementById('current-user-name').textContent = user.name;
  document.getElementById('current-user-handle').textContent = '@' + user.username;
  document.getElementById('current-user-avatar').textContent = user.name.split(' ').map(n => n[0]).join('');
  
  const switcher = document.getElementById('user-switch-select');
  if (switcher) switcher.value = user.id;
}

// Modal System
function openModal(html) {
  const box = document.getElementById('modal-content');
  box.innerHTML = html;
  document.getElementById('app-modal').classList.add('active');
}

function closeModal() {
  document.getElementById('app-modal').classList.remove('active');
}

function openCreateGroupModal() {
  const user = getCurrentUser();
  const otherUsers = appData.users.filter(u => u.id !== user.id);

  openModal(`
    <h2 style="font-size:20px; font-weight:800; margin-bottom:14px;">Create New Group</h2>
    <div class="form-group">
      <label class="form-label">Group Name *</label>
      <input type="text" id="new-group-name" class="form-control" placeholder="e.g. Manali Road Trip, Apartment 501" />
    </div>
    <div class="form-group">
      <label class="form-label">Description</label>
      <input type="text" id="new-group-desc" class="form-control" placeholder="Trip or shared living notes..." />
    </div>
    <div class="form-group">
      <label class="form-label">Select Initial Members:</label>
      <div style="display:flex; flex-direction:column; gap:8px; margin-top:6px;">
        ${otherUsers.map(u => `
          <label style="display:flex; align-items:center; gap:8px; font-size:13px; cursor:pointer;">
            <input type="checkbox" class="new-group-member" value="${u.id}" checked />
            ${escapeHtml(u.name)} (@${escapeHtml(u.username)})
          </label>
        `).join('')}
      </div>
    </div>
    <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn btn-primary" onclick="saveNewGroup()">Create Group</button>
    </div>
  `);
}

function saveNewGroup() {
  const name = document.getElementById('new-group-name').value.trim();
  const desc = document.getElementById('new-group-desc').value.trim();
  if (!name) { alert('Please enter a group name.'); return; }

  const memberIds = [appData.currentUserId];
  document.querySelectorAll('.new-group-member:checked').forEach(cb => {
    memberIds.push(parseInt(cb.value));
  });

  const newId = appData.groups.length ? Math.max(...appData.groups.map(g => g.id)) + 1 : 1;
  appData.groups.unshift({ id: newId, name, desc, memberIds });
  saveState();
  closeModal();
  setView('groups');
}

function openAddMemberModal(groupId) {
  const group = getGroup(groupId);
  const eligible = appData.users.filter(u => !group.memberIds.includes(u.id));

  if (eligible.length === 0) {
    alert('All registered users are already in this group!');
    return;
  }

  openModal(`
    <h2 style="font-size:20px; font-weight:800; margin-bottom:14px;">Add Member to ${escapeHtml(group.name)}</h2>
    <div class="form-group">
      <label class="form-label">Select User:</label>
      <select id="add-member-select" class="form-control">
        ${eligible.map(u => `<option value="${u.id}">${escapeHtml(u.name)} (@${escapeHtml(u.username)})</option>`).join('')}
      </select>
    </div>
    <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:20px;">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn btn-primary" onclick="confirmAddMember(${groupId})">Add Member</button>
    </div>
  `);
}

function confirmAddMember(groupId) {
  const uid = parseInt(document.getElementById('add-member-select').value);
  const group = getGroup(groupId);
  group.memberIds.push(uid);
  saveState();
  closeModal();
  setView('groups');
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[m]);
}

// App Initialization
window.addEventListener('DOMContentLoaded', () => {
  document.documentElement.setAttribute('data-theme', appData.theme || 'dark');
  updateUserBadge();
  setView('dashboard');
});
