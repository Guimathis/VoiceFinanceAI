document.addEventListener("DOMContentLoaded", () => {
    // --- ESTADO & SELETORES ---
    const tabButtons = document.querySelectorAll(".tab-btn");
    const tabContents = document.querySelectorAll(".tab-content");

    // Dashboard
    const dashStartDate = document.getElementById("dash-start-date");
    const dashEndDate = document.getElementById("dash-end-date");
    const btnRefreshSummary = document.getElementById("btn-refresh-summary");
    const summaryTotalSpent = document.getElementById("summary-total-spent");
    const summaryTotalPending = document.getElementById("summary-total-pending");
    const summaryTotalCount = document.getElementById("summary-total-count");
    const summaryCanceledCount = document.getElementById("summary-canceled-count");
    const summaryConfirmedCount = document.getElementById("summary-confirmed-count");
    const summaryPendingCount = document.getElementById("summary-pending-count");
    const categoryContainer = document.getElementById("category-distribution-container");

    // Chat
    const chatMessages = document.getElementById("chat-messages");
    const chatForm = document.getElementById("chat-form");
    const chatInput = document.getElementById("chat-input");
    const btnClearChat = document.getElementById("btn-clear-chat");

    // Expenses Table
    const expensesTableBody = document.getElementById("expenses-table-body");
    const filterCategory = document.getElementById("filter-category");
    const filterStatus = document.getElementById("filter-status");
    const filterSearch = document.getElementById("filter-search");
    const btnApplyFilters = document.getElementById("btn-apply-filters");

    // Modal Create
    const modalCreate = document.getElementById("modal-create-expense");
    const btnOpenCreateModal = document.getElementById("btn-open-create-modal");
    const modalCreateClose = document.getElementById("modal-create-close");
    const modalCreateCancel = document.getElementById("modal-create-cancel");
    const formCreateExpense = document.getElementById("form-create-expense");
    const createDate = document.getElementById("create-date");

    // Formatação de Moeda
    const formatCurrency = (val) => {
        if (val === null || val === undefined) return "R$ 0,00";
        return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(val);
    };

    // --- INICIALIZAÇÃO DE DATAS ---
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split("T")[0];
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split("T")[0];
    const todayStr = now.toISOString().split("T")[0];

    dashStartDate.value = firstDay;
    dashEndDate.value = lastDay;
    createDate.value = todayStr;

    // --- NAVEGAÇÃO POR ABAS ---
    tabButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-tab");
            tabButtons.forEach(b => b.classList.remove("active"));
            tabContents.forEach(c => c.classList.remove("active"));

            btn.classList.add("active");
            document.getElementById(targetId).classList.add("active");

            if (targetId === "tab-dashboard") loadDashboardSummary();
            if (targetId === "tab-expenses") loadExpenses();
        });
    });

    // --- DASHBOARD / PAINEL FINANCEIRO ---
    async function loadDashboardSummary() {
        const start = dashStartDate.value;
        const end = dashEndDate.value;
        const url = `/api/v1/expenses/summary?startDate=${start}&endDate=${end}`;

        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error("Erro ao carregar resumo.");
            const data = await response.json();

            summaryTotalSpent.textContent = formatCurrency(data.totalSpent);
            summaryTotalPending.textContent = formatCurrency(data.totalPending);
            summaryTotalCount.textContent = data.totalExpensesCount || 0;
            summaryCanceledCount.textContent = `${data.canceledCount || 0} despesas descartadas`;
            summaryConfirmedCount.textContent = `${data.confirmedCount || 0} confirmadas`;
            summaryPendingCount.textContent = `${data.pendingCount || 0} aguardando confirmação`;

            renderCategoryBreakdown(data.categoryBreakdown || [], data.totalSpent || 0);
        } catch (err) {
            console.error("Falha ao carregar dashboard:", err);
            categoryContainer.innerHTML = `<p style="color: var(--accent-rose);">Não foi possível carregar os dados.</p>`;
        }
    }

    function renderCategoryBreakdown(breakdown, totalSpent) {
        if (!breakdown || breakdown.length === 0) {
            categoryContainer.innerHTML = `<p style="color: var(--text-secondary); font-size: 0.9rem;">Nenhum gasto confirmado registrado no período.</p>`;
            return;
        }

        categoryContainer.innerHTML = breakdown.map(cat => `
            <div class="category-row">
                <div class="category-row-header">
                    <span class="category-name">${cat.category}</span>
                    <span class="category-values">${formatCurrency(cat.total)} (${cat.percentage}%)</span>
                </div>
                <div class="progress-bar-bg">
                    <div class="progress-bar-fill" style="width: ${Math.min(cat.percentage, 100)}%;"></div>
                </div>
            </div>
        `).join("");
    }

    btnRefreshSummary.addEventListener("click", loadDashboardSummary);

    // --- TABELA DE DESPESAS ---
    async function loadExpenses() {
        expensesTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 1.5rem;">Carregando despesas...</td></tr>`;

        const category = filterCategory.value;
        const status = filterStatus.value;
        const search = filterSearch.value.trim();

        let query = `/api/v1/expenses?page=0&size=50`;
        if (category) query += `&category=${category}`;
        if (status) query += `&status=${status}`;
        if (search) query += `&description=${encodeURIComponent(search)}`;

        try {
            const res = await fetch(query);
            if (!res.ok) throw new Error("Erro na listagem.");
            const data = await res.json();
            const expenses = data.content || [];

            if (expenses.length === 0) {
                expensesTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 1.5rem;">Nenhuma despesa encontrada com os filtros selecionados.</td></tr>`;
                return;
            }

            expensesTableBody.innerHTML = expenses.map(exp => `
                <tr>
                    <td>${exp.occurredOn || "-"}</td>
                    <td><strong>${exp.category || "N/A"}</strong></td>
                    <td>${exp.description || "<span style='color: var(--text-secondary);'>Sem descrição</span>"}</td>
                    <td><strong>${formatCurrency(exp.amount)}</strong></td>
                    <td><span class="badge ${exp.status}">${translateStatus(exp.status)}</span></td>
                    <td><span class="badge-origin">${exp.origin || "MANUAL"}</span></td>
                    <td style="text-align: right;">
                        <div class="actions-cell" style="justify-content: flex-end;">
                            ${exp.status === "PENDING_CONFIRMATION" ? `
                                <button class="action-btn confirm" onclick="confirmExpenseDirect('${exp.id}')">Confirmar</button>
                                <button class="action-btn cancel" onclick="cancelExpenseDirect('${exp.id}')">Cancelar</button>
                            ` : ""}
                            <button class="action-btn delete" onclick="deleteExpense('${exp.id}')">Excluir</button>
                        </div>
                    </td>
                </tr>
            `).join("");
        } catch (err) {
            console.error("Falha ao buscar despesas:", err);
            expensesTableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--accent-rose); padding: 1.5rem;">Erro ao carregar despesas.</td></tr>`;
        }
    }

    function translateStatus(st) {
        if (st === "CONFIRMED") return "Confirmado";
        if (st === "PENDING_CONFIRMATION") return "Pendente";
        if (st === "CANCELED") return "Cancelado";
        return st;
    }

    btnApplyFilters.addEventListener("click", loadExpenses);

    // Ações diretas expostas globalmente
    window.confirmExpenseDirect = async (id) => {
        try {
            const res = await fetch(`/api/v1/expenses/${id}/confirm`, { method: "POST" });
            if (res.ok) {
                loadExpenses();
                loadDashboardSummary();
            } else {
                alert("Não foi possível confirmar esta despesa.");
            }
        } catch (err) {
            console.error(err);
        }
    };

    window.cancelExpenseDirect = async (id) => {
        try {
            const res = await fetch(`/api/v1/expenses/${id}/cancel`, { method: "POST" });
            if (res.ok) {
                loadExpenses();
                loadDashboardSummary();
            } else {
                alert("Não foi possível cancelar esta despesa.");
            }
        } catch (err) {
            console.error(err);
        }
    };

    window.deleteExpense = async (id) => {
        if (!confirm("Tem certeza que deseja excluir esta despesa?")) return;
        try {
            const res = await fetch(`/api/v1/expenses/${id}`, { method: "DELETE" });
            if (res.ok) {
                loadExpenses();
                loadDashboardSummary();
            } else {
                alert("Não foi possível excluir a despesa.");
            }
        } catch (err) {
            console.error(err);
        }
    };

    // --- MODAL DE CRIAÇÃO MANUAL ---
    btnOpenCreateModal.addEventListener("click", () => {
        modalCreate.classList.add("active");
    });

    const closeModal = () => modalCreate.classList.remove("active");
    modalCreateClose.addEventListener("click", closeModal);
    modalCreateCancel.addEventListener("click", closeModal);

    formCreateExpense.addEventListener("submit", async (e) => {
        e.preventDefault();
        const payload = {
            amount: parseFloat(document.getElementById("create-amount").value),
            currency: "BRL",
            category: document.getElementById("create-category").value,
            description: document.getElementById("create-description").value.trim(),
            occurredOn: document.getElementById("create-date").value
        };

        try {
            const res = await fetch("/api/v1/expenses", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                formCreateExpense.reset();
                createDate.value = todayStr;
                closeModal();
                loadExpenses();
                loadDashboardSummary();
            } else {
                const errData = await res.json();
                alert(errData.detail || "Erro ao salvar despesa.");
            }
        } catch (err) {
            console.error("Erro na criação:", err);
            alert("Erro de comunicação ao salvar despesa.");
        }
    });

    // --- CHAT COM IA ---
    chatForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;

        appendMessage("user", text);
        chatInput.value = "";

        const loadingBubble = appendMessage("assistant", "Pensando...");

        try {
            const res = await fetch("/api/v1/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ action: text })
            });

            const data = await res.json();
            loadingBubble.remove();

            if (res.ok) {
                appendMessage("assistant", data.message || "Ação processada.");
                loadDashboardSummary();
            } else {
                appendMessage("assistant", `⚠️ ${data.detail || "Não foi possível processar seu pedido."}`);
            }
        } catch (err) {
            console.error("Erro no chat:", err);
            loadingBubble.remove();
            appendMessage("assistant", "⚠️ Erro de comunicação com o servidor.");
        }
    });

    function appendMessage(sender, text) {
        const bubble = document.createElement("div");
        bubble.className = `chat-bubble ${sender}`;
        bubble.textContent = text;
        chatMessages.appendChild(bubble);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return bubble;
    }

    // Ações rápidas no chat
    document.querySelectorAll(".quick-action-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            chatInput.value = btn.getAttribute("data-text");
            chatInput.focus();
        });
    });

    btnClearChat.addEventListener("click", () => {
        chatMessages.innerHTML = `
            <div class="chat-bubble assistant">
                Histórico limpo. Como posso ajudar com suas despesas hoje?
            </div>
        `;
    });

    // Carga inicial
    loadDashboardSummary();
});
